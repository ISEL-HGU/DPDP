package edu.handong.csee.isel.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import edu.handong.csee.isel.ProjectInformation;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class Testing {
	ProjectInformation projectInformation;
	
	public Testing(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}

	public HashMap<Integer,ArrayList<String>> findDeveloperCluster() throws Exception {
		HashMap<Integer,ArrayList<String>> cluster_developer = new HashMap<>();
		String locationOfProfilingModel = projectInformation.getLocationOfModels()+File.separator+"EM.model";
		
		CSVLoader loader = new CSVLoader();
		loader.setSource(new File(projectInformation.getTestDeveloperProfilingInstanceCSV()));

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
		
		EM em = (EM)SerializationHelper.read(new FileInputStream(locationOfProfilingModel));
		
		ClusterEvaluation eval = new ClusterEvaluation();
		eval.setClusterer(em);
		
//		System.out.println("------------------------------NUM cluster-----------------------  "+eval.getNumClusters());

		return null;
	}
}
