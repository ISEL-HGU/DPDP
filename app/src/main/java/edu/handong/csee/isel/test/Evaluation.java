package edu.handong.csee.isel.test;

import java.io.File;
import java.io.FileNotFoundException;
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

public class Evaluation {
	ProjectInformation projectInformation;
	
	public Evaluation(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}
	
	public ArrayList<PredictionResult> readPredictedCSVfile(int hierarchy){
		ArrayList<PredictionResult> predictionResults = new ArrayList<>();
		String[] evaluationCSVHeader = PredictionResult.evaluationCSVHeader;
		
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
		
		printConfusionMatrixResult(clu_confusionMatrix);
		
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
		
		printConfusionMatrixResult(dev_confusionMatrix);
		
		return dev_confusionMatrix;
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
	
	private String calMCC(double TP, double FP, double FN, double TN) {
		double up = (TP*TN)-(FP*FN);
		double under = (TP + FP) * (TP + FN) * (TN +FP) * (TN+FN);
		double MCC = up/Math.sqrt(under);
		
		return Double.toString(MCC);
	}

	private String calFmeasure(double TP, double FP, double FN) {
		double recall = TP/(TP + FN);
		double precision = TP/(TP + FP);
		double fmeasure = ((precision * recall)/(precision + recall))*2;
		return Double.toString(fmeasure);
	}

	private String calRecall(double TP, double FN) {
		double recall = TP/(TP + FN);
		
		return Double.toString(recall);
	}

	private String calPrecision(double TP, double FP) {
		double precision = TP/(TP + FP);
		return Double.toString(precision);
	}

	public void evaluateProject(HashMap<String, ConfusionMatrix> clu_confusionMatrix) throws IOException {
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
		
		printConfusionMatrixResult(pro_confusionMatrix);
	}
	
	private void printConfusionMatrixResult(HashMap<String, ConfusionMatrix> key_confusionMatrix) throws IOException {
		String outputPath = projectInformation.getOutputPath() + File.separator + projectInformation.getProjectName() +"-evaluation.csv";;
		
		File temp = new File(outputPath);
		boolean isFile = temp.isFile();
		FileWriter out = new FileWriter(outputPath, true); 
		CSVPrinter printer;
		
		if(isFile) {
			printer = new CSVPrinter(out, CSVFormat.DEFAULT);
		}else {
			printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(ConfusionMatrix.evaluationHeader));
		}			
		
		
		try (printer) {
			key_confusionMatrix.forEach((key,confusionMatrix) -> {
				try {
					double TP = confusionMatrix.getTP();
					double FP = confusionMatrix.getFP();
					double TN = confusionMatrix.getTN();
					double FN = confusionMatrix.getFN();
				
					List<String> informationList = new ArrayList<>();
					informationList.add(Integer.toString(projectInformation.getHierarchy()));
					informationList.add(confusionMatrix.getCluster());
					if(key.equals(confusionMatrix.getCluster())||key.equals(projectInformation.getProjectName())) key = "-";
					informationList.add(key);
					informationList.add(Integer.toString(confusionMatrix.getNumOfClean()));
					informationList.add(Integer.toString(confusionMatrix.getNumOfBuggy()));
					informationList.add(calPrecision(TP,FP)); //TP/(TP + FP)
					informationList.add(calRecall(TP,FN)); // TP/(TP + FN);
					informationList.add(calFmeasure(TP,FP,FN)); //((precision * recall)/(precision + recall))*2;
					informationList.add(calMCC(TP,FP,FN,TN));
					informationList.add(Double.toString(TP));
					informationList.add(Double.toString(FP));
					informationList.add(Double.toString(TN));
					informationList.add(Double.toString(FN));
					printer.printRecord(informationList);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			printer.printRecord("\n");
			printer.printRecord("\n");
			
			printer.close();
			out.close();
		}
	}

}
