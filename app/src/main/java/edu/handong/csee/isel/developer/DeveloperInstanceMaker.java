package edu.handong.csee.isel.developer;

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
import edu.handong.csee.isel.data.ExtractData;
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

public class DeveloperInstanceMaker {
	ProjectInformation projectInformation;
	
	private final static String developerIDPatternStr = "'(.+)'";
	private final static Pattern developerIDPattern = Pattern.compile(developerIDPatternStr);
	
	private final static String firstKeyPatternStr = "@attribute\\sKey\\s\\{([^,]+)";
	private final static Pattern firstKeyPattern = Pattern.compile(firstKeyPatternStr);

	private final static String firstDeveloperIDPatternStr = ".+\\{'\\s([^,]+)',.+\\}"; 
	private final static Pattern firstDeveloperIDPattern = Pattern.compile(firstDeveloperIDPatternStr);

	public DeveloperInstanceMaker(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}

	public void makeDeveloperProfilingInstanceCSV() throws Exception {
		//read CSV
		String[] developerProfilingMetrics = new String[6];
		
		//make totalDevInstances directory
		File dir = new File(projectInformation.getOutputPath() +File.separator+"totalDevProfilingInstances");
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
			em.buildClusterer(newData);
			weka.core.SerializationHelper.write(projectInformation.getOutputPath()+File.separator+"EM.model", em);
			
			ClusterEvaluation eval = new ClusterEvaluation();
			eval.setClusterer(em);
			eval.evaluateClusterer(newData);
			
			int numOfCluster = eval.getNumClusters();
			
System.out.println(numOfCluster);
			
			double[] assignments = eval.getClusterAssignments();
			HashMap<Integer,ArrayList<String>> cluster_instances = new HashMap<>();
			
			for(int i = 0; i < assignments.length; i++) {
				int cluster = (int)assignments[i];
				String developerID = parsingDeveloperName(data.get(i).stringValue(0));
System.out.println("developerID : " + developerID);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		
		HashMap<String,Instances> developerID_data = new HashMap<>();
		
		for(int i = 0; i < authorID.numValues(); i++) {
			String developerID = parsingDeveloperName(authorID.value(i));
			String nominalToFilter = authorID.value(i);
			
			Instances filteredInstances = new Instances(newData, 0); // Empty Instances with same header
			newData.parallelStream()
			        .filter(instance -> instance.stringValue(index).equals(nominalToFilter))
			        .forEachOrdered(filteredInstances::add);
			
			developerID_data.put(developerID, filteredInstances);
			
			DataSink.write(totalDevDefectInstancesForder+File.separator+developerID+".arff", filteredInstances);
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
