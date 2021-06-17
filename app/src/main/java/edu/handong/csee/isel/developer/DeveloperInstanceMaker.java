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

import edu.handong.csee.isel.ProjectInformation;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class DeveloperInstanceMaker {
	ProjectInformation projectInformation;
	
	private final static String developerIDPatternStr = "'(.+)'";
	private final static Pattern developerIDPattern = Pattern.compile(developerIDPatternStr);
	
	public DeveloperInstanceMaker(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}

	public void makeDeveloperInstanceCSV() throws Exception {
		//read CSV
		String[] developerProfilingMetrics = new String[7];
		
		developerProfilingMetrics[0] = "-m";
		developerProfilingMetrics[1] = projectInformation.getDeveloperProfilingInstanceCSVPath();
		developerProfilingMetrics[2] = "-o";
		developerProfilingMetrics[3] = projectInformation.getReferenceFolderPath();
		developerProfilingMetrics[4] = "-w";
		developerProfilingMetrics[5] = "-p";
		developerProfilingMetrics[6] = projectInformation.getProjectName();

		DeveloperProfilingMetric developerProfilingMetric = new DeveloperProfilingMetric();
		developerProfilingMetric.run(developerProfilingMetrics);
		projectInformation.setDeveloperProfilingInstanceCSVPath(developerProfilingMetric.getOutpuCSV());
	}
	
	public void applyClusteringAlgorithm() {
		try {
			HashMap<Integer,ArrayList<String>> cluster_developer = new HashMap<>();

			
			CSVLoader loader = new CSVLoader();
			loader.setSource(new File(projectInformation.getDefectInstancePath()));
//			loader.setSource(new File("/Users/yangsujin/Documents/eclipse/derby-reference/Developer_derby_PDP.csv"));
//		System.out.println(projectInformation.getDeveloperProfilingInstanceCSVPath());
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
					String[] column = dataInstance.split(",");
//					Matcher m = developerIDPattern.matcher(column[0]);
//					m.find();
//					System.out.println(column[0]);
					column[0] = column[0].substring(1, column[0].length()-1);
					
//					System.out.println(column[0]);
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
	
	private String parsingDeveloperName(String stringValue) {
		String developerName = stringValue.substring(1, stringValue.length());
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
