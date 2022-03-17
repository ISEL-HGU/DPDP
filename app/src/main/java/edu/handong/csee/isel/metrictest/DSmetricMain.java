package edu.handong.csee.isel.metrictest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

public class DSmetricMain {

	public static void main(String[] args) throws Exception {
		long beforeTime = System.currentTimeMillis();
		
		
		String input = "/Users/yangsujin/Documents/DPDP/ranger-reference/ranger_Label.csv";
		String repositoryPath = "tmp/ranger_result_Name.txt";
		String projectName = setProjectName(input);
		
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
		TreeMap<Date,Date> start_endCommittime = startAndEndCommittimeOfRefactoring(time_refactoringCommit,projectHistories);
		

		

//		for(Date commitTime : time_refactoringCommit.keySet()) {
//			
//		}
	}

	private static TreeMap<Date,Date> startAndEndCommittimeOfRefactoring(
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
			
			//subtraction millisecond in endtime
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
			Date commitTime = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss").parse(record.get("meta_data-commitTime"));
			String authorId = record.get("AuthorID");
			String hashkey = key.substring(0,key.indexOf("-"));
			String filepath = key.substring(key.indexOf("-")+1,key.length());
			
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
		
		FileWriter write = new FileWriter("tmp/ranger_result_Name.txt");
		BufferedWriter buff = new BufferedWriter(write);
		
		try {
			GitService gitService = new GitServiceImpl();
			GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
			Repository repo = gitService.openRepository("/Users/yangsujin/Desktop/reference/repositories/ranger");
			
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
//				    		System.out.println("|"+count+"|	Refactorings at " + commitId+"\n");
//							System.out.println("	"+ref.toString()+"\n");
		    				
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
