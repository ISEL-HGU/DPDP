package edu.handong.csee.isel.metrictest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
		TreeSet<String> refactoringCommit = null;
		
		//parsing refactoring commit
		boolean test = true;
		if(test == true) {
			refactoringCommit = readTxtFileForTest(repositoryPath);
		}else {
			refactoringCommit = miningRefactoringCommit(repositoryPath);
		}
		System.out.println("Length of refactoring commit : "+refactoringCommit.size());

		long afterTime = System.currentTimeMillis(); 
		long secDiffTime = (afterTime - beforeTime)/1000;
		System.out.println("Mining refactoring commit 실행시(m) : "+secDiffTime/60);
		
		//read and save project commit history metric
		TreeMap<Date,ProjectHistory> projectHistories = new TreeMap<>();
		Reader in = new FileReader(input);
		Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);
		
		for(CSVRecord record : records) {
			String key = record.get("Key");
			Date commitTime = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss").parse(record.get("meta_data-commitTime"));
			String hashkey = key.substring(0,key.indexOf("-"));
			String filepath = key.substring(key.indexOf("-")+1,key.length());
			
			if(projectHistories.containsKey(commitTime)) {
				ProjectHistory projectHistory = projectHistories.get(commitTime);
				projectHistory.addHashkeys(hashkey);
				projectHistory.addFilePath(filepath);
			}else {
				ProjectHistory projectHistory = new ProjectHistory();
				projectHistory.addHashkeys(hashkey);
				projectHistory.addFilePath(filepath);
				projectHistories.put(commitTime, projectHistory);
			}
			
			System.out.println(key);
			System.out.println(commitTime);
			System.out.println(hashkey);
			System.out.println(filepath);
			System.exit(0);
		}

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

	private static TreeSet<String> miningRefactoringCommit(String repositoryPath) {
		TreeSet<String> refactoringCommit = new TreeSet<>();
		
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
	
	ProjectHistory(){
		this.hashkeys = new ArrayList<>();
		this.filePath = new ArrayList<>();
	}
	
	protected void addHashkeys(String hashkey) {
		this.hashkeys.add(hashkey);
	}
	
	protected void addFilePath(String filepath) {
		this.filePath.add(filepath);
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
	
}
