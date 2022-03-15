package edu.handong.csee.isel.metrictest;

import java.util.List;
import java.util.TreeSet;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

public class DSmetricMain {

	public static void main(String[] args) {
		long beforeTime = System.currentTimeMillis();
		
		
		String input = "/Users/yangsujin/Documents/DPDP/ranger-reference/ranger_Label.csv";
//				args[0]; //project_label.csv
		String projectName = setProjectName(input);
		TreeSet<String> refactoringCommit = new TreeSet<>();
		
		//parsing refactoring commit
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
	
		
		long afterTime = System.currentTimeMillis(); 
		long secDiffTime = (afterTime - beforeTime)/1000;
		
		System.out.println("parsing refactoring commit 실행시(m) : "+secDiffTime/60);
		
		//read project commit history metric
		

	}

	private static String setProjectName(String input) {
		// TODO Auto-generated method stub
		return null;
	}

}
