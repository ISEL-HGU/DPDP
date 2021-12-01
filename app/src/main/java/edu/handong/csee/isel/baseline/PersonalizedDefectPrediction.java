package edu.handong.csee.isel.baseline;

import java.io.File;
import java.util.HashMap;
import java.util.Random;

import edu.handong.csee.isel.ProjectInformation;
import edu.handong.csee.isel.Utils;
import edu.handong.csee.isel.test.ConfusionMatrix;
import edu.handong.csee.isel.test.DPDPEvaluation;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.functions.Logistic;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.MultiSearch;
import weka.classifiers.meta.multisearch.DefaultEvaluationMetrics;
import weka.classifiers.meta.multisearch.DefaultSearch;
import weka.classifiers.trees.ADTree;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.LMT;
import weka.classifiers.trees.RandomForest;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.setupgenerator.AbstractParameter;
import weka.core.setupgenerator.MathParameter;
import weka.filters.Filter;
import weka.filters.supervised.instance.SMOTE;

public class PersonalizedDefectPrediction {
	ProjectInformation projectInformation;
	
	public PersonalizedDefectPrediction(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}
	
	public void predictPersonalizedDefectPrediction(HashMap<String, String> modelSetting) throws Exception{
		String projectName = projectInformation.getProjectName();
		//read developer arff 
		File dir = new File(projectInformation.getInputPath());
		File files[] = dir.listFiles();
		
		HashMap<String,ConfusionMatrix> dev_evaluationResult = new HashMap<>();
		
		//list arff file
		for(File file : files) {
			if(!file.toString().endsWith(".arff")) continue;
			String developerID = Utils.parsingDeveloperNameFromArff(file.toString(),projectName);
			
			ConfusionMatrix cm = buildModelAndEvaluation(file.toString(),modelSetting);
			
			if(cm == null) {
//				System.out.println("the number of "+developerID+" commit is less than "+projectInformation.getAtLeastOfCommit());
				continue;
			}
			
			dev_evaluationResult.put(developerID, cm);
		}
		
		Utils.printConfusionMatrixResult(dev_evaluationResult,projectInformation,modelSetting.get("Algorithm"),"PDP");
		System.out.println("Finish print the evaluation result of each developer~! | projectName : " + projectName);
		DPDPEvaluation eval = new DPDPEvaluation(projectInformation);
		eval.evaluateProject(dev_evaluationResult,"PDP");
	}

	private ConfusionMatrix buildModelAndEvaluation(String arffPath, HashMap<String, String> modelSetting) throws Exception {
		ConfusionMatrix cm = new ConfusionMatrix();
		
		DataSource source = new DataSource(arffPath);
		Instances data = source.getDataSet();
		data.setClassIndex(0);
		AttributeStats attStats = data.attributeStats(0);
		System.out.println(arffPath);
		int buggyIndex = data.attribute(0).indexOfValue("buggy");
		int cleanIndex = data.attribute(0).indexOfValue("clean");
		int numOfBuggy = attStats.nominalCounts[buggyIndex];
		int numOfClean = attStats.nominalCounts[cleanIndex];
		int total = numOfBuggy + numOfClean;
		
		if(total < projectInformation.getAtLeastOfCommit()) {
			return null;
		}
		
		if(!modelSetting.get("classImbalanceAlgo").equals("None")) {
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
		}
		
		//set classify algorithm
		Classifier classifyModel = null;
		String algorithm = modelSetting.get("Algorithm");
		
		if(algorithm.compareTo("random") == 0) {
			classifyModel = new RandomForest();
		}else if(algorithm.compareTo("naive") == 0){
			classifyModel = new NaiveBayes();
		}else if(algorithm.compareTo("j48") == 0){
			classifyModel = new J48();
		}else if(algorithm.compareTo("bayesNet") == 0){
			classifyModel = new BayesNet();
		}else if(algorithm.compareTo("lmt") == 0){
			classifyModel = new LMT();
		}else if (algorithm.compareTo("ibk") == 0) {
			classifyModel = new IBk();
		}else if (algorithm.compareTo("logi") == 0) {
			classifyModel = new Logistic();
		}else if (algorithm.compareTo("adt") == 0) {
			classifyModel = new ADTree();
		}
		
//		MultiSearch multi_search = new MultiSearch();
//		//set multisearch option
//		MathParameter param = new MathParameter();
//		param.setProperty(modelSetting.get("PTproperty")); //change according to algorithm
//		param.setMin(Integer.parseInt(modelSetting.get("PTmin")));
//		param.setMax(Integer.parseInt(modelSetting.get("PTmax")));
//		param.setStep(Integer.parseInt(modelSetting.get("PTstep")));
//		param.setExpression("I");
//		
//		//set multisearch evaluation option
//		String multisearchEvaluationName = modelSetting.get("EvaluationName");
//		SelectedTag tag = null;
//		if(multisearchEvaluationName.equals("AUC")) {
//			tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_AUC, new DefaultEvaluationMetrics().getTags());
//		}
//		else if(multisearchEvaluationName.equals("Fmeasure")) {//!
//			tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_FMEASURE, new DefaultEvaluationMetrics().getTags());
//		}
//		else if(multisearchEvaluationName.equals("MCC")) {//!
//			tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_MATTHEWS_CC, new DefaultEvaluationMetrics().getTags());
//		}
//		else if(multisearchEvaluationName.equals("Precision")) {
//			tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_PRECISION, new DefaultEvaluationMetrics().getTags());
//		}
//		else if(multisearchEvaluationName.equals("Recall")) {
//			tag = new SelectedTag(DefaultEvaluationMetrics.EVALUATION_RECALL, new DefaultEvaluationMetrics().getTags());
//		}
//		
//		//build classify model
//		multi_search.setSearchParameters(new AbstractParameter[]{param});
//		multi_search.setEvaluation(tag);
//		multi_search.setAlgorithm(new DefaultSearch());
//		multi_search.setClassifier(classifyModel);
//		multi_search.buildClassifier(data);
		
		//evaluation
		classifyModel.buildClassifier(data);
		Evaluation evaluation = new Evaluation(data);
		evaluation.crossValidateModel(classifyModel, data, 10, new Random(1));

//		evaluation.crossValidateModel(multi_search, data, 10, new Random(1));

		cm.setNumOfBuggy(numOfBuggy);
		cm.setNumOfClean(numOfClean);
		cm.setTP(evaluation.numTruePositives(buggyIndex));
		cm.setFP(evaluation.numFalsePositives(buggyIndex));
		cm.setTN(evaluation.numTrueNegatives(buggyIndex));
		cm.setFN(evaluation.numFalseNegatives(buggyIndex));
		
		evaluation.areaUnderROC(buggyIndex);
		
		System.out.println("weka : "+ evaluation.areaUnderROC(buggyIndex));
//		System.out.println("call : "+ Utils.calAUC(cm.getTP(),cm.getFP(),cm.getFN(),cm.getTN()));
		
		return cm;
	}
}
