package edu.handong.csee.isel.model;

import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import edu.handong.csee.isel.ProjectInformation;
import edu.handong.csee.isel.data.DeveloperInfo;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.Logistic;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.MultiSearch;
import weka.classifiers.meta.multisearch.DefaultEvaluationMetrics;
import weka.classifiers.meta.multisearch.DefaultSearch;
import weka.classifiers.trees.ADTree;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.LMT;
import weka.classifiers.trees.RandomForest;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.SerializationHelper;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.setupgenerator.AbstractParameter;
import weka.core.setupgenerator.MathParameter;
import weka.filters.Filter;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.unsupervised.attribute.Remove;

public class ModelMaker {
	ProjectInformation projectInformation;
	
	public ModelMaker(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}
	
	public void makeClusterDefectModel(ArrayList<String> clusterArffPaths) throws Exception {		
		//location of model
		File clusterModelFolder = new File(projectInformation.getDefectInstancePath() +File.separator+"ClusterModel");
		String clusterModelFolderPath = clusterModelFolder.getAbsolutePath();
		clusterModelFolder.mkdir();
		
		//location of model information
		String modelInformationPath = projectInformation.getDefectInstancePath() +File.separator+"Model_Information.csv";
		File temp = new File(modelInformationPath);
		boolean isFile = temp.isFile();
		FileWriter out = new FileWriter(modelInformationPath, true); 
		CSVPrinter printer = null;
		
		if(isFile) {
			printer = new CSVPrinter(out, CSVFormat.DEFAULT);
		}else {
			printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(ClusterModelInfo.CSVHeader));
		}	
		
		for(String clusterArffPath : clusterArffPaths) {
			String clusterName = clusterArffPath.substring(clusterArffPath.lastIndexOf("/")+1,clusterArffPath.lastIndexOf("."));
			ClusterModelInfo clusterModelInfo = new ClusterModelInfo();
System.out.println(clusterName);
			DataSource source = new DataSource(clusterArffPath);
			Instances data = source.getDataSet();
			data.setClassIndex(0);
			AttributeStats attStats = data.attributeStats(0);

			int buggyIndex = data.attribute(0).indexOfValue("buggy");
			int cleanIndex = data.attribute(0).indexOfValue("clean");
			
			clusterModelInfo.setNumOfBuggy(attStats.nominalCounts[buggyIndex]);
			clusterModelInfo.setNumOfClean(attStats.nominalCounts[cleanIndex]);
			
			//Apply class imbalance algorithm
			if(projectInformation.isImb()) {
				System.out.println("Apply SMOTE");
				SMOTE smote = new SMOTE();
				int nearestNeighbor = 5;
				int percentage = 200;
				
				if((attStats.nominalCounts[buggyIndex] * 3) > attStats.nominalCounts[cleanIndex]) {
					percentage = (int)((((double)attStats.nominalCounts[cleanIndex])/(double)(attStats.nominalCounts[buggyIndex])-1) * 100);
				}
				
				smote.setInputFormat(data);
				smote.setNearestNeighbors(nearestNeighbor);
				smote.setPercentage(percentage);
				data= new Instances(Filter.useFilter(data, smote));
				
				clusterModelInfo.setClassImbalanceAlgo("SMOTE");
				clusterModelInfo.setNumOfBuggy_solveImb(attStats.nominalCounts[buggyIndex]);
				clusterModelInfo.setNumOfClean_solveImb(attStats.nominalCounts[cleanIndex]);
			}else {
				clusterModelInfo.setClassImbalanceAlgo("None");
				clusterModelInfo.setNumOfBuggy_solveImb(0);
				clusterModelInfo.setNumOfClean_solveImb(0);
			}
			
			//Apply classification algorithm
			ArrayList<String> algorithms = new ArrayList<String>(Arrays.asList("ibk","random","adt","lmt"));
			
			for(String algorithm : algorithms) {
				Classifier classifyModel = null;
				AlgorithmInfo algorithmInfo = null;
				System.out.println("Now Algorithm : "+algorithm);
				
				if(algorithm.compareTo("random") == 0) {
					classifyModel = new RandomForest();
					algorithmInfo = new AlgorithmInfo(50,200,50,"numIterations");
				}else if(algorithm.compareTo("naive") == 0){
					classifyModel = new NaiveBayes();
				}else if(algorithm.compareTo("j48") == 0){
					classifyModel = new J48();
				}else if(algorithm.compareTo("bayesNet") == 0){
					classifyModel = new BayesNet();
				}else if(algorithm.compareTo("lmt") == 0){
					classifyModel = new LMT();
					algorithmInfo = new AlgorithmInfo(20,80,20,"numBoostingIterations");
				}else if (algorithm.compareTo("ibk") == 0) {
					classifyModel = new IBk();
					algorithmInfo = new AlgorithmInfo(2,10,1,"KNN");
				}else if (algorithm.compareTo("logi") == 0) {
					classifyModel = new Logistic();
				}else if (algorithm.compareTo("adt") == 0) {
					classifyModel = new ADTree();
					algorithmInfo = new AlgorithmInfo(0,60,20,"numOfBoostingIterations");
				}
				
				//set multi_search (parpmeter tuning)
				ArrayList<String> multisearchEvaluationNames = new ArrayList<String>(Arrays.asList("Fmeasure"));
				MultiSearch multi_search = new MultiSearch();
				
				MathParameter param = new MathParameter();
				param.setProperty(algorithmInfo.getProperty()); //change according to algorithm
				param.setMin(algorithmInfo.getMin());
				param.setMax(algorithmInfo.getMax());
				param.setStep(algorithmInfo.getStep());
				param.setExpression("I");
				
				for(String multisearchEvaluationName : multisearchEvaluationNames) {
					
					SelectedTag tag = null;
					if(multisearchEvaluationName.equals("AUC")) {
						tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_AUC, new DefaultEvaluationMetrics().getTags());
					}
					else if(multisearchEvaluationName.equals("Fmeasure")) {//!
						tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_FMEASURE, new DefaultEvaluationMetrics().getTags());
					}
					else if(multisearchEvaluationName.equals("MCC")) {//!
						tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_MATTHEWS_CC, new DefaultEvaluationMetrics().getTags());
					}
					else if(multisearchEvaluationName.equals("Precision")) {
						tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_PRECISION, new DefaultEvaluationMetrics().getTags());
					}
					else if(multisearchEvaluationName.equals("Recall")) {
						tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_RECALL, new DefaultEvaluationMetrics().getTags());
					}
					
					multi_search.setSearchParameters(new AbstractParameter[]{param});
					multi_search.setEvaluation(tag);
					multi_search.setAlgorithm(new DefaultSearch());
					multi_search.setClassifier(classifyModel);
					multi_search.buildClassifier(data);
					
					//make the hashkey of model
					String input = clusterName+algorithm+algorithmInfo.getProperty()+"_"+algorithmInfo.getMin()+"_"+algorithmInfo.getMax()+"_"+algorithmInfo.getStep()+
									multisearchEvaluationName+clusterModelInfo.getNumOfBuggy()+clusterModelInfo.getNumOfClean();
					MessageDigest digest = MessageDigest.getInstance("SHA-1");
					digest.reset();
					digest.update(input.getBytes("utf8"));
					String hashKey = String.format("%64x", new BigInteger(1, digest.digest()));
					hashKey = hashKey.trim();
					
					//save model information
					List<String> informationList = new ArrayList<>();
					informationList.add(hashKey);
					informationList.add(clusterName);
					informationList.add(algorithm);
					informationList.add(Double.toString(clusterModelInfo.getNumOfBuggy()+clusterModelInfo.getNumOfClean()));
					informationList.add(Double.toString(clusterModelInfo.getNumOfBuggy()));
					informationList.add(Double.toString(clusterModelInfo.getNumOfClean()));
					informationList.add(multisearchEvaluationName);
					informationList.add(clusterModelInfo.getClassImbalanceAlgo());
					informationList.add(Double.toString(clusterModelInfo.getNumOfBuggy_solveImb()+clusterModelInfo.getNumOfClean_solveImb()));
					informationList.add(Double.toString(clusterModelInfo.getNumOfBuggy_solveImb()));
					informationList.add(Double.toString(clusterModelInfo.getNumOfClean_solveImb()));
					informationList.add(algorithmInfo.getProperty());
					informationList.add(Integer.toString(algorithmInfo.getMin()));
					informationList.add(Integer.toString(algorithmInfo.getMax()));
					informationList.add(Integer.toString(algorithmInfo.getStep()));
					printer.printRecord(informationList);
					

					//save the model
					String modelPath = clusterModelFolderPath+File.separator+
							clusterName+"_"+
							algorithm+"_"+
							hashKey+"_"+
							multisearchEvaluationName+"_"+
							clusterModelInfo.getClassImbalanceAlgo()+"_"+
							algorithmInfo.getProperty()+"_"+
							algorithmInfo.getMin()+"_"+
							algorithmInfo.getMax()+"_"+
							algorithmInfo.getStep()+
							".model";
					
					SerializationHelper.write(modelPath, multi_search);
					System.out.println("Success to save "+clusterName+"_"+algorithm);
				}
			}
		}
		printer.close();
		out.close();
	}

	public void makeDeveloperProfilingClusterModel() {
		try {
			HashMap<Integer,ArrayList<String>> cluster_developer = new HashMap<>();

			
			CSVLoader loader = new CSVLoader();
			loader.setSource(new File(projectInformation.getDefectInstancePath()));

			Instances data = loader.getDataSet();

			//delete developer ID column of CSV file
			int[] toSelect = new int[data.numAttributes()-1];

			for (int i = 0, j = 1; i < data.numAttributes()-1; i++,j++) {
				toSelect[i] = j;
			}
			
			Remove removeFilter = new Remove();
			removeFilter.setAttributeIndicesArray(toSelect);
			removeFilter.setInvertSelection(true);
			removeFilter.setInputFormat(data);
			Instances newData = Filter.useFilter(data, removeFilter);
			
			EM em = new  EM();
			
			if(projectInformation.getNumOfCluster() == 0) {
				em.buildClusterer(newData);
			}else {
				em.setNumClusters(projectInformation.getNumOfCluster());
				em.buildClusterer(newData);
			}
			
			SerializationHelper.write(projectInformation.getOutputPath()+File.separator+"EM.model", em);
			
			ClusterEvaluation eval = new ClusterEvaluation();
			eval.setClusterer(em);
			eval.evaluateClusterer(newData);
			
			int numOfCluster = eval.getNumClusters();
			
System.out.println("number Of Cluster : " + numOfCluster);
			
			double[] assignments = eval.getClusterAssignments();
			HashMap<Integer,ArrayList<String>> cluster_instances = new HashMap<>();
			
			for(int i = 0; i < assignments.length; i++) {
				int cluster = (int)assignments[i];
				String developerID = parsingDeveloperName(data.get(i).stringValue(0));
				String dataInstance = data.get(i).toString();
				
				ArrayList<String> developerList;
				ArrayList<String> dataInstances = null;
				
				if(cluster_developer.containsKey(cluster)) {
					developerList = cluster_developer.get(cluster);
					developerList.add(developerID);
					
					dataInstances = cluster_instances.get(cluster);
					dataInstances.add(dataInstance);
				}else {
					developerList = new ArrayList<>();
					developerList.add(developerID);
					cluster_developer.put(cluster, developerList);
					
					dataInstances = new ArrayList<>();
					dataInstances.add(dataInstance);
					cluster_instances.put(cluster, dataInstances);
				}
			}
			
//			for(int cluster : cluster_developer.keySet()) {
//				System.out.println("cluster : "+cluster);
//				for(String developer : cluster_developer.get(cluster)) {
//					System.out.println(developer);
//				}
//			}
			
			//save each cluster instances
			File clusterDir = new File(projectInformation.getOutputPath() +File.separator+"ClusterCSV");
			String directoryPath = clusterDir.getAbsolutePath();
			if(clusterDir.isDirectory()) {
				deleteFile(directoryPath);
			}
			clusterDir.mkdir();
			
			for(int cluster : cluster_instances.keySet()) {
//				System.out.println("cluster : "+cluster);
				FileWriter out = new FileWriter(directoryPath+File.separator+"cluster_"+cluster+".csv");
				CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(DeveloperInfo.CSVHeader));
				
				for(String dataInstance : cluster_instances.get(cluster)) {
					String developerName = dataInstance.substring(0,dataInstance.lastIndexOf("'")+1);
					
					if(developerName.contains("PersonIdent")) {
						Pattern developerIDPattern = Pattern.compile("'.+\\[,(.+),.+\\]'");
						Matcher m = developerIDPattern.matcher(developerName);
						m.find();
						dataInstance = "'"+m.group(1)+"'"+dataInstance.substring(dataInstance.lastIndexOf("'")+1,dataInstance.length());
					}
					
					String[] column = dataInstance.split(",");
					if(column[0].startsWith("'")) {
						column[0] = column[0].substring(1, column[0].length()-1);
					}
					printer.printRecord(column);
				}
				printer.flush();
				printer.close();
				out.close();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String parsingDeveloperName(String stringValue) {
		String developerName = stringValue;
		if(stringValue.startsWith(" ")) {
			developerName = stringValue.substring(1, stringValue.length());
		}
		return developerName;
	}
	
	public static void deleteFile(String path) {
		File deleteFolder = new File(path);

		if(deleteFolder.exists()){
			File[] deleteFolderList = deleteFolder.listFiles();
			
			for (int i = 0; i < deleteFolderList.length; i++) {
				if(deleteFolderList[i].isFile()) {
					deleteFolderList[i].delete();
				}else {
					deleteFile(deleteFolderList[i].getPath());
				}
				deleteFolderList[i].delete(); 
			}
			deleteFolder.delete();
		}
	}
}

class AlgorithmInfo{
	int min;
	int max;
	int step;
	String property;
	
	AlgorithmInfo(int min, int max, int step, String property) {
		this.min = min;
		this.max = max;
		this.step = step;
		this.property = property;
	}
	
	protected int getMin() {
		return min;
	}
	protected void setMin(int min) {
		this.min = min;
	}
	protected int getMax() {
		return max;
	}
	protected void setMax(int max) {
		this.max = max;
	}
	protected int getStep() {
		return step;
	}
	protected void setStep(int step) {
		this.step = step;
	}
	protected String getProperty() {
		return property;
	}
	protected void setProperty(String property) {
		this.property = property;
	}
	
	
}
