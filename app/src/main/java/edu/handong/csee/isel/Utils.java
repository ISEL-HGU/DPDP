package edu.handong.csee.isel;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import edu.handong.csee.isel.data.DeveloperInfo;
import edu.handong.csee.isel.data.MetaData;
import edu.handong.csee.isel.test.ConfusionMatrix;

public class Utils {
	public static String[] evaluationHeader = {"hierachy","Cluster","ID","NOC","NOB","Precision","Recall","Fmeasure","MCC","TP","FP","TN","FN"};
	public static String[] clusterFinderCSVHeader = {"ID","hierachy","clusterType"};
	public static String[] evaluationCSVHeader = {"ID","hierachy","clusterType","prediction","actual","match","model"};
	
	public static MetaData readMetadataCSV(String metadataPath) throws IOException {
        ArrayList<HashMap<String, String>> metricToValueMapList = new ArrayList<>();

        Reader in = new FileReader(metadataPath);
        Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);

        for (CSVRecord record : records) {
//        	if(DPDPMain.excludedDeveloper.contains(record.get("AuthorID"))) {
//        		continue;
//        	}

            HashMap<String, String> metricToValueMap = new HashMap<>();

            for (String metric : MetaData.headers) {

                metricToValueMap.put(metric, record.get(metric));
            }

            metricToValueMapList.add(metricToValueMap);
        }

        MetaData metaData = new MetaData(Arrays.asList(DeveloperInfo.CSVHeader), metricToValueMapList);
        return metaData;
    }
	
	public static void showFilesInDIr(String dirPath, TreeSet<String> array) {
	    File dir = new File(dirPath);
	    File files[] = dir.listFiles();

	    for (int i = 0; i < files.length; i++) {
	        File file = files[i];
	        if (file.isDirectory()) {
	            showFilesInDIr(file.getPath(),array);
	        } else if(file.toString().endsWith(".model")){
	        	array.add(file.toString());
	        }
	    }
	}
	
	public static void printConfusionMatrixResult(HashMap<String, ConfusionMatrix> key_confusionMatrix,ProjectInformation projectInformation, String architecture) throws IOException {
		String outputPath = projectInformation.getOutputPath() + File.separator + projectInformation.getProjectName() +"-"+architecture+"-evaluation.csv";;
		
		File temp = new File(outputPath);
		boolean isFile = temp.isFile();
		FileWriter out = new FileWriter(outputPath, true); 
		CSVPrinter printer;
		
		if(isFile) {
			printer = new CSVPrinter(out, CSVFormat.DEFAULT);
		}else {
			printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(Utils.evaluationHeader));
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
	
	private static String calMCC(double TP, double FP, double FN, double TN) {
		double up = (TP*TN)-(FP*FN);
		double under = (TP + FP) * (TP + FN) * (TN +FP) * (TN+FN);
		double MCC = up/Math.sqrt(under);
		
		return Double.toString(MCC);
	}

	private static String calFmeasure(double TP, double FP, double FN) {
		double recall = TP/(TP + FN);
		double precision = TP/(TP + FP);
		double fmeasure = ((precision * recall)/(precision + recall))*2;
		return Double.toString(fmeasure);
	}

	private static String calRecall(double TP, double FN) {
		double recall = TP/(TP + FN);
		
		return Double.toString(recall);
	}

	private static String calPrecision(double TP, double FP) {
		double precision = TP/(TP + FP);
		return Double.toString(precision);
	}
}


