package edu.handong.csee.isel.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;

import edu.handong.csee.isel.ProjectInformation;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class DataFileMaker {
	ProjectInformation projectInformation;
//	
//	private final static String developerIDPatternStr = "'(.+)'";
//	private final static Pattern developerIDPattern = Pattern.compile(developerIDPatternStr);
//	
//	private final static String firstKeyPatternStr = "@attribute\\sKey\\s\\{([^,]+)";
//	private final static Pattern firstKeyPattern = Pattern.compile(firstKeyPatternStr);
//
//	private final static String firstDeveloperIDPatternStr = ".+\\{'\\s([^,]+)',.+\\}"; 
//	private final static Pattern firstDeveloperIDPattern = Pattern.compile(firstDeveloperIDPatternStr);

	public DataFileMaker(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}

	public void makeDeveloperProfilingInstanceCSV() throws Exception {
		//read CSV
		String[] developerProfilingMetrics = new String[6];
		
		//make totalDevInstances directory
		File dir = new File(projectInformation.getOutputPath());
		if(!dir.isDirectory()) {
			dir.mkdir();
		}
		
		//total developer Instance CSV path
		String totalDeveloperInstanceCSV = dir.getAbsolutePath() + File.separator+"Developer_Profiling.csv";
		
		developerProfilingMetrics[0] = "-m";
		developerProfilingMetrics[1] = projectInformation.getDeveloperProfilingInstanceCSVPath();
		developerProfilingMetrics[2] = "-o";
		developerProfilingMetrics[3] = totalDeveloperInstanceCSV;
		developerProfilingMetrics[4] = "-p";
		developerProfilingMetrics[5] = projectInformation.getProjectName();

		DeveloperProfilingMetric developerProfilingMetric = new DeveloperProfilingMetric();
		developerProfilingMetric.run(developerProfilingMetrics);
	}
	
	public void makeDeveloperDefectInstanceArff(boolean noBow) throws Exception {
		//make totalDevInstances directory
		File dir = new File(projectInformation.getOutputPath() +File.separator+"totalDevDefectInstances");
		if(!dir.isDirectory()) {
			dir.mkdir();
		}
		
		//total developer Instance path
		String totalDevDefectInstancesForder = dir.getAbsolutePath();
		System.out.println(totalDevDefectInstancesForder);
		
		String defectDataArffPath;
		if(noBow) {
			ExtractData.main(extractDataPargs(projectInformation.getDefectInstancePath(),projectInformation.getReferenceFolderPath(),noBow));
			defectDataArffPath = ExtractData.getResultPath();
		}else {
			defectDataArffPath = projectInformation.getDefectInstancePath();
		}
//		String defectDataArffPath = "/Users/yangsujin/Documents/eclipse/derby-reference/derby-data-bow.arff";
		System.out.println(defectDataArffPath);
		
		DataSource source = new DataSource(defectDataArffPath);

		Instances data = source.getDataSet();
		
		//delete key column
		int[] toSelect = new int[data.numAttributes()-1];

		for (int i = 0, j = 0; i < data.numAttributes()-1; i++,j++) {
			toSelect[i] = j;
		}
		
		Remove removeFilter = new Remove();
		removeFilter.setAttributeIndicesArray(toSelect);
		removeFilter.setInvertSelection(true);
		removeFilter.setInputFormat(data);
		Instances newData = Filter.useFilter(data, removeFilter);
		
		//split arff file according to each developer
		Attribute authorID = newData.attribute("meta_data-AuthorID");
		int index = authorID.index();
		
		for(int i = 0; i < authorID.numValues(); i++) {
			String developerID = parsingDeveloperName(authorID.value(i));
			String nominalToFilter = authorID.value(i);
			
			Instances filteredInstances = new Instances(newData, 0); // Empty Instances with same header
			newData.parallelStream()
			        .filter(instance -> instance.stringValue(index).equals(nominalToFilter))
			        .forEachOrdered(filteredInstances::add);
			
			DataSink.write(totalDevDefectInstancesForder+File.separator+projectInformation.getProjectName()+"-"+developerID+".arff", filteredInstances);
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
	
	private String[] extractDataPargs(String arffPath, String directoryPath, boolean noBOW) {

		String[] extratPDPargs = new String[3];
		extratPDPargs[0] = arffPath;
		extratPDPargs[1] = directoryPath;
		if(noBOW == false) {
			extratPDPargs[2] = "p";
		}else {
			extratPDPargs[2] = "bow";
		}

		return extratPDPargs;
	}

}
