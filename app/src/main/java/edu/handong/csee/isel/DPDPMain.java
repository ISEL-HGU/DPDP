package edu.handong.csee.isel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import edu.handong.csee.isel.baseline.ChangeClassification;
import edu.handong.csee.isel.baseline.PersonalizedDefectPrediction;
import edu.handong.csee.isel.data.DataFileMaker;
import edu.handong.csee.isel.model.ModelMaker;
import edu.handong.csee.isel.test.ClusterFinder;
import edu.handong.csee.isel.test.ConfusionMatrix;
import edu.handong.csee.isel.test.DPDPEvaluation;
import edu.handong.csee.isel.test.Prediction;
import edu.handong.csee.isel.test.PredictionResult;

public class DPDPMain {
	ProjectInformation projectInformation;
//	static public ArrayList<String> excludedDeveloper = new ArrayList<>();
	boolean train;
	boolean test;
	
	boolean dataMaker;
	int dataMakerMode;
	
	String weka;
	String aWeka;
	boolean clusterM;
	boolean defectM;
	
	boolean testMode_fileMaker;
	boolean testMode_clusterFinder;
	boolean testMode_prediction;
	
	boolean evaluation;
	int evaluationMode;
	
	boolean baseline;
	int baselineMode;
	
	boolean verbose;
	boolean help;
	

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		DPDPMain main = new DPDPMain();
		main.run(args);
	}
	
	private void run(String[] args) throws Exception {
		Options options = createOptions();

		if(parseOptions(options, args)){
			if (help){
				printHelp(options);
				return;
			}
			
			System.out.println("####################This project is "+ projectInformation.projectName+"####################");
			System.out.println();
			//make deverloper proriling instances
			ModelMaker modelMaker = new ModelMaker(projectInformation);
			
			//Data Maker mode
			if(train) {
				if(dataMaker) {
					DataFileMaker dataFileMaker = new DataFileMaker(projectInformation);
					
					//check is input file exist 
					File isExist = new File(projectInformation.getInputPath());
					
					if(!isExist.exists()) {
						System.out.println("The data file is not exist");
						System.out.println();
						System.out.println();
						System.exit(0);
					}
					
					switch(dataMakerMode) {
					case 1: 
						/*
						 * Software metric
						 * use options : input(i), output(o)
						*/
						dataFileMaker.makeDeveloperArff();
						break;
						
					case 2: 
						/*
						 * Developer profiling metric
						 * use options : input(i), output(o), repository path (repo)
						*/
						dataFileMaker.makeDeveloperProfilingCSV();
						break;
						
					case 3:
						dataFileMaker.makeDeveloperArff();
						dataFileMaker.makeDeveloperProfilingCSV();	
						break;
					}
				}
			}
			
//			if(!clusterM && !defectM && !test &&!evaluation && !baseline) {
//				File isExist = new File(projectInformation.getInputPath());
//				
//				if(!isExist.exists()) {
//					System.out.println("The data file is not exist");
//					System.out.println();
//					System.out.println();
//					System.exit(0);
//				}
//				
//				dataFileMaker.makeDeveloperArff();
//				dataFileMaker.makeDeveloperProfilingCSV();	
//			}
			
			if(clusterM) {
				modelMaker.makeDeveloperProfilingClusterModel();
			}
			
			if(defectM) {
				ArrayList<String> clusterArffPaths = null;
				if(weka != null){
					clusterArffPaths = readFileList(weka);
					modelMaker.makeClusterDefectModel(clusterArffPaths);
				}else if (aWeka != null){
					System.out.println(aWeka);
					clusterArffPaths = new ArrayList<>();
					clusterArffPaths.add(aWeka);
					modelMaker.makeClusterDefectModel(clusterArffPaths);
				}else if((weka == null) && (aWeka == null)){
					DataFileMaker dataFileMaker = new DataFileMaker(projectInformation);
					clusterArffPaths = dataFileMaker.makeClusterArffForTraining();
					modelMaker.makeClusterDefectModel(clusterArffPaths);
				}
			}
			
			if(test) {
				System.out.println("Test");
				String clusterFinerResultPath = null;
				
				if(projectInformation.isTestSubOption_once()) {
					testMode_fileMaker = true;
					testMode_clusterFinder = true;
					testMode_prediction = true;
				}
				
				if(testMode_fileMaker) {
					DataFileMaker dataFileMaker = new DataFileMaker(projectInformation);
					System.out.println("Test mode : fileMaker");
					
					//mk test data directory
					dataFileMaker.makeDeveloperArff();
					dataFileMaker.makeDeveloperProfilingCSV();
				}
				
				if(testMode_clusterFinder) {
					System.out.println("Test mode : clusterFinder");
					
					ClusterFinder clusteringFinder = new ClusterFinder(projectInformation);
					clusterFinerResultPath = clusteringFinder.findDeveloperCluster();
				}
				
				if(testMode_prediction) {
					System.out.println("Test mode : evaluation");
					
					clusterFinerResultPath = setClusterFinerResultPath(projectInformation,clusterFinerResultPath);
					Prediction prediction = new Prediction(projectInformation);
					HashMap<Integer, HashMap<String,ArrayList<String>> > hierachy_cluster_developers = prediction.readCsvFile(clusterFinerResultPath);
					
					prediction.predictionArffOfTestDeveloper(hierachy_cluster_developers);
				}
			}
			
			if(evaluation) {
				DPDPEvaluation eval = new DPDPEvaluation(projectInformation);
				ArrayList<PredictionResult> predictionResults = eval.readPredictedCSVfile(projectInformation.getHierarchy());
				HashMap<String, ConfusionMatrix> dev_confusionMatrix = null;
				HashMap<String, ConfusionMatrix> clu_confusionMatrix = null;
				
				switch(evaluationMode) {
				case 1:
					dev_confusionMatrix = eval.evaluateDeveloper(predictionResults);
					clu_confusionMatrix = eval.evaluateCluster(dev_confusionMatrix);
					eval.evaluateProject(clu_confusionMatrix,"DPDP");
					break;
					
				case 2:
					dev_confusionMatrix = eval.evaluateDeveloper(predictionResults);
					eval.evaluateCluster(dev_confusionMatrix);
					break;
					
				case 3:
					eval.evaluateDeveloper(predictionResults);
					break;
				}
			}
			
			if(baseline) {
				ChangeClassification cc = new ChangeClassification(projectInformation);
				PersonalizedDefectPrediction pdp = new PersonalizedDefectPrediction(projectInformation);
				HashMap<String,String> modelSetting = readModelInformationCSV(projectInformation.getModelInformationCSV());
		
				switch(baselineMode) {
				case 1: //once
					
					break;
					
				case 2: //cc
					cc.predictChangeClassification(modelSetting);
					break;
					
				case 3: //pdp
					pdp.predictPersonalizedDefectPrediction(modelSetting);
					break;
				}
			}
			
			if(verbose) {
				System.out.println("Your program is terminated. (This message is shown because you turned on -v option!");
			}
		}
		
		System.out.println();
		System.out.println();
	}

	private HashMap<String, String> readModelInformationCSV(String modelInformationCSV) throws Exception {
		HashMap<String,String> modelSetting = new HashMap<>();
		Reader in = new FileReader(modelInformationCSV);
		Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);
		
		for (CSVRecord record : records) {
			modelSetting.put("classImbalanceAlgo", record.get("classImbalanceAlgo"));
			modelSetting.put("Algorithm",record.get("Algorithm"));
			modelSetting.put("EvaluationName",record.get("EvaluationName"));
			modelSetting.put("PTproperty",record.get("PTproperty"));
			modelSetting.put("PTmin",record.get("PTmin"));
			modelSetting.put("PTmax",record.get("PTmax"));
			modelSetting.put("PTstep",record.get("PTstep"));
			break;
		}
		return modelSetting;
	}

	private String setClusterFinerResultPath(ProjectInformation projectInformation, String clusterFinerResultPath) {
		if(!projectInformation.isTestSubOption_once()) {
			return projectInformation.getLocationOfClusterModels();
		}else {
			return clusterFinerResultPath;
		}
		
	}

	private ArrayList<String> readFileList(String weka2) {
		ArrayList<String> clusterArffPaths = new ArrayList<>();
		
		File dir = new File(weka2);
		File []fileList = dir.listFiles();
		
		for(File file : fileList) {
			
			clusterArffPaths.add(file.getAbsolutePath());
		}
		
		return clusterArffPaths;
	}

	private void deleteFile(String path) {
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
	
	private boolean parseOptions(Options options, String[] args) {
		CommandLineParser parser = new DefaultParser();
		projectInformation = new ProjectInformation();

		try {
			CommandLine cmd = parser.parse(options, args);
			train = cmd.hasOption("train");
			test = cmd.hasOption("test");
			evaluation = cmd.hasOption("eval");
			
			projectInformation.setInputPath(cmd.getOptionValue("i"));
			projectInformation.setOutputPath(cmd.getOptionValue("o"));

			
//			if((clusterM&&defectM&&test)||(clusterM&&defectM)||(defectM&&test)||(clusterM&&test)) {
//				System.out.println("Wrong input!");
//				System.exit(0);
//			}
			
			if(train == true) {
				dataMaker = cmd.hasOption("dataMaker");
				clusterM = cmd.hasOption("clusterM");
				defectM = cmd.hasOption("defectM");
				
				if(!(dataMaker && !clusterM && !defectM)||
						(!dataMaker && clusterM && !defectM)||
						(!dataMaker && !clusterM && defectM)) {
					System.out.println("Wrong input!!!");
					System.exit(0);
				}else if(dataMaker) {
					if(cmd.hasOption("SwMetric")) {
						dataMakerMode = 1;
						//projectName-data.arff file path
						projectInformation.setAInputPath(cmd.getOptionValue("i"));
						projectInformation.setOutputPath(cmd.getOptionValue("o"));
						projectInformation.setProjectName(parsingArffProjectName(projectInformation.getInputPath()));
					}else if (cmd.hasOption("PfMetric")) {
						dataMakerMode = 2;
						//Label.csv file path
						projectInformation.setAInputPath(cmd.getOptionValue("i"));
						//repository path
						projectInformation.setRepositoryPath(cmd.getOptionValue("repo"));
						//number of Thread Pool
						if(cmd.hasOption("pool")) {
							projectInformation.setNumberOfThreadPool(cmd.getOptionValue("pool"));
						}else {
							projectInformation.setNumberOfThreadPool("1");
						}
						projectInformation.setOutputPath(cmd.getOptionValue("o"));
						projectInformation.setProjectName(parsingCSVProjectName(projectInformation.getInputPath()));
						
					}else if(cmd.hasOption("all")) {
						dataMakerMode = 3;
						if(cmd.hasOption("pool")) {
							projectInformation.setNumberOfThreadPool(cmd.getOptionValue("pool"));
						}else {
							projectInformation.setNumberOfThreadPool("1");
						}
						projectInformation.setInputPath(cmd.getOptionValue("i"));
						projectInformation.setRepositoryPath(cmd.getOptionValue("repo"));
						projectInformation.setOutputPath(cmd.getOptionValue("o"));
					}
				}else if(clusterM){
					
				}else if(defectM) {
					
				}
			}
			
			
			if(test == true) {
				testMode_fileMaker = cmd.hasOption("file");
				testMode_clusterFinder = cmd.hasOption("cluster");
				testMode_prediction = cmd.hasOption("pre");
				if(cmd.hasOption("once")) {
					projectInformation.setTestSubOption_once(true);
				}
				
				if(((testMode_fileMaker&&testMode_clusterFinder&&testMode_prediction)||(testMode_fileMaker&&testMode_clusterFinder)||(testMode_clusterFinder&&testMode_prediction)||(testMode_fileMaker&&testMode_prediction))||(!cmd.hasOption("once"))) {
					System.out.println("Wrong input!");
					System.exit(0);
				}
			}
			
			projectInformation.setBow(cmd.hasOption("bow"));
			projectInformation.setImb(cmd.hasOption("imb"));
			projectInformation.setLocationOfClusterModels(cmd.getOptionValue("cm"));
			projectInformation.setLocationOfDefectModels(cmd.getOptionValue("dm"));
			weka = cmd.getOptionValue("weka");
			aWeka = cmd.getOptionValue("aweka");
			
			if(cmd.getOptionValue("k") != null) {
				projectInformation.setNumOfCluster(Integer.parseInt(cmd.getOptionValue("k")));
			}else {
				projectInformation.setNumOfCluster(0);
			}
			
			if(cmd.hasOption("eval")) {
				if(cmd.hasOption("project")) {
					evaluationMode = 1;
				}else if (cmd.hasOption("cluster")) {
					evaluationMode = 2;
				}else if(cmd.hasOption("developer")) {
					evaluationMode = 3;
				}
				
				if(cmd.hasOption("al")) {
					projectInformation.setAtLeastOfCommit(Integer.parseInt(cmd.getOptionValue("al")));
				}else {
					projectInformation.setAtLeastOfCommit(0);
				}
				
				if(cmd.hasOption("algo")) {
					projectInformation.setEvaluation_algorithm(cmd.getOptionValue("algo"));
				}else {
					projectInformation.setEvaluation_algorithm("adt");
				}
				projectInformation.setHierarchy(Integer.parseInt(cmd.getOptionValue("hi")));
				projectInformation.setProjectName(cmd.getOptionValue("n"));
			}
			
			if(cmd.hasOption("base")) {
				baseline = true;
				if(cmd.hasOption("once")) {
					baselineMode = 1;
				}else if(cmd.hasOption("cc")) {
					baselineMode = 2;
				}else if (cmd.hasOption("pdp")) {
					baselineMode = 3;
				}
				
				if(cmd.hasOption("al")) {
					projectInformation.setAtLeastOfCommit(Integer.parseInt(cmd.getOptionValue("al")));
				}else {
					projectInformation.setAtLeastOfCommit(0);
				}
				
				projectInformation.setModelInformationCSV(cmd.getOptionValue("model"));
				projectInformation.setProjectName(cmd.getOptionValue("n"));
			}

			help = cmd.hasOption("h");

		} catch (Exception e) {
			printHelp(options);
			return false;
		}
		return true;
	}

	private String parsingCSVProjectName(String csv) {
		String projectName = null;
		
		Pattern pattern = Pattern.compile("(.+)/(.+)_Label.csv");
		Matcher matcher = pattern.matcher(csv);

		while(matcher.find()) {
			projectName = matcher.group(2);
		}
		if(projectName == null) {
			Pattern pattern2 = Pattern.compile("(.+)/(.+)_Label");
			Matcher matcher2 = pattern2.matcher(csv);
			while(matcher2.find()) {
				projectName = matcher2.group(2);
			}
		}
		
		return projectName;
	}

	private String parsingArffProjectName(String arff) {
		String projectName = null;
		
		Pattern pattern = Pattern.compile("(.+)/(.+)-data.arff");
		Matcher matcher = pattern.matcher(arff);

		while(matcher.find()) {
			projectName = matcher.group(2);
		}
		if(projectName == null) {
			Pattern pattern2 = Pattern.compile("(.+)/(.+)-data");
			Matcher matcher2 = pattern2.matcher(arff);
			while(matcher2.find()) {
				projectName = matcher2.group(2);
			}
		}
		
		return projectName;
	}

	private Options createOptions() {
		Options options = new Options();

		//common option
		options.addOption(Option.builder("i").longOpt("input")
				.desc("")
				.hasArg()
				.argName("URI")
				.required()// 필수
				.build());

		options.addOption(Option.builder("o").longOpt("output")
				.desc("output path. Don't use double quotation marks")
				.hasArg()
				.argName("path")
				.build());
		
		
		options.addOption(Option.builder("all").longOpt("excute all options")
				.desc("")
				.argName("do all step")
				.build());
		
		options.addOption(Option.builder("train").longOpt("train Mode")
				.desc("")
				.argName("trainMode")
				.build());
		
		options.addOption(Option.builder("test").longOpt("test Mode")
				.desc("")
				.argName("testMode")
				.build());
		
		//data option
		options.addOption(Option.builder("dataMaker").longOpt("data Maker")
				.desc("Collect the software metric or developer metric")
				.argName("dataMaker")
				.build());
		
			//data sub option -SwMetric-
			options.addOption(Option.builder("SwMetric").longOpt("Software Metric")
					.desc("collect software metric for defect prediction")
					.argName("SoftwareMetric")
					.build());
		
				//data subsub option -SwMetric- no bow
				options.addOption(Option.builder("bow").longOpt("NoBagOfWords")
						.desc("Remove the metric of Bag Of Words")
						.argName("NoBagOfWords")
						.build());
		
			//data sub option -PfMetric-
			options.addOption(Option.builder("PfMetric").longOpt("Profiling Metric")
					.desc("collect developer metric for defect prediction")
					.argName("ProfilingMetric")
					.build());
		
				//data subsub option -PfMetric- repository path
				options.addOption(Option.builder("repo").longOpt("repository")
						.desc("Remove the metric of Bag Of Words")
						.hasArg()
						.argName("NoBagOfWords")
						.build());
				
				options.addOption(Option.builder("pool").longOpt("ThreadPool")
						.desc("number of Thread Pool")
						.hasArg()
						.argName("NumOfThreadPool")
						.build());
		
		
		//not use
		options.addOption(Option.builder("imb").longOpt("applySMOTE")
				.desc("Sove imbalanced data problem using SMOTE")
				.argName("")
				.build());
		
		//train option
		options.addOption(Option.builder("clusterM").longOpt("developerClusteringModel")
				.desc("")
				.argName("")
				.build());
		
		options.addOption(Option.builder("defectM").longOpt("clusterDefectModel")
				.desc("")
				.argName("")
				.build());
		
		//train option - sub option with clusterM
		options.addOption(Option.builder("k").longOpt("numberOfEMCluster")
				.desc("Number of profiling cluster")
				.argName("")
				.hasArg()
				.build());
		
		//train option - sub option with defectM
		options.addOption(Option.builder("weka").longOpt("arff file folder path")
				.desc("make Classification Model using weka (arff file folder path)")
				.argName("")
				.hasArg()
				.build());
		
		//train option - sub option with defectM
		options.addOption(Option.builder("aweka").longOpt("one arff file")
				.desc("make Classification Model using weka (one arff file path)")
				.hasArg()
				.argName("")
				.build());
		
		//test option
		options.addOption(Option.builder("test").longOpt("testMode")
				.desc("")
				.argName("")
				.build());
		//step 1
		options.addOption(Option.builder("file").longOpt("testDataFileMaker")
				.desc("")
				.argName("")
				.build());
		//step 2
		options.addOption(Option.builder("cluster").longOpt("testClusterFinder")
				.desc("")
				.argName("")
				.build());
				
		//step 3
		options.addOption(Option.builder("pre").longOpt("testPredictionArff")
				.desc("")
				.argName("")
				.build());
		
		//test option - sub option with cluster and pre
		options.addOption(Option.builder("cm").longOpt("locationOfClusterModels")
				.desc("location of saved cluster model")
				.argName("")
				.hasArg()
				.build());
		
		//test option - sub option with pre
		options.addOption(Option.builder("dm").longOpt("locationOfDefectModels")
				.desc("location of saved defect prediction model")
				.argName("")
				.hasArg()
				.build());
		
		//test option - sub option - run all test step at once
		options.addOption(Option.builder("once").longOpt("runAllTestProcess")
				.desc("")
				.argName("")
				.build());
		
		
		//evaluation option
		options.addOption(Option.builder("eval").longOpt("evaluation")
				.desc("")
				.argName("")
				.build());
		
		options.addOption(Option.builder("hi").longOpt("hierarchy")
				.desc("hierarchy of clustering")
				.argName("")
				.hasArg()
				.build());
		
		options.addOption(Option.builder("n").longOpt("projectName")
				.desc("name of project")
				.argName("")
				.hasArg()
				.build());
		
		options.addOption(Option.builder("project").longOpt("testDataFileMaker")
				.desc("")
				.argName("")
				.build());
		
		options.addOption(Option.builder("cluster").longOpt("testDataFileMaker")
				.desc("")
				.argName("")
				.build());
		
		options.addOption(Option.builder("developer").longOpt("testDataFileMaker")
				.desc("")
				.argName("")
				.build());
		
		options.addOption(Option.builder("al").longOpt("atLeast")
				.desc("at Least (minimum commit of dev)")
				.argName("")
				.hasArg()
				.build());
		
		options.addOption(Option.builder("algo").longOpt("algorithm")
				.desc("algorithm for evaluation")
				.argName("")
				.hasArg()
				.build());
		
		//baseline cc or pdp
		options.addOption(Option.builder("base").longOpt("baseline")
				.desc("")
				.argName("")
				.build());
		
			//suboptionMode
		options.addOption(Option.builder("cc").longOpt("changeClassification")
				.desc("")
				.argName("")
				.build());
		
			//suboptionMode
		options.addOption(Option.builder("pdp").longOpt("PersonalizedDefectPrediction")
				.desc("")
				.argName("")
				.build());
		
		options.addOption(Option.builder("model").longOpt("modelCSVfile")
				.desc("model information csv file")
				.argName("")
				.hasArg()
				.build());
		
		return options;
	}

	private void printHelp(Options options) {
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		String header = "Collecting developer Meta-data program";
		String footer = "\nPlease report issues at https://github.com/HGUISEL/DAISE/issues";
		formatter.printHelp("DAISE", header, options, footer, true);
	}

}
