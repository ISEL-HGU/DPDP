package edu.handong.csee.isel;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectInformation {
	String defectInstancePath;
	String outputPath;
	String projectName;
	String referenceFolderPath;
	String developerDataCSVPath;
	String testDeveloperProfilingInstanceCSV;
	String testDeveloperDefectInstanceArff;
	String locationOfModels;
	boolean bow;
	boolean imb;
	
	ProjectInformation(){
		this.defectInstancePath = null;
		this.outputPath = null;
		this.projectName = null;
		this.referenceFolderPath = null;
		this.developerDataCSVPath = null;
		this.testDeveloperProfilingInstanceCSV = null;
		this.testDeveloperDefectInstanceArff = null;
		this.locationOfModels = null;
		this.bow = false;
		this.imb = false;
		
	}

	public String getDefectInstancePath() {
		return defectInstancePath;
	}

	protected void setDefectInstancePath(String defectInstancePath) {
		this.defectInstancePath = defectInstancePath;
		

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

	public String getLocationOfModels() {
		return locationOfModels;
	}

	public void setLocationOfModels(String locationOfModels) {
		this.locationOfModels = locationOfModels;
	}
	
}
