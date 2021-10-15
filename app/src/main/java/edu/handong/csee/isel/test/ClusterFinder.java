package edu.handong.csee.isel.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.util.TreeMap;
import java.util.TreeSet;

import edu.handong.csee.isel.ProjectInformation;
import edu.handong.csee.isel.Utils;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class ClusterFinder {
	ProjectInformation projectInformation;
	
	public ClusterFinder(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}

	public String findDeveloperCluster() throws Exception {
		HashMap<String,ArrayList<String>> cluster_developer = new HashMap<>();
		String locationOfEMmodel = projectInformation.getLocationOfClusterModels();
		
		//read EM models
		TreeSet<String> fileList = new TreeSet<>(); //save EM models path
		
		File dir = new File(locationOfEMmodel);
	    File files[] = dir.listFiles();
	
	    for (int i = 0; i < files.length; i++) {
	        File file = files[i];
	        if (file.isDirectory()) {
	        	Utils.showFilesInDIr(file.getPath(),fileList);
	        } else if(file.toString().endsWith(".model")){
	        	fileList.add(file.toString());
	        }
	    }
		
		//read test developer profiling instances
	    CSVLoader loader = new CSVLoader();
		loader.setSource(new File(projectInformation.getInputPath()));
		Instances initData = loader.getDataSet();
		
		//parsing 1st, 2nd, 3rd ... EM model information
		TreeMap<Integer,ArrayList<Clustering>> subClustering = new TreeMap<>();
		
		for(String em : fileList){
			String fullPath = em;
			String clusterName = em.replace(locationOfEMmodel, "");
			int depth = (int)clusterName.chars().filter(c -> c == '/').count();
			clusterName = clusterName.substring(1, clusterName.lastIndexOf(File.separator)+1);
			
			ArrayList<Clustering> clusterings = null;
			
			if(subClustering.containsKey(depth)) {
				clusterings = subClustering.get(depth);
				Clustering clustering = new Clustering(initData);
				clustering.setClusterName(clusterName);
				clustering.setEmPath(fullPath);
				clusterings.add(clustering);
			} else {
				clusterings = new ArrayList<>();
				Clustering clustering = new Clustering(initData);
				clustering.setClusterName(clusterName);
				clustering.setEmPath(fullPath);
				clusterings.add(clustering);
				subClustering.put(depth, clusterings);
			}
		}
		
		System.out.println(subClustering);
		
		
		//apply em model to each developer
		for(int key : subClustering.keySet()) {
			ArrayList<Clustering> clusterings = subClustering.get(key);
			
			for(Clustering clustering : clusterings) {
				String emPath = clustering.getEmPath();
				String clusterName = clustering.getClusterName();
				Instances data = null;
				
				if(key == 1) {
					data = new Instances(initData);
				}else {
					data = clustering.getData();
				}
				
				Instances newData = removeDeveloperIDColumn(data);
				
				EM em = (EM)SerializationHelper.read(new FileInputStream(emPath));
				
				ClusterEvaluation eval = new ClusterEvaluation();
				eval.setClusterer(em);
				eval.evaluateClusterer(newData);
				System.out.println("------------------------------NUM cluster-----------------------  "+eval.getNumClusters());

				double[] assignments = eval.getClusterAssignments();
				
				for(int i = 0; i < assignments.length; i++) {
					String cluster = clusterName + "cluster_"+(int)assignments[i]+ File.separator;
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
					
					setDataIntoSubCluster(subClustering, key, cluster, data, i);
				}
			}		
		}
		
		//write cluster_dev information into csv file
		String outputPath = projectInformation.getOutputPath() + File.separator + "clusterFinderResult.csv";
		File temp = new File(outputPath);
		boolean isFile = temp.isFile();
		FileWriter out = new FileWriter(outputPath, true); 
		CSVPrinter printer;
		
		if(isFile) {
			printer = new CSVPrinter(out, CSVFormat.DEFAULT);
		}else {
			printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(PredictionResult.clusterFinderCSVHeader));
		}			
		
		try (printer) {
			cluster_developer.forEach((cluster,devList) -> {
				devList.forEach((dev) -> {
					try {
						int depth = (int)cluster.chars().filter(c -> c == '/').count();
						String clusterName = cluster.substring(0, cluster.lastIndexOf(File.separator));
						printer.printRecord(dev,depth,clusterName);
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			});
			
		}
		
		return outputPath;
	}
	
	private void setDataIntoSubCluster(TreeMap<Integer, ArrayList<Clustering>> subClustering, int key, String cluster,
			Instances data, int i) {
		
		for (Entry<Integer, ArrayList<Clustering>> entrySet : subClustering.entrySet()) {
			if(key > entrySet.getKey()) continue;
			ArrayList<Clustering> clus = entrySet.getValue();
			for(Clustering clu : clus) {
				String cluName = clu.getClusterName();
				if(cluster.equals(cluName)) {
					clu.setData(data.get(i));
				}
			}
		}
	}

	private Instances removeDeveloperIDColumn(Instances data) throws Exception {
		int[] toSelect = new int[data.numAttributes()-1];

		for (int i = 0, j = 1; i < data.numAttributes()-1; i++,j++) {
			toSelect[i] = j;
		}
		
		Remove removeFilter = new Remove();
		removeFilter.setAttributeIndicesArray(toSelect);
		removeFilter.setInvertSelection(true);
		removeFilter.setInputFormat(data);
		Instances newData = Filter.useFilter(data, removeFilter);
		return newData;
	}
	
	private String parsingDeveloperName(String stringValue) {
		String developerName = stringValue;
		if(stringValue.startsWith(" ")) {
			developerName = stringValue.substring(1, stringValue.length());
		}
		return developerName;
	}
}

class Clustering{
	String clusterName;
	String emPath;
	String clusteredResult;
	Instances data;
	
	Clustering(Instances data){
		this.clusterName = "";
		this.emPath = null;
		this.clusteredResult = null;
		this.data = new Instances(data,0);
	}

	protected String getEmPath() {
		return emPath;
	}

	protected void setEmPath(String emPath) {
		this.emPath = emPath;
	}

	protected Instances getData() {
		return data;
	}

	protected void setData(Instance data) {
		this.data.add(data);
	}

	protected String getClusterName() {
		return clusterName;
	}

	protected void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	protected String getClusteredResult() {
		return clusteredResult;
	}

	protected void setClusteredResult(String clusteredResult) {
		this.clusteredResult = clusteredResult;
	}
	
}
