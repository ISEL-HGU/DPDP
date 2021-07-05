package edu.handong.csee.isel;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectInformation {
	String defectInstancePath;
	String outputPath;
	String projectName;
	String referenceFolderPath;
	String developerProfilingInstanceCSVPath;
	boolean bow;
	boolean imb;
	
	ProjectInformation(){
		this.defectInstancePath = null;
		this.outputPath = null;
		this.projectName = null;
		this.referenceFolderPath = null;
		this.developerProfilingInstanceCSVPath = null;
		this.bow = false;
		this.imb = false;
		
	}

	public String getDefectInstancePath() {
		return defectInstancePath;
	}

	protected void setDefectInstancePath(String defectInstancePath) {
		this.defectInstancePath = defectInstancePath;
		

		//set Projectname and referenceFolderPath
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
		setDeveloperProfilingInstanceCSVPath(developerProfilingInstanceCSVPath);
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

	public String getDeveloperProfilingInstanceCSVPath() {
		return developerProfilingInstanceCSVPath;
	}

	public void setDeveloperProfilingInstanceCSVPath(String developerProfilingInstanceCSVPath) {
		this.developerProfilingInstanceCSVPath = developerProfilingInstanceCSVPath;
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
	
}
