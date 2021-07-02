package edu.handong.csee.isel.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import edu.handong.csee.isel.ProjectInformation;
import edu.handong.csee.isel.data.DeveloperInfo;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class ModelMaker {
	ProjectInformation projectInformation;
	
	public ModelMaker(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}
	
	public void makeClusterDefectModel() throws Exception {
		//cluster csv folder
		File clusterCSVfolder = new File(projectInformation.getDefectInstancePath()+File.separator+"ClusterCSV");
		
		//developer arff folder
		String developerArffFolder = projectInformation.getDefectInstancePath()+File.separator+"totalDevDefectInstances";
		
		File clusterModelFolder = new File(projectInformation.getDefectInstancePath() +File.separator+"ClusterModel");
		String clusterModelFolderPath = clusterModelFolder.getAbsolutePath();
		if(clusterModelFolder.isDirectory()) {
			deleteFile(clusterModelFolderPath);
		}
		clusterModelFolder.mkdir();
		
		//read csv file and save cluster_developerArff
		HashMap<String,ArrayList<String>> cluster_developerArff = new HashMap<>();
		File []fileList = clusterCSVfolder.listFiles();
		
		for(File file : fileList) {
			String clusterName = file.getName();
			clusterName = clusterName.substring(clusterName.lastIndexOf("/")+1,clusterName.lastIndexOf("."));
//System.out.println(clusterName);
			ArrayList<String> developerArff = new ArrayList<>();
			Reader in = new FileReader(file);
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);

			for (CSVRecord record : records) {
				developerArff.add(developerArffFolder+File.separator+record.get(0)+".arff");
			}
			cluster_developerArff.put(clusterName, developerArff);
		}
		
		//merge each cluster developers arff
		
		//merge 1) make total @attribute	
		for(String clusterName : cluster_developerArff.keySet()) {
			ArrayList<String> developerArffList = cluster_developerArff.get(clusterName);
			HashMap<String, Integer> attributeName_index = new HashMap<>();
			ArrayList<String> arffAttribute = new ArrayList<>();
			ArrayList<String> authorId = new ArrayList<>();
			int attributeIndex = 0;
			
System.out.println(clusterName);

			for(String developerArff : developerArffList) {
System.out.println(developerArff);

				DataSource source = new DataSource(developerArff);
				Instances data = source.getDataSet();
				
				//save authorId
				authorId.add(data.get(0).stringValue(data.attribute("meta_data-AuthorID")));

				if(attributeIndex == 0 ) {
					//init about attribte variables
					attributeIndex = initAttributeVariables(attributeIndex, data, attributeName_index, arffAttribute);
				}else {
					//check duplicate attribute (if new attribute, add it)
					attributeIndex = addNonDuplicatedAttribute(data, attributeIndex, attributeName_index, arffAttribute);
				}
			}
			
			//make new meta_data-AuthorID
			for(String line : arffAttribute) {
				System.out.println(line);
			}
//			DataSink.write("/Users/yangsujin/Documents/eclipse/PDP_result/merge.arff",clusterAttribute);
			break;
		}
		
	}

	private int addNonDuplicatedAttribute(Instances data, int attributeIndex,
			HashMap<String, Integer> attributeName_index, ArrayList<String> arffAttribute) {
		
		for(int j = 0; j < data.numAttributes(); j++) {
			Attribute attribute = data.attribute(j);
			if(! (attributeName_index.containsKey(attribute.name())) ) {
				arffAttribute.add(attributeIndex,attribute.toString());
				attributeName_index.put(attribute.name(),attributeIndex);
				attributeIndex++;
			}
		}
		
		return attributeIndex;
	}

	private int initAttributeVariables(int attributeIndex, Instances data,
			HashMap<String, Integer> attributeName_index, ArrayList<String> arffAttribute) {
		//save string of attribute
		for(attributeIndex = 0; attributeIndex < data.numAttributes(); attributeIndex++) {
			Attribute attribute = data.attribute(attributeIndex);
			arffAttribute.add(attributeIndex,attribute.toString());
			attributeName_index.put(attribute.name(), attributeIndex);
		}
		return attributeIndex;
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
			
System.out.println("number Of Cluster : " + numOfCluster);
			
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
