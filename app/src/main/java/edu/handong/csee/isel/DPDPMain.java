package edu.handong.csee.isel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import edu.handong.csee.isel.data.DataFileMaker;
import edu.handong.csee.isel.model.ModelMaker;
import edu.handong.csee.isel.test.Testing;

public class DPDPMain {
	ProjectInformation projectInformation;
	boolean test;
	boolean clusterM;
	boolean defectM;
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
			
			System.out.println("This project is "+ projectInformation.projectName);
			
			//make deverloper proriling instances
			DataFileMaker dataFileMaker = new DataFileMaker(projectInformation);
			ModelMaker modelMaker = new ModelMaker(projectInformation);
			
			if(!clusterM && !defectM && !test) {
				File isExist = new File(projectInformation.getDefectInstancePath());
				if(!isExist.exists()) {
					System.out.println("The data file is not exist");
					System.exit(0);
				}
				dataFileMaker.makeDeveloperProfilingInstanceCSV("train");	
				dataFileMaker.makeDeveloperDefectInstanceArff("train");
			}
			
			if(clusterM) {
				modelMaker.makeDeveloperProfilingClusterModel();
			}
			
			if(defectM) {
				ArrayList<String> clusterArffPaths = dataFileMaker.makeClusterArff();
				modelMaker.makeClusterDefectModel(clusterArffPaths);
			}
			
			if(test) {
				System.out.println("Test");
				Testing testing = new Testing(projectInformation);
				
				//mk result directory
				File testDir = new File(projectInformation.getOutputPath() +File.separator+projectInformation.getProjectName());
				String directoryPath = testDir.getAbsolutePath();
				if(testDir.isDirectory()) {
					deleteFile(directoryPath);
				}
				testDir.mkdir();
				
				dataFileMaker.makeDeveloperProfilingInstanceCSV("test");	
				dataFileMaker.makeDeveloperDefectInstanceArff("test");
				
				System.out.println(projectInformation.getTestDeveloperDefectInstanceArff());
				System.out.println(projectInformation.getTestDeveloperProfilingInstanceCSV());
				
				HashMap<Integer,ArrayList<String>> cluster_developer = testing.findDeveloperCluster();
			}
			
			if(verbose) {
				System.out.println("Your program is terminated. (This message is shown because you turned on -v option!");
			}
		}
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
			projectInformation.setDefectInstancePath(cmd.getOptionValue("i"));
			projectInformation.setOutputPath(cmd.getOptionValue("o"));
			
			clusterM = cmd.hasOption("clusterM");
			defectM = cmd.hasOption("defectM");
			test = cmd.hasOption("test");
			
			if((clusterM&&defectM&&test)||(clusterM&&defectM)||(defectM&&test)||(clusterM&&test)) {
				System.out.println("Wrong input!");
				System.exit(0);
			}
			
			projectInformation.setBow(cmd.hasOption("bow"));
			projectInformation.setImb(cmd.hasOption("imb"));
			projectInformation.setLocationOfModels(cmd.getOptionValue("m"));

//			defectM = cmd.hasOption("defectM");
			help = cmd.hasOption("h");

		} catch (Exception e) {
			printHelp(options);
			return false;
		}
		return true;
	}

	private Options createOptions() {
		Options options = new Options();

		// add options by using OptionBuilder
		options.addOption(Option.builder("i").longOpt("metadata.arff")
				.desc("Address of meta data arff file. Don't use double quotation marks")
				.hasArg()
				.argName("URI")
				.required()// 필수
				.build());

		options.addOption(Option.builder("o").longOpt("output")
				.desc("output path. Don't use double quotation marks")
				.hasArg()
				.argName("path")
				.build());
		
		options.addOption(Option.builder("bow").longOpt("NoBagOfWords")
				.desc("Remove the metric of Bag Of Words")
				.argName("NoBagOfWords")
				.build());

		options.addOption(Option.builder("clusterM").longOpt("developerClusteringModel")
				.desc("")
				.argName("")
				.build());
		
		options.addOption(Option.builder("defectM").longOpt("clusterDefectModel")
				.desc("")
				.argName("")
				.build());
		
		options.addOption(Option.builder("test").longOpt("testMode")
				.desc("")
				.argName("")
				.build());
		
		options.addOption(Option.builder("imb").longOpt("applySMOTE")
				.desc("Sove imbalanced data problem using SMOTE")
				.argName("")
				.build());
		
		options.addOption(Option.builder("m").longOpt("locationOfModels")
				.desc("location of saved model")
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
