package edu.handong.csee.isel;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import edu.handong.csee.isel.developer.DeveloperInstanceMaker;

public class DPDPMain {
	ProjectInformation projectInformation;
	boolean clusterM;
	boolean noBow;
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
			DeveloperInstanceMaker developerInstanceMaker = new DeveloperInstanceMaker(projectInformation);
			
			if(!clusterM) {
				developerInstanceMaker.makeDeveloperProfilingInstanceCSV();	
				developerInstanceMaker.makeDeveloperDefectInstanceArff(noBow);
			}else {
				developerInstanceMaker.makeDeveloperProfilingClusterModel();
			}
			
			if(defectM) {
				
			}
			
			if(verbose) {
				System.out.println("Your program is terminated. (This message is shown because you turned on -v option!");
			}
		}
	}
	
	private boolean parseOptions(Options options, String[] args) {
		CommandLineParser parser = new DefaultParser();
		projectInformation = new ProjectInformation();

		try {
			CommandLine cmd = parser.parse(options, args);
			projectInformation.setDefectInstancePath(cmd.getOptionValue("i"));
			projectInformation.setOutputPath(cmd.getOptionValue("o"));
			if(cmd.hasOption("clusterM")) {
				clusterM = true;
			}else {
				clusterM = false;
			}
			
			if(cmd.hasOption("bow")) {
				noBow = true;
			}else{
				noBow = false;
			}
			
			defectM = cmd.hasOption("defectM");
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
