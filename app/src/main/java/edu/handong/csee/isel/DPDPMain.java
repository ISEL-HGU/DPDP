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
			DeveloperInstanceMaker developerFeatureMaker = new DeveloperInstanceMaker(projectInformation);
			
			if(!projectInformation.isDoClustering()) {
				developerFeatureMaker.makeDeveloperInstanceCSV();	
			}else {
				developerFeatureMaker.applyClusteringAlgorithm();
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
			projectInformation.setDoClustering(cmd.hasOption("c"));
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
				.required()
				.build());// 필수

		options.addOption(Option.builder("o").longOpt("output")
				.desc("output path. Don't use double quotation marks")
				.hasArg()
				.argName("path")
				.required()
				.build());

		options.addOption(Option.builder("c").longOpt("clustering")
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
