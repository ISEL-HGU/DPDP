package edu.handong.csee.isel.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
		String modelInformationPath = projectInformation.getOutputPath() +File.separator+projectInformation.getProjectName()+"_Test_Evaluation.csv";
		File temp = new File(modelInformationPath);
		boolean isFile = temp.isFile();
		FileWriter out = new FileWriter(modelInformationPath, true); 
		CSVPrinter printer = null;
		
		if(isFile) {
			printer = new CSVPrinter(out, CSVFormat.DEFAULT);
		}else {
			printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(EvaluationGlobalValue.CSVHeader));
		}
		
		HashMap<String, ArrayList<EvaluationGlobalValue>> globalEvaluateResult = new HashMap<>();
		File locationOfModel = new File(projectInformation.getLocationOfDefectModels());
		File []models = locationOfModel.listFiles();
		
		for(File modelPath : models) {
			Classifier DPDPclassifyModel = (Classifier) SerializationHelper.read(new FileInputStream(modelPath));
			EvaluationGlobalValue evaluationClusterValue = new EvaluationGlobalValue();
			
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
			
			System.out.println("---------------------------------------------");
			System.out.println("clusterNumber :		"+ modelNumber);
			System.out.println("modelAlgirhtm :		"+ modelAlgirhtm);
			System.out.println("EvaluationName : 	"+ multisearchEvaluationName);
			System.out.println("property :		"+ property);
			System.out.println("min :			"+ min);
			System.out.println("max :			"+ max);
			System.out.println("step :			"+ step);
			
			ArrayList<String> developers = cluster_developer.get(modelNumber);
			
			if(developers == null) continue;
			
			for(String developer : developers) {
				EvaluationInformation evaluationInformation = new EvaluationInformation();
				System.out.println("Developer : "+developer);
				String arffPath = developerDefectInstancePath.get(developer);
				
				evaluationInformation.setDeveloper(developer);
				evaluationInformation.setModelHash(modelHash);
				evaluationInformation.setClusterNumber(modelNumber);
				evaluationInformation.setAlgorithm(modelAlgirhtm);
				
				DataSource source = new DataSource(arffPath);
				Instances data = source.getDataSet();
				data.setClassIndex(0);
				AttributeStats attStats = data.attributeStats(0);

				int buggyIndex = data.attribute(0).indexOfValue("buggy");
				int cleanIndex = data.attribute(0).indexOfValue("clean");

				//if there is no buggy data, skip this developer
				if(attStats.nominalCounts[buggyIndex] == 0) continue;
				if(attStats.nominalCounts[buggyIndex]+attStats.nominalCounts[cleanIndex] < 10) continue;
				
				evaluationInformation.setNum_of_buggy(attStats.nominalCounts[buggyIndex]);
				evaluationInformation.setNum_of_clean(attStats.nominalCounts[cleanIndex]);
				
				//evaluate DPDP
				Evaluation evaluation = new Evaluation(data);
				
				evaluation.evaluateModel(DPDPclassifyModel, data);
				
				//set evaluation value
				evaluationInformation.setDPDP_precision(evaluation.precision(buggyIndex));
				evaluationInformation.setDPDP_recall(evaluation.recall(buggyIndex));
				evaluationInformation.setDPDP_fmeasure(evaluation.fMeasure(buggyIndex));
				evaluationInformation.setDPDP_mcc(evaluation.matthewsCorrelationCoefficient(buggyIndex));
				evaluationInformation.setDPDP_auc(evaluation.areaUnderROC(buggyIndex));
				
				//set confusion matrix
				evaluationInformation.setDPDP_TP(evaluation.numTruePositives(buggyIndex));
				evaluationInformation.setDPDP_FP(evaluation.numFalsePositives(buggyIndex));
				evaluationInformation.setDPDP_TN(evaluation.numTrueNegatives(buggyIndex));
				evaluationInformation.setDPDP_FN(evaluation.numFalseNegatives(buggyIndex));
				//evaluate PDP
				if(!classImbalanceAlgo.equals("None")) {
					data = ApplyClassImbalanceAlgo(data, classImbalanceAlgo,evaluationInformation);
				}
				
				evaluationInformation = evaluatePDP(evaluationInformation, data,modelAlgirhtm, multisearchEvaluationName, property, min, max, step, buggyIndex);
				
				evaluationInformation = calculateWinTieLoss(evaluationInformation);
				
				//print evaluationInformation result
				List<String> informationList = new ArrayList<>();
				informationList.add(evaluationInformation.getDeveloper());
				informationList.add(evaluationInformation.getAlgorithm());
				informationList.add(Integer.toString(evaluationInformation.getClusterNumber()));
				informationList.add(evaluationInformation.getModelHash());
				informationList.add(Integer.toString(evaluationInformation.getNum_of_clean()));
				informationList.add(Integer.toString(evaluationInformation.getNum_of_buggy()));
				informationList.add(Double.toString(evaluationInformation.getPDP_precision()));
				informationList.add(Double.toString(evaluationInformation.getPDP_recall()));
				informationList.add(Double.toString(evaluationInformation.getPDP_fmeasure()));
				informationList.add(Double.toString(evaluationInformation.getPDP_auc()));
				informationList.add(Double.toString(evaluationInformation.getPDP_mcc()));
				informationList.add(Double.toString(evaluationInformation.getDPDP_precision()));
				informationList.add(Double.toString(evaluationInformation.getDPDP_recall()));
				informationList.add(Double.toString(evaluationInformation.getDPDP_fmeasure()));
				informationList.add(Double.toString(evaluationInformation.getDPDP_auc()));
				informationList.add(Double.toString(evaluationInformation.getDPDP_mcc()));
				informationList.add(evaluationInformation.getDPDP_Precision_WTL());
				informationList.add(evaluationInformation.getDPDP_Recall_WTL());
				informationList.add(evaluationInformation.getDPDP_FMeasure_WTL());
				informationList.add(evaluationInformation.getDPDP_auc_WTL());
				informationList.add(evaluationInformation.getDPDP_mcc_WTL());
				printer.printRecord(informationList);
				//save cluster evaluation value
				evaluationClusterValue = calculateEvaluationClusterValue(evaluationClusterValue,evaluationInformation);
			}
			//if all developer of a cluster has empty buggy data set, don't save it
			if(evaluationClusterValue.getAlgorithm() == null) continue;
			
			if(globalEvaluateResult.containsKey(modelAlgirhtm)) {
				ArrayList<EvaluationGlobalValue> eval = globalEvaluateResult.get(modelAlgirhtm);
				eval.add(evaluationClusterValue);
			}else {
				ArrayList<EvaluationGlobalValue> eval = new ArrayList<>();
				eval.add(evaluationClusterValue);
				globalEvaluateResult.put(modelAlgirhtm, eval);
			}
		}
		
		HashMap<String, EvaluationGlobalValue> totalGlobalEvaluateResult = new HashMap<>();
		
		printer.printRecord("\n");
		
		printer.printRecord(
				"ClusterNumber",
				"Algorithm",
				"num of clean",
				"num of buggy",
				"PDP_precision",
				"PDP_recall",
				"PDP_fmeasure",
				"PDP_auc",
				"PDP_mcc",
				"DPDP_precision",
				"DPDP_recall",
				"DPDP_fmeasure",
				"DPDP_auc",
				"DPDP_mcc",
				"DPDP_Precision_WTL",
				"DPDP_Recall_WTL",
				"DPDP_FMeasure_WTL",
				"DPDP_auc_WTL",
				"DPDP_mcc_WTL"
				);
		
		for(String algorithm : globalEvaluateResult.keySet()) {
			ArrayList<EvaluationGlobalValue> list = globalEvaluateResult.get(algorithm);
			for(EvaluationGlobalValue evaluationClusterValue : list) {
				
				double pTP = evaluationClusterValue.getPDP_TP();
				double pFP = evaluationClusterValue.getPDP_FP();
				double pTN = evaluationClusterValue.getPDP_TN();
				double pFN = evaluationClusterValue.getPDP_FN();
				
				double dTP = evaluationClusterValue.getDPDP_TP();
				double dFP = evaluationClusterValue.getDPDP_FP();
				double dTN = evaluationClusterValue.getDPDP_TN();
				double dFN = evaluationClusterValue.getDPDP_FN();
				
				List<String> informationList = new ArrayList<>();
				informationList.add(Integer.toString(evaluationClusterValue.getClusterNumber()));
				informationList.add(evaluationClusterValue.getAlgorithm());
				informationList.add(Integer.toString(evaluationClusterValue.getNum_of_clean()));
				informationList.add(Integer.toString(evaluationClusterValue.getNum_of_buggy()));
				informationList.add(calPrecision(pTP,pFP)); //TP/(TP + FP)
				informationList.add(calRecall(pTP,pFN)); // TP/(TP + FN);
				informationList.add(calFmeasure(pTP,pFP,pFN)); //((precision * recall)/(precision + recall))*2;
				informationList.add(calAUC(pTP,pFP,pFN,pTN));
				informationList.add(calMCC(pTP,pFP,pFN,pTN));
				informationList.add(calPrecision(dTP,dFP)); //TP/(TP + FP)
				informationList.add(calRecall(dTP,dFN)); // TP/(TP + FN);
				informationList.add(calFmeasure(dTP,dFP,dFN)); //((precision * recall)/(precision + recall))*2;
				informationList.add(calAUC(dTP,dFP,dFN,dTN));
				informationList.add(calMCC(dTP,dFP,dFN,dTN));
				informationList.add(evaluationClusterValue.getP_WTL("win")+" / "+evaluationClusterValue.getP_WTL("tie")+" / "+evaluationClusterValue.getP_WTL("loss"));
				informationList.add(evaluationClusterValue.getR_WTL("win")+" / "+evaluationClusterValue.getR_WTL("tie")+" / "+evaluationClusterValue.getR_WTL("loss"));
				informationList.add(evaluationClusterValue.getF_WTL("win")+" / "+evaluationClusterValue.getF_WTL("tie")+" / "+evaluationClusterValue.getF_WTL("loss"));
				informationList.add(evaluationClusterValue.getA_WTL("win")+" / "+evaluationClusterValue.getA_WTL("tie")+" / "+evaluationClusterValue.getA_WTL("loss"));
				informationList.add(evaluationClusterValue.getM_WTL("win")+" / "+evaluationClusterValue.getM_WTL("tie")+" / "+evaluationClusterValue.getM_WTL("loss"));
				printer.printRecord(informationList);
				
				if(totalGlobalEvaluateResult.containsKey(evaluationClusterValue.getAlgorithm())) {
					EvaluationGlobalValue eval = totalGlobalEvaluateResult.get(evaluationClusterValue.getAlgorithm());
					updateGlobalValue(eval,evaluationClusterValue);
				}else {
					EvaluationGlobalValue eval = new EvaluationGlobalValue();
					updateGlobalValue(eval,evaluationClusterValue);
					totalGlobalEvaluateResult.put(evaluationClusterValue.getAlgorithm(), eval);
				}
			}
		}
		
		printer.printRecord("\n");
		
		printer.printRecord(
				"ToTal",
				"Algorithm",
				"num of clean",
				"num of buggy",
				"PDP_precision",
				"PDP_recall",
				"PDP_fmeasure",
				"PDP_auc",
				"PDP_mcc",
				"DPDP_precision",
				"DPDP_recall",
				"DPDP_fmeasure",
				"DPDP_auc",
				"DPDP_mcc",
				"DPDP_Precision_WTL",
				"DPDP_Recall_WTL",
				"DPDP_FMeasure_WTL",
				"DPDP_auc_WTL",
				"DPDP_mcc_WTL"
				);
		
		for(String algorithm : totalGlobalEvaluateResult.keySet()) {
			EvaluationGlobalValue evaluationClusterValue = totalGlobalEvaluateResult.get(algorithm);
			
			double pTP = evaluationClusterValue.getPDP_TP();
			double pFP = evaluationClusterValue.getPDP_FP();
			double pTN = evaluationClusterValue.getPDP_TN();
			double pFN = evaluationClusterValue.getPDP_FN();
			
			double dTP = evaluationClusterValue.getDPDP_TP();
			double dFP = evaluationClusterValue.getDPDP_FP();
			double dTN = evaluationClusterValue.getDPDP_TN();
			double dFN = evaluationClusterValue.getDPDP_FN();
			
			List<String> informationList = new ArrayList<>();
			informationList.add(projectInformation.getProjectName());
			informationList.add(evaluationClusterValue.getAlgorithm());
			informationList.add(Integer.toString(evaluationClusterValue.getNum_of_clean()));
			informationList.add(Integer.toString(evaluationClusterValue.getNum_of_buggy()));
			informationList.add(calPrecision(pTP,pFP)); //TP/(TP + FP)
			informationList.add(calRecall(pTP,pFN)); // TP/(TP + FN);
			informationList.add(calFmeasure(pTP,pFP,pFN)); //((precision * recall)/(precision + recall))*2;
			informationList.add(calAUC(pTP,pFP,pFN,pTN));
			informationList.add(calMCC(pTP,pFP,pFN,pTN));
			informationList.add(calPrecision(dTP,dFP)); //TP/(TP + FP)
			informationList.add(calRecall(dTP,dFN)); // TP/(TP + FN);
			informationList.add(calFmeasure(dTP,dFP,dFN)); //((precision * recall)/(precision + recall))*2;
			informationList.add(calAUC(dTP,dFP,dFN,dTN));
			informationList.add(calMCC(dTP,dFP,dFN,dTN));
			informationList.add(evaluationClusterValue.getP_WTL("win")+" / "+evaluationClusterValue.getP_WTL("tie")+" / "+evaluationClusterValue.getP_WTL("loss"));
			informationList.add(evaluationClusterValue.getR_WTL("win")+" / "+evaluationClusterValue.getR_WTL("tie")+" / "+evaluationClusterValue.getR_WTL("loss"));
			informationList.add(evaluationClusterValue.getF_WTL("win")+" / "+evaluationClusterValue.getF_WTL("tie")+" / "+evaluationClusterValue.getF_WTL("loss"));
			informationList.add(evaluationClusterValue.getA_WTL("win")+" / "+evaluationClusterValue.getA_WTL("tie")+" / "+evaluationClusterValue.getA_WTL("loss"));
			informationList.add(evaluationClusterValue.getM_WTL("win")+" / "+evaluationClusterValue.getM_WTL("tie")+" / "+evaluationClusterValue.getM_WTL("loss"));
			printer.printRecord(informationList);
		}
		
		printer.printRecord("\n");
		printer.printRecord("\n");
		
		printer.close();
		out.close();
	}
	
	private EvaluationGlobalValue updateGlobalValue(EvaluationGlobalValue eval,
			EvaluationGlobalValue evaluationClusterValue) {
		eval.setAlgorithm(evaluationClusterValue.getAlgorithm());
		
		eval.setNum_of_clean(evaluationClusterValue.getNum_of_clean());
		eval.setNum_of_buggy(evaluationClusterValue.getNum_of_buggy());
		
		eval.setPDP_TP(evaluationClusterValue.getPDP_TP());
		eval.setPDP_FP(evaluationClusterValue.getPDP_FP());
		eval.setPDP_TN(evaluationClusterValue.getPDP_TN());
		eval.setPDP_FN(evaluationClusterValue.getPDP_FN());
		
		eval.setDPDP_TP(evaluationClusterValue.getDPDP_TP());
		eval.setDPDP_FP(evaluationClusterValue.getDPDP_FP());
		eval.setDPDP_TN(evaluationClusterValue.getDPDP_TN());
		eval.setDPDP_FN(evaluationClusterValue.getDPDP_FN());
		
		eval.setP_WTL("win",evaluationClusterValue.getP_WTL("win"));
		eval.setP_WTL("tie",evaluationClusterValue.getP_WTL("tie"));
		eval.setP_WTL("loss",evaluationClusterValue.getP_WTL("loss"));
		
		eval.setR_WTL("win",evaluationClusterValue.getR_WTL("win"));
		eval.setR_WTL("tie",evaluationClusterValue.getR_WTL("tie"));
		eval.setR_WTL("loss",evaluationClusterValue.getR_WTL("loss"));
		
		eval.setF_WTL("win",evaluationClusterValue.getF_WTL("win"));
		eval.setF_WTL("tie",evaluationClusterValue.getF_WTL("tie"));
		eval.setF_WTL("loss",evaluationClusterValue.getF_WTL("loss"));
		
		eval.setM_WTL("win",evaluationClusterValue.getM_WTL("win"));
		eval.setM_WTL("tie",evaluationClusterValue.getM_WTL("tie"));
		eval.setM_WTL("loss",evaluationClusterValue.getM_WTL("loss"));
		
		eval.setA_WTL("win",evaluationClusterValue.getA_WTL("win"));
		eval.setA_WTL("tie",evaluationClusterValue.getA_WTL("tie"));
		eval.setA_WTL("loss",evaluationClusterValue.getA_WTL("loss"));
		return eval;
	}

	private String calAUC(double TP, double FP, double FN, double TN) {
		double TPR = TP/(TP+FN);
		double FPR = FP/(FP+TN);
//		X = [0;TPR;1];
//		Y = [0;FPR;1];
//		AUC = trapz(Y,X) % AUC = 0.8926
				
//		final int tpInd = tcurve.attribute(TRUE_POS_NAME).index();
//	    final int fpInd = tcurve.attribute(FALSE_POS_NAME).index();
//	    final double[] tpVals = tcurve.attributeToDoubleArray(tpInd);
//	    final double[] fpVals = tcurve.attributeToDoubleArray(fpInd);
//
//	    double area = 0.0, cumNeg = 0.0;
//	    final double totalPos = tpVals[0];
//	    final double totalNeg = fpVals[0];
//	    for (int i = 0; i < n; i++) {
//	      double cip, cin;
//	      if (i < n - 1) {
//	        cip = tpVals[i] - tpVals[i + 1];
//	        cin = fpVals[i] - fpVals[i + 1];
//	      } else {
//	        cip = tpVals[n - 1];
//	        cin = fpVals[n - 1];
//	      }
//	      area += cip * (cumNeg + (0.5 * cin));
//	      cumNeg += cin;
//	    }
//	    area /= (totalNeg * totalPos);
				
		return "???";
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

	private EvaluationGlobalValue calculateEvaluationClusterValue(EvaluationGlobalValue evaluationClusterValue,
			EvaluationInformation evaluationInformation) {
		evaluationClusterValue.setClusterNumber(evaluationInformation.getClusterNumber());
		evaluationClusterValue.setAlgorithm(evaluationInformation.getAlgorithm());
		
		evaluationClusterValue.setNum_of_clean(evaluationInformation.getNum_of_clean());
		evaluationClusterValue.setNum_of_buggy(evaluationInformation.getNum_of_buggy());
		
		evaluationClusterValue.setPDP_TP(evaluationInformation.getPDP_TP());
		evaluationClusterValue.setPDP_FP(evaluationInformation.getPDP_FP());
		evaluationClusterValue.setPDP_TN(evaluationInformation.getPDP_TN());
		evaluationClusterValue.setPDP_FN(evaluationInformation.getPDP_FN());
		
		evaluationClusterValue.setDPDP_TP(evaluationInformation.getDPDP_TP());
		evaluationClusterValue.setDPDP_FP(evaluationInformation.getDPDP_FP());
		evaluationClusterValue.setDPDP_TN(evaluationInformation.getDPDP_TN());
		evaluationClusterValue.setDPDP_FN(evaluationInformation.getDPDP_FN());
		
		evaluationClusterValue.setP_WTL(evaluationInformation.getDPDP_Precision_WTL(),1);
		evaluationClusterValue.setR_WTL(evaluationInformation.getDPDP_Recall_WTL(),1);
		evaluationClusterValue.setF_WTL(evaluationInformation.getDPDP_FMeasure_WTL(),1);
		evaluationClusterValue.setM_WTL(evaluationInformation.getDPDP_mcc_WTL(),1);
		evaluationClusterValue.setA_WTL(evaluationInformation.getDPDP_auc_WTL(),1);
		
		return evaluationClusterValue;
	}


	private EvaluationInformation evaluatePDP(EvaluationInformation evaluationInformation, Instances data, String modelAlgirhtm, String multisearchEvaluationName,
			String property, int min, int max, int step, int buggyIndex) throws Exception {
		Classifier classifyModel = null;
		
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
		
		evaluationInformation.setPDP_precision(eval.precision(buggyIndex));
		evaluationInformation.setPDP_recall(eval.recall(buggyIndex));
		evaluationInformation.setPDP_fmeasure(eval.fMeasure(buggyIndex));
		evaluationInformation.setPDP_mcc(eval.matthewsCorrelationCoefficient(buggyIndex));
		evaluationInformation.setPDP_auc(eval.areaUnderROC(buggyIndex));
		
		evaluationInformation.setPDP_TP(eval.numTruePositives(buggyIndex));
		evaluationInformation.setPDP_FP(eval.numFalsePositives(buggyIndex));
		evaluationInformation.setPDP_TN(eval.numTrueNegatives(buggyIndex));
		evaluationInformation.setPDP_FN(eval.numFalseNegatives(buggyIndex));
		
		return evaluationInformation;
	}

	private Instances ApplyClassImbalanceAlgo(Instances data, String classImbalanceAlgo, EvaluationInformation evaluationInformation) throws Exception {
		System.out.println("Apply SMOTE in PDP");
		SMOTE smote = new SMOTE();
		int nearestNeighbor = 5;
		int percentage = 200;
		
		if((evaluationInformation.getNum_of_buggy() * 3) > evaluationInformation.getNum_of_clean()) {
			percentage = (int)((((double)evaluationInformation.getNum_of_clean())/(double)(evaluationInformation.getNum_of_buggy())-1) * 100);
		}
		
		smote.setInputFormat(data);
		smote.setNearestNeighbors(nearestNeighbor);
		smote.setPercentage(percentage);
		data= new Instances(Filter.useFilter(data, smote));
		
		return data;
	}
	
private EvaluationInformation calculateWinTieLoss(EvaluationInformation evaluationInformation) {
		
		if(evaluationInformation.getDPDP_precision() > evaluationInformation.getPDP_precision()) {
			evaluationInformation.setDPDP_Precision_WTL("win");
		}else if(evaluationInformation.getDPDP_precision() == evaluationInformation.getPDP_precision()) {
			evaluationInformation.setDPDP_Precision_WTL("tie");
		}else {
			evaluationInformation.setDPDP_Precision_WTL("loss");
		}
		
		if(evaluationInformation.getDPDP_recall() > evaluationInformation.getPDP_recall()) {
			evaluationInformation.setDPDP_Recall_WTL("win");
		}else if(evaluationInformation.getDPDP_recall() == evaluationInformation.getPDP_recall()) {
			evaluationInformation.setDPDP_Recall_WTL("tie");
		}else {
			evaluationInformation.setDPDP_Recall_WTL("loss");
		}
		
		if(evaluationInformation.getDPDP_fmeasure() > evaluationInformation.getPDP_fmeasure()) {
			evaluationInformation.setDPDP_FMeasure_WTL("win");
		}else if(evaluationInformation.getDPDP_fmeasure() == evaluationInformation.getPDP_fmeasure()) {
			evaluationInformation.setDPDP_FMeasure_WTL("tie");
		}else {
			evaluationInformation.setDPDP_FMeasure_WTL("loss");
		}
		
		if(evaluationInformation.getDPDP_mcc() > evaluationInformation.getPDP_mcc()) {
			evaluationInformation.setDPDP_mcc_WTL("win");
		}else if(evaluationInformation.getDPDP_mcc() == evaluationInformation.getPDP_mcc()) {
			evaluationInformation.setDPDP_mcc_WTL("tie");
		}else {
			evaluationInformation.setDPDP_mcc_WTL("loss");
		}
		
		if(evaluationInformation.getDPDP_auc() > evaluationInformation.getPDP_auc()) {
			evaluationInformation.setDPDP_auc_WTL("win");
		}else if(evaluationInformation.getDPDP_auc() == evaluationInformation.getPDP_auc()) {
			evaluationInformation.setDPDP_auc_WTL("tie");
		}else {
			evaluationInformation.setDPDP_auc_WTL("loss");
		}
		
		return evaluationInformation;
	}
}
