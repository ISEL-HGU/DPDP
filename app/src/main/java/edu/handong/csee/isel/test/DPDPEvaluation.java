package edu.handong.csee.isel.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import edu.handong.csee.isel.ProjectInformation;
import edu.handong.csee.isel.Utils;
import weka.classifiers.evaluation.Prediction;

public class DPDPEvaluation {
	ProjectInformation projectInformation;
	
	public DPDPEvaluation(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}
	
	public ArrayList<PredictionResult> readPredictedCSVfile(int hierarchy){
		ArrayList<PredictionResult> predictionResults = new ArrayList<>();
		String[] predictionCSVHeader = Utils.predictionCSVHeader;
		String evaluationAlgorithm = projectInformation.getEvaluation_algorithm();
		try {
			Reader in = new FileReader(projectInformation.getInputPath());
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);
			
			for (CSVRecord record : records) {
				int hierachy =  Integer.parseInt(record.get(predictionCSVHeader[1]));
				if(hierachy != hierarchy) continue;
				
				String developerID = record.get(predictionCSVHeader[0]);
				String cluster = record.get(predictionCSVHeader[2]);
				String algorithm = record.get(predictionCSVHeader[3]);
				String actual = record.get(predictionCSVHeader[5]);
				boolean match = Boolean.parseBoolean(record.get(predictionCSVHeader[6]));
				String predictionHash = record.get(predictionCSVHeader[8]);
				Prediction predictionObject = getPredictionObject(predictionHash);

				if(!evaluationAlgorithm.equals(algorithm)) {
					continue;
				}

				PredictionResult predictionResult = new PredictionResult();
				predictionResult.setAuthorId(developerID);
				predictionResult.setClusterType(cluster);
				predictionResult.setAlgorithm(algorithm);
				predictionResult.setLabel(actual);
				predictionResult.setMatch(match);
				predictionResult.setPredictionObject(predictionObject);
				predictionResults.add(predictionResult);
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return predictionResults;
	}
	
	private Prediction getPredictionObject(String predictionHash) throws Exception {
		
		String inputPath = projectInformation.getInputPath();
		if(inputPath.endsWith(".csv")) {
			inputPath = inputPath.substring(0, inputPath.lastIndexOf(File.separator));
		}else {
			System.out.println("Wrong input!!!");
		}
		
		FileInputStream file = new FileInputStream(inputPath+File.separator+"PredictionObjects"+File.separator+predictionHash);
		byte[] serializedPrediction = file.readAllBytes();
		
		ByteArrayInputStream baos = new ByteArrayInputStream(serializedPrediction);
		ObjectInputStream oos = new ObjectInputStream(baos);
		
		Prediction prediction = (Prediction)oos.readObject();
		
	    oos.close();
	    baos.close();
	    
		return prediction;
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
			
			ArrayList<Prediction> predictions = dConfusionMatrix.getPredictionObjects();
			
			ConfusionMatrix confusionMatrix = null;
			
			if(clu_confusionMatrix.containsKey(cluster)) {
				confusionMatrix = clu_confusionMatrix.get(cluster);
				confusionMatrix.setNumOfClean(numOfClean);
				confusionMatrix.setNumOfBuggy(numOfBuggy);
				confusionMatrix.setTP(TP);
				confusionMatrix.setFP(FP);
				confusionMatrix.setTN(TN);
				confusionMatrix.setFN(FN);
				confusionMatrix.setPredictionObjects(predictions);
			}else {
				confusionMatrix = new ConfusionMatrix();
				confusionMatrix.setCluster(cluster);
				confusionMatrix.setNumOfClean(numOfClean);
				confusionMatrix.setNumOfBuggy(numOfBuggy);
				confusionMatrix.setTP(TP);
				confusionMatrix.setFP(FP);
				confusionMatrix.setTN(TN);
				confusionMatrix.setFN(FN);
				confusionMatrix.setPredictionObjects(predictions);
				clu_confusionMatrix.put(cluster, confusionMatrix);
			}
		});		
		
		Utils.printConfusionMatrixResult(clu_confusionMatrix,projectInformation,projectInformation.getEvaluation_algorithm(),"DPDP");
		
		return clu_confusionMatrix;
	}
	
	
	public HashMap<String, ConfusionMatrix> evaluateDeveloper(ArrayList<PredictionResult> predictionResults) throws IOException {
		HashMap<String,ConfusionMatrix> dev_confusionMatrix = new HashMap<>();
		
		predictionResults.forEach((predictionResult) -> {
			String key = predictionResult.getAuthorId();
			String actual = predictionResult.getLabel();
			boolean match = predictionResult.isMatch();
			Prediction prediction = predictionResult.getPredictionObject();
			ConfusionMatrix confusionMatrix = null;
			
			if(dev_confusionMatrix.containsKey(key)) {
				confusionMatrix = dev_confusionMatrix.get(key);
				findCaseAndSave(confusionMatrix, actual, match);
				confusionMatrix.setPredictionObjects(prediction);
			}else {
				confusionMatrix = new ConfusionMatrix();
				confusionMatrix.setCluster(predictionResult.getClusterType());
				findCaseAndSave(confusionMatrix, actual, match);
				confusionMatrix.setPredictionObjects(prediction);
				dev_confusionMatrix.put(key, confusionMatrix);
			}
		});
		
		HashMap<String, ConfusionMatrix> re_dev_confusionMatrix = null;
		if(projectInformation.getAtLeastOfCommit() > 0) {
			re_dev_confusionMatrix = saveAtLeastCommitDev(dev_confusionMatrix, projectInformation.getAtLeastOfCommit());
		}else {
			re_dev_confusionMatrix = dev_confusionMatrix;
		}
		
		Utils.printConfusionMatrixResult(re_dev_confusionMatrix,projectInformation,projectInformation.getEvaluation_algorithm(),"DPDP");
		
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
			
			ArrayList<Prediction> predictions = cConfusionMatrix.getPredictionObjects();
			
			ConfusionMatrix confusionMatrix = null;
			
			if(pro_confusionMatrix.containsKey(projectName)) {
				confusionMatrix = pro_confusionMatrix.get(projectName);
				confusionMatrix.setNumOfClean(numOfClean);
				confusionMatrix.setNumOfBuggy(numOfBuggy);
				confusionMatrix.setTP(TP);
				confusionMatrix.setFP(FP);
				confusionMatrix.setTN(TN);
				confusionMatrix.setFN(FN);
				confusionMatrix.setPredictionObjects(predictions);
			}else {
				confusionMatrix = new ConfusionMatrix();
				confusionMatrix.setCluster("-");
				confusionMatrix.setNumOfClean(numOfClean);
				confusionMatrix.setNumOfBuggy(numOfBuggy);
				confusionMatrix.setTP(TP);
				confusionMatrix.setFP(FP);
				confusionMatrix.setTN(TN);
				confusionMatrix.setFN(FN);
				confusionMatrix.setPredictionObjects(predictions);
				pro_confusionMatrix.put(projectName, confusionMatrix);
			}
		});		
		
		Utils.printConfusionMatrixResult(pro_confusionMatrix,projectInformation,projectInformation.getEvaluation_algorithm(),architecture);
	}

}
