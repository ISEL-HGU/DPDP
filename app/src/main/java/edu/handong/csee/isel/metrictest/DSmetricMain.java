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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	static String input;
	static String repositoryPath;
	static String projectName;
	static String output;
	
	public static HashMap<String, DeveloperScatteringMetric> main(String[] args) throws Exception {
		long beforeTime = System.currentTimeMillis();
		System.out.println("--------------Start DS metric---------------");
		
		input = args[0];
		repositoryPath = args[1];
		projectName = args [2];
		output = args[3];
		
		Git git = Git.open(new File(repositoryPath));
		ArrayList<String> commitHashs = getCommitHashs(git);
		
		//parsing refactoring commit
		boolean test = false;
		TreeSet<String> refactoringCommit = null;
		
		if(test == true) {
			refactoringCommit = readTxtFileForTest(repositoryPath);
		}else {
			refactoringCommit = miningRefactoringCommit(repositoryPath,output);
		}
		System.out.println("~ Length of refactoring commit : "+refactoringCommit.size()+" ~");

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
//    		    Runnable CV = new CrossValidation();
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

					int maxCount = 0;
					if(combination > 5) {
						maxCount = 5;
					}else {
						maxCount = combination;
					}
					ExecutorService executor = Executors.newFixedThreadPool(maxCount);

					//start cal structural&semantic
					ArrayList<Integer> dists = new ArrayList<>();
					ArrayList<Float> sims = new ArrayList<>();
					HashMap<String,TreeSet<String>> nameOfSemanticFiles = new HashMap<>();
										
					for(int i = 0; i < combination; i++) {
						int file1Index = caseOfCombination[i][0];
						int file2Index = caseOfCombination[i][1];
						
						String[] file1 = splitPaths.get(file1Index);
						String[] file2 = splitPaths.get(file2Index);
						
						Runnable metrics = new DSmetricCalculator(file1,file2,dists,sims,nameOfSemanticFiles,endCommitHash,endCommitTime,repositoryPath,projectHistories,git,commitHashs);
						executor.execute(metrics);
					}
					executor.shutdown();
					
		    		while (!executor.isTerminated()) {
		    		}
					
					//normalize structural
					int sumDist = dists.stream().mapToInt(i -> i.intValue()).sum();
					float structural  = normalization * (float)sumDist;
					//normalize semantic
					float semantic = 0;
					if((sims.size() == 0) || nameOfSemanticFiles.isEmpty()) {
						semantic = 0;
					}else {
						float simNormalization = calSimCombination(nameOfSemanticFiles);
						float sumSim = (float)sims.stream().mapToDouble(i -> i.floatValue()).sum();
						semantic = simNormalization * (1/sumSim);
					}
					  
					System.out.println("structural : "+structural);
					System.out.println("semantic : "+semantic);
					
					//save the scattering metric result
					scatteringMetric.setStructuralScattering(structural);
					scatteringMetric.setSemanticScattering(semantic);
				}
				
				//save all DS metric from a dev in all window 
				if(developerScatteringMetrics.containsKey(authorId)) {
					ArrayList<DeveloperScatteringMetric> list = developerScatteringMetrics.get(authorId);
					list.add(scatteringMetric);
				}else {
					ArrayList<DeveloperScatteringMetric> list = new ArrayList<>();
					list.add(scatteringMetric);
					developerScatteringMetrics.put(authorId, list);
				} 
			}
			    		
    		System.out.println("Finished all threads");	
			
		});
		
		//sum all DS metric from a dev in all window 
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
		
		//for debugging
//		for(String dev : sumDeveloperScatteringMetric.keySet()) {
//			System.out.println("dev name : "+dev);
//			System.out.println("sumStructural : "+sumDeveloperScatteringMetric.get(dev).getStructuralScattering());
//			System.out.println("sumSemantic : "+sumDeveloperScatteringMetric.get(dev).getSemanticScattering());
//		}
		
		return sumDeveloperScatteringMetric;
	}

	private static float calSimCombination(HashMap<String, TreeSet<String>> nameOfSemanticFiles) {
		float combination = 0;
		for(String key : nameOfSemanticFiles.keySet()) {
			combination = combination + (float)calculateCombination(nameOfSemanticFiles.get(key).size());
		}
		return combination;
	}

	private static ArrayList<String> getCommitHashs(Git git) throws Exception {
		ArrayList<String> commitHashs = new ArrayList<>();
		Iterable<RevCommit> commits = git.log().all().call(); 
		commits.forEach((commit) -> {
			commitHashs.add(commit.getId().name());
		});
		return commitHashs;
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
			String content = FileUtils.readFileToString(new File("."+File.separator+projectName+"_refactoring_commit.txt"), "UTF-8");
			String[] lines = content.split("\n");
			
			for(int i = 0; i<lines.length; i++) {
				refactoringCommit.add(lines[i]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return refactoringCommit;
	}

	private static TreeSet<String> miningRefactoringCommit(String repositoryPath, String output) throws IOException {
		TreeSet<String> refactoringCommit = new TreeSet<>();
		FileWriter write = new FileWriter("."+File.separator+projectName+"_refactoring_commit.txt");
		BufferedWriter buff = new BufferedWriter(write);
		
		try {
			GitService gitService = new GitServiceImpl();
			GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
			System.out.println(repositoryPath);
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
		    				try {
								buff.append(commitId+"\n");
								refactoringCommit.add(commitId);
			    				System.out.println(commitId);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
				    	}
				    }
				  }
				});
			buff.close();
			write.close();
		} catch (Exception e) {
			System.out.println("refactorings : hihi");
			e.printStackTrace();
		}
	
		return refactoringCommit;
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