package edu.handong.csee.isel.model;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import edu.handong.csee.isel.ProjectInformation;
import edu.handong.csee.isel.data.DeveloperInfo;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class ModelMaker {
	ProjectInformation projectInformation;
	
	public ModelMaker(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}
	
	public void makeClusterDefectModel() {
		//cluster csv folder
		String clusterCSVfolder = projectInformation.getDefectInstancePath()+File.separator+"ClusterCSV";
		
		//developer arff folder
		String developerArffFolder = projectInformation.getDefectInstancePath()+File.separator+"totalDevDefectInstances";
	
		
		File clusterModelFolder = new File(projectInformation.getOutputPath() +File.separator+"ClusterModel");
		String clusterModelFolderPath = clusterModelFolder.getAbsolutePath();
		if(clusterModelFolder.isDirectory()) {
			deleteFile(clusterModelFolderPath);
		}
		clusterModelFolder.mkdir();
		
		File []fileList = clusterModelFolder.listFiles();
		
		for(File file : fileList) {
			
		}
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
			e.printStackTrace();
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
}
