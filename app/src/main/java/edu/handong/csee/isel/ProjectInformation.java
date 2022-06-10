package edu.handong.csee.isel;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectInformation {
	String inputPath;
	String outputPath;
	String projectName;
	String repositoryPath;
	
	
	String referenceFolderPath;
	String developerDataCSVPath;
	String testDeveloperProfilingInstanceCSV;
	String testDeveloperDefectInstanceArff;
	String locationOfClusterModels;
	String locationOfDefectModels;
	String testFolderPath;
	String modelInformationCSV;
	String evaluation_algorithm;
	String numberOfThreadPool;
	int numOfCluster;
	int hierarchy;
	int atLeastOfCommit;
	boolean bow;
	boolean imb;
	boolean testSubOption_once;
	
	ProjectInformation(){
		this.inputPath = null;
		this.outputPath = null;
		this.projectName = null;
		this.repositoryPath = null;
		
		
		this.referenceFolderPath = null;
		this.developerDataCSVPath = null;
		this.testDeveloperProfilingInstanceCSV = null;
		this.testDeveloperDefectInstanceArff = null;
		this.locationOfClusterModels = null;
		this.locationOfDefectModels = null;
		this.bow = false;
		this.imb = false;
		this.atLeastOfCommit = 0;
		this.numOfCluster = 0;
		this.testSubOption_once = false;
		this.testFolderPath = null;
		this.hierarchy = 0;
		this.modelInformationCSV = null;
		this.evaluation_algorithm = null;
		this.numberOfThreadPool = null;
	}

	public String getInputPath() {
		return inputPath;
	}
	
	protected void setAInputPath(String inputPath) {
		this.inputPath = inputPath;
	}

	protected void setInputPath(String defectInstancePath) {
		this.inputPath = defectInstancePath;
		

		//set Project name and referenceFolderPath
		String developerProfilingInstanceCSVPath = null;
		String referenceFolderPath = null;
		String projectName = null;
		
		Pattern pattern = Pattern.compile("(.+)/(.+)-data.arff");
		Matcher matcher = pattern.matcher(defectInstancePath);
		while(matcher.find()) {
			referenceFolderPath = matcher.group(1);
			projectName = matcher.group(2);
		}
		if(projectName == null) {
			Pattern pattern2 = Pattern.compile("(.+)/(.+)-data");

			Matcher matcher2 = pattern2.matcher(defectInstancePath);
			while(matcher2.find()) {
				projectName = matcher2.group(2);
			}
		}
		referenceFolderPath = referenceFolderPath+File.separator+projectName+"-reference";
		developerProfilingInstanceCSVPath = referenceFolderPath+File.separator+projectName+"_Label.csv";
		
		setProjectName(projectName);
		setReferenceFolderPath(referenceFolderPath);
		setDeveloperDataCSVPath(developerProfilingInstanceCSVPath);
	}

	public String getOutputPath() {
		return outputPath;
	}

	protected void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	public String getRepositoryPath() {
		return repositoryPath;
	}

	protected void setRepositoryPath(String repositoryPath) {
		this.repositoryPath = repositoryPath;
	}
	
	public String getProjectName() {
		return projectName;
	}

	protected void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getReferenceFolderPath() {
		return referenceFolderPath;
	}

	protected void setReferenceFolderPath(String referenceFolderPath) {
		this.referenceFolderPath = referenceFolderPath;
	}

	public String getDeveloperDataCSVPath() {
		return developerDataCSVPath;
	}

	public void setDeveloperDataCSVPath(String developerProfilingInstanceCSVPath) {
		this.developerDataCSVPath = developerProfilingInstanceCSVPath;
	}

	public String getTestDeveloperProfilingInstanceCSV() {
		return testDeveloperProfilingInstanceCSV;
	}

	public void setTestDeveloperProfilingInstanceCSV(String testDeveloperProfilingInstanceCSV) {
		this.testDeveloperProfilingInstanceCSV = testDeveloperProfilingInstanceCSV;
	}

	public String getTestDeveloperDefectInstanceArff() {
		return testDeveloperDefectInstanceArff;
	}

	public void setTestDeveloperDefectInstanceArff(String testDeveloperDefectInstanceArff) {
		this.testDeveloperDefectInstanceArff = testDeveloperDefectInstanceArff;
	}

	public boolean isBow() {
		return bow;
	}

	public void setBow(boolean bow) {
		this.bow = bow;
	}

	public boolean isImb() {
		return imb;
	}

	public void setImb(boolean imb) {
		this.imb = imb;
	}

	public String getLocationOfClusterModels() {
		return locationOfClusterModels;
	}

	public void setLocationOfClusterModels(String locationOfClusterModels) {
		this.locationOfClusterModels = locationOfClusterModels;
	}

	public String getLocationOfDefectModels() {
		return locationOfDefectModels;
	}

	public void setLocationOfDefectModels(String locationOfDefectModels) {
		this.locationOfDefectModels = locationOfDefectModels;
	}

	public int getNumOfCluster() {
		return numOfCluster;
	}

	public void setNumOfCluster(int numOfCluster) {
		this.numOfCluster = numOfCluster;
	}

	public boolean isTestSubOption_once() {
		return testSubOption_once;
	}

	public void setTestSubOption_once(boolean testSubOption_once) {
		this.testSubOption_once = testSubOption_once;
	}

	public String getTestFolderPath() {
		return testFolderPath;
	}

	public void setTestFolderPath(String testFolderPath) {
		this.testFolderPath = testFolderPath;
	}

	public int getHierarchy() {
		return hierarchy;
	}

	public void setHierarchy(int hierarchy) {
		this.hierarchy = hierarchy;
	}

	public int getAtLeastOfCommit() {
		return atLeastOfCommit;
	}

	public void setAtLeastOfCommit(int atLeastOfCommit) {
		this.atLeastOfCommit = atLeastOfCommit;
	}

	public String getModelInformationCSV() {
		return modelInformationCSV;
	}

	public void setModelInformationCSV(String modelInformationCSV) {
		this.modelInformationCSV = modelInformationCSV;
	}

	public String getEvaluation_algorithm() {
		return evaluation_algorithm;
	}

	public void setEvaluation_algorithm(String evaluation_algorithm) {
		this.evaluation_algorithm = evaluation_algorithm;
	}

	public String getNumberOfThreadPool() {
		return numberOfThreadPool;
	}

	public void setNumberOfThreadPool(String numberOfThreadPool) {
		this.numberOfThreadPool = numberOfThreadPool;
	}
	
	
}
