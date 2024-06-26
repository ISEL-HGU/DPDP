package edu.handong.csee.isel.metrictest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;

import edu.handong.csee.isel.Utils;

class DSmetricCalculator implements Runnable{
	String[] file1;
	String[] file2;
	String filePath1;
	String filePath2;
	List<Integer> dists;
	List<Float> sims;
	Hashtable<String,ArrayList<String>> nameOfSemanticFiles;
	String endCommitHash;
	Date endCommitTime;
	String repositoryPath;
	TreeMap<Date, ProjectHistory> projectHistories;
	Git git;
	ArrayList<String> commitHashs;
	String projectName;
	String file1nfile2;
	Hashtable<String,Float> file1nfile2_semantic;
	List<Long> experimentTime;
	
protected DSmetricCalculator(String[] file1, String[] file2, String filePath1, String filePath2,List<Integer> dists, List<Float> sims,
			Hashtable<String, ArrayList<String>> nameOfSemanticFiles, String endCommitHash, Date endCommitTime,
			String repositoryPath, TreeMap<Date, ProjectHistory> projectHistories, Git git,
			ArrayList<String> commitHashs,String projectName, Hashtable<String,Float> file1nfile2_semantic, List<Long> experimentTime) {
		this.file1 = file1;
		this.file2 = file2;
		this.filePath1 = filePath1;
		this.filePath2 = filePath2;
		this.dists = dists;
		this.sims = sims;
		this.nameOfSemanticFiles = nameOfSemanticFiles;
		this.endCommitHash = endCommitHash;
		this.endCommitTime = endCommitTime;
		this.repositoryPath = repositoryPath;
		this.projectHistories = projectHistories;
		this.git = git;
		this.commitHashs = commitHashs;
		this.projectName = projectName;
		this.file1nfile2_semantic = file1nfile2_semantic;
		this.file1nfile2 = makeSemanticKey(filePath1,filePath2);
		this.experimentTime = experimentTime;
	}

	@Override
	public void run() {
		//2) calculate the structural scattering
		//2)-1 calculate the dist of two filePath
		int dist = calculateDistOfTwoFiles(file1,file2);
		dists.add(dist);
		
		//3) calculate the semantic scattering
		//3)-1 calculate the similarity of two filePath
		boolean isSamePackage = compareTwoFilePaths(filePath1,filePath2,nameOfSemanticFiles);
		if(isSamePackage) {
			if(!file1nfile2_semantic.containsKey(file1nfile2)) {
				long beforeTime = System.currentTimeMillis();
				float sim = calSimularityOfTwoFiles(filePath1,filePath2,endCommitTime,repositoryPath,endCommitHash,projectHistories,git,commitHashs);
				long afterTime = System.currentTimeMillis(); 
				long secDiffTime = (afterTime - beforeTime)/1000;

				if(experimentTime.get(1) < secDiffTime) {
					experimentTime.set(1, secDiffTime);
				}
				
				if(experimentTime.get(2) > secDiffTime) {
					experimentTime.set(2, secDiffTime);
				}

				sims.add(sim);
				file1nfile2_semantic.put(file1nfile2, sim);
			}else {
				Long numDup = experimentTime.get(0) + 1;
				experimentTime.set(0, numDup);
				sims.add(file1nfile2_semantic.get(file1nfile2));
			}
		}
	}
	
	private float calSimularityOfTwoFiles(String filePath1, String filePath2, Date endCommitTime,
			String repositoryPath, String endCommitHash, TreeMap<Date, ProjectHistory> projectHistories, Git git, ArrayList<String> commitHashs) {
		long threadId = Thread.currentThread().getId();
		
		float simScore = 0;
		String tempFile1 = "./"+projectName+threadId+"_1.txt";
		String tempFile2 = "./"+projectName+threadId+"_2.txt";

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
				//local
//			ProcessBuilder builder = new ProcessBuilder("/Users/yangsujin/opt/anaconda3/bin/python3","./semantic.py",tempFile1,tempFile2);
				//server
			ProcessBuilder builder = new ProcessBuilder("/usr/bin/python3","/home/yangsujin/2022DPMINERbashfile/semantic.py",tempFile1,tempFile2);

			builder.redirectErrorStream(true);
			Process process = builder.start();
			simScore = Float.parseFloat(output(process.getInputStream()).trim());
//			System.out.println("Similarity Score : "+simScore);

			//delete temp file
			new File(tempFile1).delete();
			new File(tempFile2).delete();
			process.destroy();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Error!! in python process");
			e.printStackTrace();
		}
		return simScore;
	}
	
	private String output(InputStream inputStream){
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line + System.getProperty("line.separator"));
            }
            br.close();
        } catch(IOException io) {
			System.out.println("Error!! when reading python result");
			io.printStackTrace();
        }
        return sb.toString();
    }
	
	private void writeTxtFile(String fileSource, String string){
		FileWriter fw;
		try {
			fw = new FileWriter(new File(string));
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(fileSource);
			bw.close();
			fw.close();
		} catch (IOException e) {
			System.out.println("Error!! when write Txt File");
			e.printStackTrace();
		}
	}
	
	private String getPreviousCommitHash(ArrayList<String> commitHashs, String endCommitHash) {
		int indexOfCommit = commitHashs.indexOf(endCommitHash)+1;
		
		if(commitHashs.size() <= indexOfCommit) {
//			System.out.println("Error!!! Can't not find Source Path!!!");
			return "error";
		}
		return commitHashs.get(indexOfCommit);
	}
	
	private boolean compareTwoFilePaths(String filePath1, String filePath2, Hashtable<String, ArrayList<String>> nameOfSemanticFiles) {
		String filePathPackage1 = filePath1.substring(0, filePath1.lastIndexOf(File.separator));
		String filePathPackage2 = filePath2.substring(0, filePath2.lastIndexOf(File.separator));

		boolean isSamePackage = filePathPackage1.equals(filePathPackage2);

		if(isSamePackage) {
			ArrayList<String> nameOfSemanticFile = null;
			if(nameOfSemanticFiles.containsKey(filePathPackage1)) {
				nameOfSemanticFile = nameOfSemanticFiles.get(filePathPackage1);
				if(!nameOfSemanticFile.contains(filePath1))
					nameOfSemanticFile.add(filePath1);
				if(!nameOfSemanticFile.contains(filePath2))
					nameOfSemanticFile.add(filePath2);
			}else {
				nameOfSemanticFile = new ArrayList<>();
				nameOfSemanticFile.add(filePath1);
				nameOfSemanticFile.add(filePath2);
				nameOfSemanticFiles.put(filePathPackage1, nameOfSemanticFile);
			}
		}
		
		return isSamePackage;
	}
	
	private int calculateDistOfTwoFiles(String[] file1, String[] file2) {
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
	
	private static String makeSemanticKey(String filePath1, String filePath2) {
		if(filePath1.compareTo(filePath2) > 0) {
			return filePath1+"_"+filePath2;
		}else {
			return filePath2+"_"+filePath1;
		}
	}
}
