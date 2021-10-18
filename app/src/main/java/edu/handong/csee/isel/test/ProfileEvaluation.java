package edu.handong.csee.isel.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import edu.handong.csee.isel.ProjectInformation;
import edu.handong.csee.isel.Utils;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSource;

public class ProfileEvaluation {
	ProjectInformation projectInformation;
	
	public ProfileEvaluation(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}

	public HashMap<String, ArrayList<String>> readCsvFile(String clusterFinerResultPath) {
		HashMap<String, ArrayList<String>> cluster_developer = new HashMap<>();
		try {
			Reader in = new FileReader(clusterFinerResultPath);
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);

			for (CSVRecord record : records) {
				String developerID = record.get(PredictionResult.clusterFinderCSVHeader[0]);
				String cluster = record.get(PredictionResult.clusterFinderCSVHeader[2]);
				ArrayList<String> developerList = null;
				
				if(cluster_developer.containsKey(cluster)) {
					developerList = cluster_developer.get(cluster);
					developerList.add(developerID);
				}else {
					developerList = new ArrayList<>();
					developerList.add(developerID);
					cluster_developer.put(cluster, developerList);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return cluster_developer;
	}

	public void evaluateTestDeveloper(HashMap<String, ArrayList<String>> cluster_developer) throws Exception {
	    
		//confirm none-exist model or dev
		String inputDeveloperInstancePath = setDeveloperInstancePath(projectInformation);
		String defectModelPath = projectInformation.getLocationOfDefectModels();
		
		//init result saver
		ArrayList<PredictionResult> predictionResults = new ArrayList<>();
		
		for(String cluster : cluster_developer.keySet()) {
			ArrayList<String> devList = cluster_developer.get(cluster);
			//dev이름, cluster, model path, instance path
			devList.forEach((dev)->{
				try {
					predictionResults.addAll(evaluateEachDeveloper(dev, cluster, defectModelPath, inputDeveloperInstancePath));
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			});
		}
		
		String outputPath = projectInformation.getOutputPath() + File.separator + projectInformation.getProjectName() +"-evaluationInstances.csv";
		File temp = new File(outputPath);
		boolean isFile = temp.isFile();
		FileWriter out = new FileWriter(outputPath, true); 
		CSVPrinter printer;
		
		if(isFile) {
			printer = new CSVPrinter(out, CSVFormat.DEFAULT);
		}else {
			printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(PredictionResult.evaluationCSVHeader));
		}			
		
		try (printer) {
			predictionResults.forEach((predictionResult) -> {
				try {
					List<String> resultList = new ArrayList<>();
					resultList.add(predictionResult.getAuthorId());
					resultList.add(Integer.toString(predictionResult.getHierachy()));
					resultList.add(predictionResult.getClusterType());
					resultList.add(predictionResult.getPredict());
					resultList.add(predictionResult.getLabel());
					resultList.add(Boolean.toString(predictionResult.isMatch()));
					resultList.add(predictionResult.getModel());
					
					printer.printRecord(resultList);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			
		}
		
//		System.out.println(predictionResults.size());
	}

	private String setDeveloperInstancePath(ProjectInformation projectInformation2) {
		if(!projectInformation.isTestSubOption_once()) {
			return projectInformation.getInputPath();
		}else {
			return projectInformation.getTestDeveloperDefectInstanceArff();
		}
	}

	private ArrayList<PredictionResult> evaluateEachDeveloper(String dev, String cluster, String defectModelPath,
			String inputDeveloperInstancePath) throws Exception {
		ArrayList<PredictionResult> predictionResults = new ArrayList<>();
		
		//read all model path
		TreeSet<String> modelList = readModelList(defectModelPath);
		
		//find model and arff file
		String developerDefectInstance = inputDeveloperInstancePath + File.separator + dev + ".arff";
		String model = defectModelPath + File.separator + cluster + "_";
		for(String line : modelList) {
			if(line.startsWith(model)) {
				model = line;
				break;
			}
		}
		
		//read arff
		DataSource source = new DataSource(developerDefectInstance);
		Instances data = source.getDataSet();
		data.setClassIndex(0);
		AttributeStats attStats = data.attributeStats(0);

		int buggyIndex = data.attribute(0).indexOfValue("buggy");
			
		//parsing model information
		String modelPathStr = model;
		modelPathStr = modelPathStr.substring(modelPathStr.lastIndexOf("/")+1, modelPathStr.lastIndexOf("."));
		String[] modelInformation = modelPathStr.split("_");
		String modelHash = modelInformation[3];
		
//		printModelInformation(modelInformation, dev, cluster);
		
		//read model
		Classifier DPDPclassifyModel = (Classifier) SerializationHelper.read(new FileInputStream(model));
		
		//evaluation
		Evaluation evaluation = new Evaluation(data);
		evaluation.evaluateModel(DPDPclassifyModel, data);
		
		//save evaluation result
		evaluation.predictions().forEach((eva) -> {
			PredictionResult predictionResult = new PredictionResult();
			int depth = (int)cluster.chars().filter(c -> c == '/').count() + 1;

			if(eva.predicted() == buggyIndex) {
				predictionResult.setPredict("buggy");
			}else {
				predictionResult.setPredict("clean");
			}
			
			if(eva.actual() == buggyIndex) {
				predictionResult.setLabel("buggy");;
			}else {
				predictionResult.setLabel("clean");
			}
			
			if(eva.predicted() == eva.actual()) {
				predictionResult.setMatch(true);
			}else {
				predictionResult.setMatch(false);
			}
			
			predictionResult.setAuthorId(dev);
			predictionResult.setClusterType(cluster);
			predictionResult.setHierachy(depth);
			predictionResult.setModel(modelHash);
			predictionResults.add(predictionResult);
		});
		
		return predictionResults;
	}

	private void printModelInformation(String[] modelInformation, String dev, String cluster) {
		int modelNumber = Integer.parseInt(modelInformation[1]);
		String modelAlgirhtm = modelInformation[2];
		String modelHash = modelInformation[3];
		String multisearchEvaluationName = modelInformation[4];
		String classImbalanceAlgo = modelInformation[5];
		String property = modelInformation[6];
		int min = Integer.parseInt(modelInformation[7]);
		int max = Integer.parseInt(modelInformation[8]);
		int step = Integer.parseInt(modelInformation[9]);
		
		System.out.println("---------------------------------------------");
		System.out.println("dev :			"+ dev);
		System.out.println("cluster :			"+ cluster);
		System.out.println("---------------------------------------------");
		System.out.println("clusterNumber :		"+ modelNumber);
		System.out.println("modelAlgirhtm :		"+ modelAlgirhtm);
		System.out.println("EvaluationName : 	"+ multisearchEvaluationName);
		System.out.println("property :		"+ property);
		System.out.println("min :			"+ min);
		System.out.println("max :			"+ max);
		System.out.println("step :			"+ step);
	}

	private TreeSet<String> readModelList(String defectModelPath) {
		TreeSet<String> modelList = new TreeSet<>();
		
		File dir = new File(defectModelPath);
	    File files[] = dir.listFiles();
	
	    for (int i = 0; i < files.length; i++) {
	        File file = files[i];
	        if (file.isDirectory()) {
	        	Utils.showFilesInDIr(file.getPath(),modelList);
	        } else if(file.toString().endsWith(".model")){
	        	modelList.add(file.toString());
	        }
	    }
		return modelList;
	}
}
