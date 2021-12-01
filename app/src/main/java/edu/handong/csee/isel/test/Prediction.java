package edu.handong.csee.isel.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
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

public class Prediction {
	ProjectInformation projectInformation;
	
	public Prediction(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}

	public HashMap<Integer, HashMap<String,ArrayList<String>> > readCsvFile(String clusterFinerResultPath) {
		HashMap<Integer, HashMap<String,ArrayList<String>> > hierachy_cluster_developers = new HashMap<>();
		String[] clusterFinderCSVHeader = Utils.clusterFinderCSVHeader;
		try {
			Reader in = new FileReader(clusterFinerResultPath);
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);

			for (CSVRecord record : records) {
				String developerID = record.get(clusterFinderCSVHeader[0]);
				int hierachy =  Integer.parseInt(record.get(clusterFinderCSVHeader[1]));
				String cluster = record.get(clusterFinderCSVHeader[2]);
				HashMap<String, ArrayList<String>> cluster_developer = null;
				
				if(hierachy_cluster_developers.containsKey(hierachy)) {
					cluster_developer = hierachy_cluster_developers.get(hierachy);
					set_cluster_developer(cluster_developer,cluster,developerID);
				}else {
					cluster_developer = new HashMap<>();
					set_cluster_developer(cluster_developer,cluster,developerID);
					hierachy_cluster_developers.put(hierachy,cluster_developer);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return hierachy_cluster_developers;
	}
	
	private void set_cluster_developer(HashMap<String, ArrayList<String>> cluster_developer, String cluster, String developerID) {
		ArrayList<String> developerList;
		if(cluster_developer.containsKey(cluster)) {
			developerList = cluster_developer.get(cluster);
			developerList.add(developerID);
		}else {
			developerList = new ArrayList<>();
			developerList.add(developerID);
			cluster_developer.put(cluster, developerList);
		}
	}

	public void predictionArffOfTestDeveloper(HashMap<Integer, HashMap<String, ArrayList<String>>> hierachy_cluster_developers) throws Exception {
	    //make directory for save prediction object of evaluation
		File predictionDir = new File(projectInformation.getOutputPath() + File.separator + "PredictionObjects");
		System.out.println(projectInformation.getOutputPath());
		String predictionDirPath = predictionDir.getAbsolutePath();
		
		if(!predictionDir.isDirectory()) {
			predictionDir.mkdir();
		}
		
		//confirm none-exist model or dev
		String inputDeveloperInstancePath = setDeveloperInstancePath(projectInformation);
		String defectModelPath = projectInformation.getLocationOfDefectModels();
		
		//init result saver
		ArrayList<PredictionResult> predictionResults = new ArrayList<>();
		
		for(int hierachy : hierachy_cluster_developers.keySet()) {
			HashMap<String, ArrayList<String>> cluster_developer = hierachy_cluster_developers.get(hierachy);
			
			for(String cluster : cluster_developer.keySet()) {
				ArrayList<String> devList = cluster_developer.get(cluster);
				//dev이름, cluster, model path, instance path
				devList.forEach((dev)->{
					try {
						predictionResults.addAll(predictEachDeveloper(hierachy, dev, cluster, defectModelPath, inputDeveloperInstancePath));
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				});
			}
		}
		
		//print prediction result
		String outputPath = projectInformation.getOutputPath() + File.separator + projectInformation.getProjectName() +"-PredictionInstances.csv";
		FileWriter out = new FileWriter(outputPath); 
		CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(Utils.predictionCSVHeader));		
		
		try (printer) {
			for(int i = 0; i < predictionResults.size(); i++) {
				PredictionResult predictionResult = predictionResults.get(i);
				String hashKey = Utils.makeHashKey(predictionResult.getAuthorId() + i);
	
				//save prediction result as csv file
				List<String> resultList = new ArrayList<>();
				resultList.add(predictionResult.getAuthorId());
				resultList.add(Integer.toString(predictionResult.getHierachy()));
				resultList.add(predictionResult.getClusterType());
				resultList.add(predictionResult.getAlgorithm());
				resultList.add(predictionResult.getPredict());
				resultList.add(predictionResult.getLabel());
				resultList.add(Boolean.toString(predictionResult.isMatch()));
				resultList.add(predictionResult.getModel());
				resultList.add(hashKey);
				printer.printRecord(resultList);
				
				
				//save prediction object 
				
					//serialize (change object into byte array)
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				
				oos.writeObject(predictionResult.getPredictionObject());
				byte[] serializedPrediction = baos.toByteArray();
				
				oos.flush();
			    oos.close();
			    baos.close();
			    
			    	//save byte array
				FileOutputStream fileOutputStream = new FileOutputStream(new File(predictionDirPath+File.separator+hashKey));
				fileOutputStream.write(serializedPrediction);

				fileOutputStream.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		printer.close();
		out.close();
		
//		System.out.println(predictionResults.size());
	}

	private String setDeveloperInstancePath(ProjectInformation projectInformation2) {
		if(!projectInformation.isTestSubOption_once()) {
			return projectInformation.getInputPath();
		}else {
			return projectInformation.getTestDeveloperDefectInstanceArff();
		}
	}

	private ArrayList<PredictionResult> predictEachDeveloper(int hierachy, String dev, String cluster, String defectModelPath,
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

		int buggyIndex = data.attribute(0).indexOfValue("buggy");
			
		//parsing model information
		String modelPathStr = model;
		modelPathStr = modelPathStr.substring(modelPathStr.lastIndexOf("/")+1, modelPathStr.lastIndexOf("."));
		String[] modelInformation = modelPathStr.split("_");
		String modelAlgorithm = modelInformation[2];
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
			
			predictionResult.setPredictionObject(eva);
			predictionResult.setAuthorId(dev);
			predictionResult.setClusterType(cluster);
			predictionResult.setHierachy(hierachy);
			predictionResult.setModel(modelHash);
			predictionResult.setAlgorithm(modelAlgorithm);
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
