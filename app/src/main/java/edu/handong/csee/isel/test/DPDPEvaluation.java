package edu.handong.csee.isel.test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import edu.handong.csee.isel.ProjectInformation;
import edu.handong.csee.isel.Utils;

public class DPDPEvaluation {
	ProjectInformation projectInformation;
	
	public DPDPEvaluation(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}
	
	public ArrayList<PredictionResult> readPredictedCSVfile(int hierarchy){
		ArrayList<PredictionResult> predictionResults = new ArrayList<>();
		String[] evaluationCSVHeader = Utils.evaluationCSVHeader;
		
		try {
			Reader in = new FileReader(projectInformation.getInputPath());
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);
			
			for (CSVRecord record : records) {
				int hierachy =  Integer.parseInt(record.get(evaluationCSVHeader[1]));
				if(hierachy != hierarchy) continue;
				
				String developerID = record.get(evaluationCSVHeader[0]);
				String cluster = record.get(evaluationCSVHeader[2]);
				String actual = record.get(evaluationCSVHeader[4]);
				boolean match = Boolean.parseBoolean(record.get(evaluationCSVHeader[5]));

				PredictionResult predictionResult = new PredictionResult();
				predictionResult.setAuthorId(developerID);
				predictionResult.setClusterType(cluster);
				predictionResult.setLabel(actual);
				predictionResult.setMatch(match);
				
				predictionResults.add(predictionResult);
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return predictionResults;
	}
	
	public HashMap<String, ConfusionMatrix> evaluateCluster(HashMap<String, ConfusionMatrix> dev_confusionMatrix) throws IOException {
		HashMap<String,ConfusionMatrix> clu_confusionMatrix = new HashMap<>();
		
		dev_confusionMatrix.forEach((dev, dConfusionMatrix) -> {
			String cluster = dConfusionMatrix.getCluster();
			
			int numOfClean = dConfusionMatrix.getNumOfClean();
			int numOfBuggy = dConfusionMatrix.getNumOfBuggy();
			
			double TP = dConfusionMatrix.getTP();
			double FP = dConfusionMatrix.getFP();
			double TN = dConfusionMatrix.getTN();
			double FN = dConfusionMatrix.getFN();
			
			ConfusionMatrix confusionMatrix = null;
			
			if(clu_confusionMatrix.containsKey(cluster)) {
				confusionMatrix = clu_confusionMatrix.get(cluster);
				confusionMatrix.setNumOfClean(numOfClean);
				confusionMatrix.setNumOfBuggy(numOfBuggy);
				confusionMatrix.setTP(TP);
				confusionMatrix.setFP(FP);
				confusionMatrix.setTN(TN);
				confusionMatrix.setFN(FN);
				
			}else {
				confusionMatrix = new ConfusionMatrix();
				confusionMatrix.setCluster(cluster);
				confusionMatrix.setNumOfClean(numOfClean);
				confusionMatrix.setNumOfBuggy(numOfBuggy);
				confusionMatrix.setTP(TP);
				confusionMatrix.setFP(FP);
				confusionMatrix.setTN(TN);
				confusionMatrix.setFN(FN);
				clu_confusionMatrix.put(cluster, confusionMatrix);
			}
		});		
		
		Utils.printConfusionMatrixResult(clu_confusionMatrix,projectInformation,"DPDP");
		
		return clu_confusionMatrix;
	}
	
	
	public HashMap<String, ConfusionMatrix> evaluateDeveloper(ArrayList<PredictionResult> predictionResults) throws IOException {
		HashMap<String,ConfusionMatrix> dev_confusionMatrix = new HashMap<>();
		
		predictionResults.forEach((predictionResult) -> {
			String key = predictionResult.getAuthorId();
			String actual = predictionResult.getLabel();
			boolean match = predictionResult.isMatch();
			ConfusionMatrix confusionMatrix = null;
			
			if(dev_confusionMatrix.containsKey(key)) {
				confusionMatrix = dev_confusionMatrix.get(key);
				findCaseAndSave(confusionMatrix, actual, match);
			}else {
				confusionMatrix = new ConfusionMatrix();
				confusionMatrix.setCluster(predictionResult.getClusterType());
				findCaseAndSave(confusionMatrix, actual, match);
				dev_confusionMatrix.put(key, confusionMatrix);
			}
		});
		
		HashMap<String, ConfusionMatrix> re_dev_confusionMatrix = null;
		if(projectInformation.getAtLeastOfCommit() > 0) {
			re_dev_confusionMatrix = saveAtLeastCommitDev(dev_confusionMatrix, projectInformation.getAtLeastOfCommit());
		}else {
			re_dev_confusionMatrix = dev_confusionMatrix;
		}
		
		Utils.printConfusionMatrixResult(re_dev_confusionMatrix,projectInformation,"DPDP");
		
		return re_dev_confusionMatrix;
	}

	private HashMap<String, ConfusionMatrix> saveAtLeastCommitDev(HashMap<String, ConfusionMatrix> dev_confusionMatrix, int atLeastOfCommit) {
		HashMap<String, ConfusionMatrix> re_dev_confusionMatrix = new HashMap<>();
		
		dev_confusionMatrix.forEach((dev,confusionMatrix)-> {
			int numOfClean = confusionMatrix.getNumOfClean();
			int numOfBuggy = confusionMatrix.getNumOfBuggy();
			int totalCommit = numOfClean + numOfBuggy;
			
			if(totalCommit >= atLeastOfCommit) {
				re_dev_confusionMatrix.put(dev, confusionMatrix);
			}
		});
		
		return re_dev_confusionMatrix;
	}

	private void findCaseAndSave(ConfusionMatrix confusionMatrix, String actual, boolean match) {
		if(actual.equals("buggy")) {
			if(match) {
				//* True Positive(TP) - actual : buggy, predicted : buggy
				confusionMatrix.setTP(1);
			}else {
				//* False Negative(FN) - actual : buggy, predicted : clean
				confusionMatrix.setFN(1);
			}
			confusionMatrix.setNumOfBuggy(1);
		}else {
			if(match) {
				//* True Negative(TN) - actual : clean, predicted : clean
				confusionMatrix.setTN(1);
			}else {
				//* False Positive(FP) - actual : clean, predicted : buggy
				confusionMatrix.setFP(1);
			}
			confusionMatrix.setNumOfClean(1);
		}
	}

	public void evaluateProject(HashMap<String, ConfusionMatrix> clu_confusionMatrix, String architecture) throws IOException {
		HashMap<String,ConfusionMatrix> pro_confusionMatrix = new HashMap<>();
		String projectName = projectInformation.getProjectName();
		
		clu_confusionMatrix.forEach((dev,cConfusionMatrix) -> {			
			int numOfClean = cConfusionMatrix.getNumOfClean();
			int numOfBuggy = cConfusionMatrix.getNumOfBuggy();
			
			double TP = cConfusionMatrix.getTP();
			double FP = cConfusionMatrix.getFP();
			double TN = cConfusionMatrix.getTN();
			double FN = cConfusionMatrix.getFN();
			ConfusionMatrix confusionMatrix = null;
			
			if(pro_confusionMatrix.containsKey(projectName)) {
				confusionMatrix = pro_confusionMatrix.get(projectName);
				confusionMatrix.setNumOfClean(numOfClean);
				confusionMatrix.setNumOfBuggy(numOfBuggy);
				confusionMatrix.setTP(TP);
				confusionMatrix.setFP(FP);
				confusionMatrix.setTN(TN);
				confusionMatrix.setFN(FN);
			}else {
				confusionMatrix = new ConfusionMatrix();
				confusionMatrix.setCluster("-");
				confusionMatrix.setNumOfClean(numOfClean);
				confusionMatrix.setNumOfBuggy(numOfBuggy);
				confusionMatrix.setTP(TP);
				confusionMatrix.setFP(FP);
				confusionMatrix.setTN(TN);
				confusionMatrix.setFN(FN);
				pro_confusionMatrix.put(projectName, confusionMatrix);
			}
		});		
		
		Utils.printConfusionMatrixResult(pro_confusionMatrix,projectInformation,architecture);
	}

}
