package edu.handong.csee.isel.metrictest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.Combinations;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import com.google.common.collect.Iterators;

import edu.handong.csee.isel.Utils;

public class DSmetricMain {
	String input;
	String repositoryPath ;
	
	public static HashMap<String, DeveloperScatteringMetric> main(String[] args) throws Exception {
		long beforeTime = System.currentTimeMillis();
		
		String input = args[0];
		String repositoryPath = args[1];
		Git git = Git.open(new File(repositoryPath));
		ArrayList<String> commitHashs = getCommitHashs(git);
		
		//parsing refactoring commit
		boolean test = true;
		TreeSet<String> refactoringCommit = null;
		
		if(test == true) {
			refactoringCommit = readTxtFileForTest(repositoryPath);
		}else {
			refactoringCommit = miningRefactoringCommit(repositoryPath);
		}
		System.out.println("Length of refactoring commit : "+refactoringCommit.size());

		//print the time of finding refactoring commit
		long afterTime = System.currentTimeMillis(); 
		long secDiffTime = (afterTime - beforeTime)/1000;
		System.out.println("Mining refactoring commit 실행시(m) : "+secDiffTime/60);
		
		//read and save project commit history metric
		Reader in = new FileReader(input);
		Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);
		TreeMap<Date,ArrayList<String>> time_refactoringCommit = new TreeMap<>();
		TreeMap<Date,ProjectHistory> projectHistories = saveProjectHistoryInformation(records,refactoringCommit,time_refactoringCommit);
		
		//calculate developer scattering metric
		TreeMap<Date,Date> windows = saveStartAndEndCommittimeOfRefactoring(time_refactoringCommit,projectHistories);
		System.out.println("windows.size() : "+windows.size());
		HashMap<String,ArrayList<DeveloperScatteringMetric>> developerScatteringMetrics = new HashMap<>();
		
//		developerScatteringMetrics 
		windows.forEach((startCommitTime, endCommitTime) -> {
//			HashMap<String,DeveloperScatteringMetric> developerScatteringMetric = new HashMap<>();
			String endCommitHash = projectHistories.get(endCommitTime).getHashkeys().get(0); // 같은 commitTime에 중복되는 commitHash가 없는지 확인하기! 일단 첫번째 index의 commitHash만 사용 

			//1) find file names that each developer modified in specified period
			HashMap<String, TreeSet<String>> authorID_filePaths = saveAuthorIdAndFilePathsInTheCurrentPeriod(startCommitTime,endCommitTime,projectHistories);
			System.out.println("Time from  "+startCommitTime+"  to  "+endCommitTime);
			
			for(String authorId : authorID_filePaths.keySet()) {
				System.out.println("authorId : "+authorId+"------------------------------------------------------------------------------------------------------------");
				DeveloperScatteringMetric scatteringMetric = new DeveloperScatteringMetric();
				scatteringMetric.setAuthorId(authorId);
				TreeSet<String> filePaths = authorID_filePaths.get(authorId);
				System.out.println(filePaths.size());
				
				if(filePaths.size() < 2) {//1)-1 if the developer modified one file or less
					scatteringMetric.setStructuralScattering(0);
					scatteringMetric.setSemanticScattering(0);
					System.out.println("!!!!less than one file!!");
					System.out.println("Skip");
					System.out.println();
					System.out.println();
				}else {//1)-2 more than 2 files developer
					
					//1)-3 preprocess file name - split file path according to "/"
					ArrayList<String[]> splitPaths = new ArrayList<>();
					for(String filePath : filePaths) {
						String[] split = filePath.split(File.separator);
						splitPaths.add(split);
					}
					
					//2) calculate combination of files 
					int theNumberOfFiles = filePaths.size();
					int combination = calculateCombination(theNumberOfFiles);
					float normalization = (float)((float)theNumberOfFiles/(float)combination);
					int[][] caseOfCombination = saveCombinationSet(theNumberOfFiles,combination);

					//start cal structural&semantic
					ArrayList<Integer> dists = new ArrayList<>();
					ArrayList<Float> sims = new ArrayList<>();
					HashMap<String,TreeSet<String>> nameOfSemanticFiles = new HashMap<>();
					
					for(int i = 0; i < combination; i++) {
						int file1Index = caseOfCombination[i][0];
						int file2Index = caseOfCombination[i][1];
						
						String[] file1 = splitPaths.get(file1Index);
						String[] file2 = splitPaths.get(file2Index);
						
						//2) calculate the structural scattering
						//2)-1 calculate the dist of two filePath
						int dist = calculateDistOfTwoFiles(file1,file2);
						dists.add(dist);
						
						//3) calculate the semantic scattering
						//3)-1 calculate the similarity of two filePath
						boolean isSamePackage = compareTwoFilePaths(file1,file2,nameOfSemanticFiles);
						if(isSamePackage) {
							float sim = calSimularityOfTwoFiles(file1,file2,endCommitTime,repositoryPath,endCommitHash,projectHistories,git,commitHashs);
							sims.add(sim);
						}
					}
					
					//normalize structural
					int sumDist = dists.stream().mapToInt(i -> i.intValue()).sum();
					float structural  = normalization * (float)sumDist;
					//normalize semantic
					float simNormalization = calSimCombination(nameOfSemanticFiles);
					float sumSim = (float)sims.stream().mapToDouble(i -> i.floatValue()).sum();
					float semantic = simNormalization * (1/sumSim);
					
					System.out.println("structural : "+structural);
					System.out.println("semantic : "+semantic);
					
					//save the scattering metric result
					scatteringMetric.setStructuralScattering(structural);
					scatteringMetric.setSemanticScattering(semantic);
				}
				
				if(developerScatteringMetrics.containsKey(authorId)) {
					ArrayList<DeveloperScatteringMetric> list = developerScatteringMetrics.get(authorId);
					list.add(scatteringMetric);
				}else {
					ArrayList<DeveloperScatteringMetric> list = new ArrayList<>();
					list.add(scatteringMetric);
					developerScatteringMetrics.put(authorId, list);
				}
			}
			
		});
		
		HashMap<String,DeveloperScatteringMetric> sumDeveloperScatteringMetric = new HashMap<>();
		for(String developer : developerScatteringMetrics.keySet()) {
			ArrayList<DeveloperScatteringMetric> temp = developerScatteringMetrics.get(developer);
			float sumStructural = 0;
			float sumSemantic = 0;
			for(DeveloperScatteringMetric dev : temp) {
				sumStructural = sumStructural + dev.getStructuralScattering();
				sumSemantic = sumSemantic + dev.getSemanticScattering();
			}
			DeveloperScatteringMetric dm = new DeveloperScatteringMetric();
			dm.setStructuralScattering(sumStructural);
			dm.setSemanticScattering(sumSemantic);
			sumDeveloperScatteringMetric.put(developer, dm);
		}
		
		return sumDeveloperScatteringMetric;
	}

	private static float calSimCombination(HashMap<String, TreeSet<String>> nameOfSemanticFiles) {
		float combination = 0;
		for(String key : nameOfSemanticFiles.keySet()) {
			combination = combination + (float)calculateCombination(nameOfSemanticFiles.get(key).size());
		}
		return combination;
	}

	private static boolean compareTwoFilePaths(String[] file1, String[] file2, HashMap<String, TreeSet<String>> nameOfSemanticFiles) {
		String filePath1 = originalFilePath(file1);
		String filePath2 = originalFilePath(file2);
		String filePathPackage1 = filePath1.substring(0, filePath1.lastIndexOf(File.separator));
		String filePathPackage2 = filePath2.substring(0, filePath2.lastIndexOf(File.separator));
		
		boolean isSamePackage = filePathPackage1.equals(filePathPackage2);
		
		if(isSamePackage) {
			TreeSet<String> nameOfSemanticFile = null;
			if(nameOfSemanticFiles.containsKey(filePathPackage1)) {
				nameOfSemanticFile = nameOfSemanticFiles.get(filePathPackage1);
				nameOfSemanticFile.add(filePath1);
				nameOfSemanticFile.add(filePath2);
			}else {
				nameOfSemanticFile = new TreeSet<>();
				nameOfSemanticFile.add(filePath1);
				nameOfSemanticFile.add(filePath2);
				nameOfSemanticFiles.put(filePathPackage1, nameOfSemanticFile);
			}
		}
		
		return isSamePackage;
	}

	private static ArrayList<String> getCommitHashs(Git git) throws Exception {
		ArrayList<String> commitHashs = new ArrayList<>();
		Iterable<RevCommit> commits = git.log().all().call(); 
		commits.forEach((commit) -> {
			commitHashs.add(commit.getId().name());
		});
		return commitHashs;
	}
	
	private static String output(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line + System.getProperty("line.separator"));
            }
        } finally {
            br.close();
        }
        return sb.toString();
    }

	private static float calSimularityOfTwoFiles(String[] file1, String[] file2, Date endCommitTime,
			String repositoryPath, String endCommitHash, TreeMap<Date, ProjectHistory> projectHistories, Git git, ArrayList<String> commitHashs) {
		float simScore = 0;
		String filePath1 = originalFilePath(file1);
		String filePath2 = originalFilePath(file2);
		String tempFile1 = "./1.txt";
		String tempFile2 = "./2.txt";

//		System.out.println("endCommitTime : "+endCommitTime);
//		System.out.println("endCommitHash : "+endCommitHash);
//		System.out.println("Start calculate similarity");
//		System.out.println(filePath1);
//		System.out.println(filePath2);
//		
		
		try {
			//get endCommitTime repository
			Repository repo = git.getRepository();
			
			//get original source code
			String fileSource1 = Utils.fetchBlob(repo, endCommitHash, filePath1);
			String fileSource2 = Utils.fetchBlob(repo, endCommitHash, filePath2);
			
			if(fileSource1.length() < 1) {
				String nowCommitHash = getPreviousCommitHash(commitHashs,endCommitHash);
				while(true) {
					fileSource1 = Utils.fetchBlob(repo, nowCommitHash, filePath1);
					if(fileSource1.length() >= 1) {
						break;
					}else {
						nowCommitHash = getPreviousCommitHash(commitHashs,nowCommitHash);
					}
				}
				
				if(nowCommitHash == "error") {
					return 0;
				}
			} 
			
			if(fileSource2.length() < 1) {
				String nowCommitHash = getPreviousCommitHash(commitHashs,endCommitHash);
				while(true) {
					fileSource2 = Utils.fetchBlob(repo, nowCommitHash, filePath2);
					if(fileSource2.length() >= 1) {
						break;
					}else {
						nowCommitHash = getPreviousCommitHash(commitHashs,nowCommitHash);
					}
				}
			}
			
			//make temp txt file
			writeTxtFile(fileSource1,tempFile1);
			writeTxtFile(fileSource2,tempFile2);
			
			//calculate the tf-idf value
			ProcessBuilder builder = new ProcessBuilder("/usr/local/bin/python3","/home/yangsujin/2022DPMINERbashfile/semantic.py",tempFile1,tempFile2);
			builder.redirectErrorStream(true);
			Process process = builder.start();
			simScore = Float.parseFloat(output(process.getInputStream()).trim());
//			System.out.println("Similarity Score : "+simScore);
			
			//delete temp file
			new File(tempFile1).delete();
			new File(tempFile2).delete();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return simScore;
	}

	private static String getPreviousCommitHash(ArrayList<String> commitHashs, String endCommitHash) {
		int indexOfCommit = commitHashs.indexOf(endCommitHash)+1;
		
		if(commitHashs.size() <= indexOfCommit) {
//			System.out.println("Error!!! Can't not find Source Path!!!");
			return "error";
		}
		return commitHashs.get(indexOfCommit);
	}

	private static void writeTxtFile(String fileSource, String string) throws IOException {
		FileWriter fw = new FileWriter(new File(string));
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(fileSource);
		bw.close();
		fw.close();
	}

	private static String originalFilePath(String[] file) {
		String filePath1 = file[0];
		for(int i = 1; i < file.length; i++) {
			filePath1 = filePath1 + File.separator + file[i];
		}
		return filePath1;
	}

	private static int calculateDistOfTwoFiles(String[] file1, String[] file2) {
		ArrayList<String> dist = new ArrayList<>();
		
		//remove class name (file.length - 1 : for excluding className)
		file1 = ArrayUtils.remove(file1, file1.length-1);
		file2 = ArrayUtils.remove(file2, file2.length-1);
		
//		for(String f1 : file1) System.out.print(f1+" ");
//		System.out.println();
//		for(String f2 : file2) System.out.print(f2+" ");
//		System.out.println();
		
		//compare two file path name
		int length = 0;
		if(file1.length < file2.length) {
			length = file1.length;
		}else {
			length = file2.length;
		}
	
		//index of the last same path name
		int lastIndex = 0;
		for(int i = 0; i < length; i++) {
			if(!file1[i].equals(file2[i])) {
				lastIndex = i;
				break;
			}else {
				lastIndex = length+1;
			}
		}
		
//		System.out.println("lastIndex : " + lastIndex);
		
		for(int i = lastIndex; i < file1.length; i++) dist.add(file1[i]);
		for(int i = lastIndex; i < file2.length; i++) dist.add(file2[i]);
		
//		System.out.println("dist" + dist);
//		System.out.println();
		return dist.size();
	}

	private static int[][] saveCombinationSet(int theNumberOfFiles, int combination) {
		int[][] caseOfCombination = new int[combination][2];
		Iterator<int[]> combinations = new Combinations(theNumberOfFiles, 2).iterator();
	    int i = 0;
	    while(combinations.hasNext()) {
	    	int[] cases = combinations.next();
    		caseOfCombination[i][0] = cases[0];
    		caseOfCombination[i][1] = cases[1];
	    	i++;
	    }
		return caseOfCombination;
	}

	private static int calculateCombination(int theNumberOfFiles) {
		Iterator<int[]> caseOfCombination = new Combinations(theNumberOfFiles, 2).iterator();
		return Iterators.size(caseOfCombination);
	}

	private static HashMap<String, TreeSet<String>> saveAuthorIdAndFilePathsInTheCurrentPeriod(Date startCommitTime,
			Date endCommitTime, TreeMap<Date, ProjectHistory> projectHistories) {
		HashMap<String, TreeSet<String>> authorID_filePaths = new HashMap<>();
		
		for(Date commitTime : projectHistories.keySet()) {
			if( (startCommitTime.before(commitTime) && endCommitTime.after(commitTime) ) 
					|| (startCommitTime.equals(commitTime)) 
					|| (endCommitTime.equals(commitTime))
				) {
				ProjectHistory projectHistory = projectHistories.get(commitTime);
				ArrayList<String> authorIds = projectHistory.getAuthorIds();
				ArrayList<String> filePaths = projectHistory.getFilePath();
				
				for(int i = 0; i < authorIds.size(); i++) {
					String authorId = authorIds.get(i);
					String filePath = filePaths.get(i);
					
					if(authorID_filePaths.containsKey(authorId)) {
						TreeSet<String> files = authorID_filePaths.get(authorId);
						files.add(filePath);
					}else {
						TreeSet<String> files = new TreeSet<>();
						files.add(filePath);
						authorID_filePaths.put(authorId, files);
					}
				}
			}
		}
		
		return authorID_filePaths;
	}

	private static TreeMap<Date,Date> saveStartAndEndCommittimeOfRefactoring(
			TreeMap<Date, ArrayList<String>> time_refactoringCommit, TreeMap<Date, ProjectHistory> projectHistories) {
		TreeSet<Date> startNendCommittime = new TreeSet<>();
		
		startNendCommittime.add(projectHistories.firstKey());
		startNendCommittime.add(projectHistories.lastKey());
		
		for(Date key : time_refactoringCommit.keySet()) {
			startNendCommittime.add(key);
		}
		
		TreeMap<Date,Date> start_endCommittime = new TreeMap<>();
		Iterator<Date> times = startNendCommittime.iterator();
		Date[] keys = projectHistories.keySet().toArray(new Date[projectHistories.size()]);
		
		Date startTime = times.next();
		Date endTime = null;
		
		while(true) {
			if(times.hasNext()) {
				endTime = times.next();
			}else {
				break;
			}

			int indexOfEndtime = Arrays.asList(keys).indexOf(endTime);
			if(indexOfEndtime-1 > 0) {
				endTime = keys[indexOfEndtime-1];
			}
			start_endCommittime.put(startTime, endTime);
			startTime = endTime;
		}
		
		return start_endCommittime;
	}

	private static TreeMap<Date, ProjectHistory> saveProjectHistoryInformation(Iterable<CSVRecord> records, TreeSet<String> refactoringCommit, TreeMap<Date, ArrayList<String>> time_refactoringCommit) throws ParseException {
		TreeMap<Date,ProjectHistory> projectHistories = new TreeMap<>();
		
		for(CSVRecord record : records) {
			String key = record.get("Key");
			Date commitTime = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(record.get("meta_data-commitTime"));
			String authorId = record.get("AuthorID");
			String hashkey = key.substring(0,key.indexOf("-"));
			String filepath = rollbackPathName(key.substring(key.indexOf("-")+1,key.length()));
			
			//save refactoring Commit time
			if(refactoringCommit.contains(hashkey)) {
				if(time_refactoringCommit.containsKey(commitTime)) {
					ArrayList<String> commits = time_refactoringCommit.get(commitTime);
					commits.add(hashkey);
				}else {
					ArrayList<String> commits = new ArrayList<>();
					commits.add(hashkey);
					time_refactoringCommit.put(commitTime, commits);
				}
			}
			
			//save Project History Information. key : commit time
			if(projectHistories.containsKey(commitTime)) {
				ProjectHistory projectHistory = projectHistories.get(commitTime);
				projectHistory.addHashkey(hashkey);
				projectHistory.addFilePath(filepath);
				projectHistory.addAuthorId(authorId);
			}else {
				ProjectHistory projectHistory = new ProjectHistory();
				projectHistory.addHashkey(hashkey);
				projectHistory.addFilePath(filepath);
				projectHistory.addAuthorId(authorId);
				projectHistories.put(commitTime, projectHistory);
			}
		}
		return projectHistories;
	}
	
	private static String rollbackPathName(String name) {
		if (name.contains("ISUJIN")) {
			name = name.replace("ISUJIN", ":");
		}
		if (name.contains(":")) {
			name = name.replace(":", File.separator);
		}
		return name;
	}

	private static TreeSet<String> readTxtFileForTest(String repositoryPath) {
		TreeSet<String> refactoringCommit = new TreeSet<>();
		
		try {
			String content = FileUtils.readFileToString(new File(repositoryPath), "UTF-8");
			String[] lines = content.split("\n");
			
			for(int i = 0; i<lines.length; i++) {
				refactoringCommit.add(lines[i]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return refactoringCommit;
	}

	private static TreeSet<String> miningRefactoringCommit(String repositoryPath) throws IOException {
		TreeSet<String> refactoringCommit = new TreeSet<>();
		
		FileWriter write = new FileWriter("tmp/result_Name.txt");
		BufferedWriter buff = new BufferedWriter(write);
		
		try {
			GitService gitService = new GitServiceImpl();
			GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
			Repository repo = gitService.openRepository(repositoryPath);
			
			miner.detectAll(repo, "master", new RefactoringHandler() {
				  
				  @Override
				  public void handle(String commitId, List<Refactoring> refactorings) {
				    
				    for (Refactoring ref : refactorings) {
		    			if(ref.getRefactoringType().equals(RefactoringType.RENAME_PACKAGE) 
	    					|| ref.getRefactoringType().equals(RefactoringType.MOVE_CLASS)
	    					|| ref.getRefactoringType().equals(RefactoringType.MOVE_RENAME_CLASS) 
	    					|| ref.getRefactoringType().equals(RefactoringType.MOVE_SOURCE_FOLDER)
	    					|| ref.getRefactoringType().equals(RefactoringType.MOVE_PACKAGE) 
	    					|| ref.getRefactoringType().equals(RefactoringType.SPLIT_PACKAGE)
	    					|| ref.getRefactoringType().equals(RefactoringType.MERGE_PACKAGE)) {
//		    				System.out.println("||	Refactorings at " + commitId+"\n");
//							System.out.println("	"+ref.toJSON()+"\n");
		    				refactoringCommit.add(commitId);
				    	}
				    }
				  }
				});
			buff.close();
			write.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return refactoringCommit;
	}

	private static String setProjectName(String input) {
		// TODO Auto-generated method stub
		return null;
	}

}

class ProjectHistory{
	ArrayList<String> hashkeys = null;
	ArrayList<String> filePath = null;
	ArrayList<String> authorIds = null;
	
	ProjectHistory(){
		this.hashkeys = new ArrayList<>();
		this.filePath = new ArrayList<>();
		this.authorIds = new ArrayList<>();
	}
	
	protected void addHashkey(String hashkey) {
		this.hashkeys.add(hashkey);
	}
	
	protected void addFilePath(String filepath) {
		this.filePath.add(filepath);
	}
	
	protected void addAuthorId(String authorId) {
		this.authorIds.add(authorId);
	}

	protected ArrayList<String> getHashkeys() {
		return hashkeys;
	}

	protected void setHashkeys(ArrayList<String> hashkeys) {
		this.hashkeys = hashkeys;
	}

	protected ArrayList<String> getFilePath() {
		return filePath;
	}

	protected void setFilePath(ArrayList<String> filePath) {
		this.filePath = filePath;
	}

	protected ArrayList<String> getAuthorIds() {
		return authorIds;
	}

	protected void setAuthorIds(ArrayList<String> authorIds) {
		this.authorIds = authorIds;
	}
	
	
}
