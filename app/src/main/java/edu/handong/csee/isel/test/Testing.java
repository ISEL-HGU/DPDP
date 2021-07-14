package edu.handong.csee.isel.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import edu.handong.csee.isel.ProjectInformation;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
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

public class Testing {
	ProjectInformation projectInformation;
	
	public Testing(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}

	public HashMap<Integer,ArrayList<String>> findDeveloperCluster() throws Exception {
		HashMap<Integer,ArrayList<String>> cluster_developer = new HashMap<>();
		String locationOfProfilingModel = projectInformation.getLocationOfClusterModels()+File.separator+"EM.model";
		
		CSVLoader loader = new CSVLoader();
		loader.setSource(new File(projectInformation.getTestDeveloperProfilingInstanceCSV()));

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
		
		EM em = (EM)SerializationHelper.read(new FileInputStream(locationOfProfilingModel));
		
		ClusterEvaluation eval = new ClusterEvaluation();
		eval.setClusterer(em);
		eval.evaluateClusterer(newData);
		System.out.println("------------------------------NUM cluster-----------------------  "+eval.getNumClusters());

		double[] assignments = eval.getClusterAssignments();
		
		for(int i = 0; i < assignments.length; i++) {
			int cluster = (int)assignments[i];
			String developerID = parsingDeveloperName(data.get(i).stringValue(0));
			
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
		
		return cluster_developer;
	}
	
	private String parsingDeveloperName(String stringValue) {
		String developerName = stringValue;
		if(stringValue.startsWith(" ")) {
			developerName = stringValue.substring(1, stringValue.length());
		}
		return developerName;
	}

	public void evaluateTestDeveloper(HashMap<Integer, ArrayList<String>> cluster_developer, HashMap<String, String> developerDefectInstancePath) throws Exception {
		String modelInformationPath = projectInformation.getOutputPath() +File.separator+"Test_Evaluation.csv";
		File temp = new File(modelInformationPath);
		boolean isFile = temp.isFile();
		FileWriter out = new FileWriter(modelInformationPath, true); 
		CSVPrinter printer = null;
		
		if(isFile) {
			printer = new CSVPrinter(out, CSVFormat.DEFAULT);
		}else {
			printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(EvaluationInformation.CSVHeader));
		}
		
		
		File locationOfModel = new File(projectInformation.getLocationOfDefectModels());
		File []models = locationOfModel.listFiles();
		
		for(File modelPath : models) {
			Classifier DPDPclassifyModel = (Classifier) SerializationHelper.read(new FileInputStream(modelPath));
			
			String modelPathStr = modelPath.getAbsolutePath();
			modelPathStr = modelPathStr.substring(modelPathStr.lastIndexOf("/")+1, modelPathStr.lastIndexOf("."));
			String[] modelInformation = modelPathStr.split("_");
			
			int modelNumber = Integer.parseInt(modelInformation[1]);
			String modelAlgirhtm = modelInformation[2];
			String modelHash = modelInformation[3];
			String multisearchEvaluationName = modelInformation[4];
			String classImbalanceAlgo = modelInformation[5];
			String property = modelInformation[6];
			int min = Integer.parseInt(modelInformation[7]);
			int max = Integer.parseInt(modelInformation[8]);
			int step = Integer.parseInt(modelInformation[9]);
			
			
			System.out.println("clusterNumber : "+ modelNumber + "	"+
					modelHash);
			
			ArrayList<String> developers = cluster_developer.get(modelNumber);
			
			for(String developer : developers) {
				EvaluationInformation evaluationInformation = new EvaluationInformation();

				String arffPath = developerDefectInstancePath.get(developer);
				System.out.println("developer : "+developer);
				
				evaluationInformation.setDeveloperName(developer);
				evaluationInformation.setModelHash(modelHash);
				evaluationInformation.setModelNumber(modelNumber);
				evaluationInformation.setModelAlgirhtm(modelAlgirhtm);
				
				DataSource source = new DataSource(arffPath);
				Instances data = source.getDataSet();
				data.setClassIndex(0);
				AttributeStats attStats = data.attributeStats(0);

				int buggyIndex = data.attribute(0).indexOfValue("buggy");
				int cleanIndex = data.attribute(0).indexOfValue("clean");
				
				evaluationInformation.setNumOfBuggy(attStats.nominalCounts[buggyIndex]);
				evaluationInformation.setNumOfClean(attStats.nominalCounts[cleanIndex]);
				
				//evaluate DPDP
				Evaluation evaluation = new Evaluation(data);
				
				evaluation.evaluateModel(DPDPclassifyModel, data);
				
				//write result
				System.out.println(evaluation.fMeasure(buggyIndex));
				
//				//evaluate PDP
//				if(classImbalanceAlgo != null) {
//					data = ApplyClassImbalanceAlgo(data, classImbalanceAlgo,evaluationInformation);
//				}
//				
//				EvaluationResult result = evaluatePDP(data,modelAlgirhtm, multisearchEvaluationName, property, min, max, step, buggyIndex);
				break;
			}
			
			break;
		}

	}

	private EvaluationResult evaluatePDP(Instances data, String modelAlgirhtm, String multisearchEvaluationName,
			String property, int min, int max, int step, int buggyIndex) throws Exception {
		Classifier classifyModel = null;
		System.out.println("Now Algorithm : "+modelAlgirhtm);
		
		if(modelAlgirhtm.compareTo("random") == 0) {
			classifyModel = new RandomForest();
		}else if(modelAlgirhtm.compareTo("naive") == 0){
			classifyModel = new NaiveBayes();
		}else if(modelAlgirhtm.compareTo("j48") == 0){
			classifyModel = new J48();
		}else if(modelAlgirhtm.compareTo("bayesNet") == 0){
			classifyModel = new BayesNet();
		}else if(modelAlgirhtm.compareTo("lmt") == 0){
			classifyModel = new LMT();
		}else if (modelAlgirhtm.compareTo("ibk") == 0) {
			classifyModel = new IBk();
		}else if (modelAlgirhtm.compareTo("logi") == 0) {
			classifyModel = new Logistic();
		}else if (modelAlgirhtm.compareTo("adt") == 0) {
			classifyModel = new ADTree();
		}
		
		//set multi_search (parpmeter tuning)
		MultiSearch multi_search = new MultiSearch();
		
		MathParameter param = new MathParameter();
		param.setProperty(property); //change according to algorithm
		param.setMin(min);
		param.setMax(max);
		param.setStep(step);
		param.setExpression("I");
		
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
		
		Evaluation eval = new Evaluation(data);
		eval.crossValidateModel(classifyModel, data, 10, new Random(1));
		
		EvaluationResult result = new EvaluationResult();
		result.setRecall(eval.recall(buggyIndex));
		result.setPrecision(eval.precision(buggyIndex));
		result.setfMeasure(eval.fMeasure(buggyIndex));
		result.setMCC(eval.matthewsCorrelationCoefficient(buggyIndex));
		result.setAUC(eval.areaUnderROC(buggyIndex));
		
		return result;
	}

	private Instances ApplyClassImbalanceAlgo(Instances data, String classImbalanceAlgo, EvaluationInformation evaluationInformation) throws Exception {
		System.out.println("Apply SMOTE in PDP");
		SMOTE smote = new SMOTE();
		int nearestNeighbor = 5;
		int percentage = 200;
		
		if((evaluationInformation.getNumOfBuggy() * 3) > evaluationInformation.getNumOfClean()) {
			percentage = (int)((((double)evaluationInformation.getNumOfClean())/(double)(evaluationInformation.getNumOfBuggy())-1) * 100);
		}
		
		smote.setInputFormat(data);
		smote.setNearestNeighbors(nearestNeighbor);
		smote.setPercentage(percentage);
		data= new Instances(Filter.useFilter(data, smote));
		
		return data;
	}
}
