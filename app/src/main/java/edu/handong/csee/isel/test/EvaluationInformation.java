package edu.handong.csee.isel.test;

public class EvaluationInformation {
	public static String[] CSVHeader = {
			"Developer",
			"Algorithm",
			"Mode",
			"Recall",
			"Precision",
			"FMeasure",
			"MCC",
			"AUC",
			"total",
			"buggy",
			"clean",
			"modelHash",
			"modelClusterNumber"};
	
	String developerName;
	String modelAlgirhtm;
	String modelHash;
	
	int modelNumber;
	double numOfBuggy;
	double numOfClean;
	
	protected EvaluationInformation() {
		this.developerName = null;
		this.modelHash = null;
		this.modelAlgirhtm = null;
		this.modelNumber = 1000;
		this.numOfBuggy = 0;
		this.numOfClean = 0;
	}

	protected String getDeveloperName() {
		return developerName;
	}

	protected void setDeveloperName(String developerName) {
		this.developerName = developerName;
	}

	protected String getModelHash() {
		return modelHash;
	}

	protected void setModelHash(String modelHash) {
		this.modelHash = modelHash;
	}

	protected String getModelAlgirhtm() {
		return modelAlgirhtm;
	}

	protected void setModelAlgirhtm(String modelAlgirhtm) {
		this.modelAlgirhtm = modelAlgirhtm;
	}

	protected int getModelNumber() {
		return modelNumber;
	}

	protected void setModelNumber(int modelNumber) {
		this.modelNumber = modelNumber;
	}

	protected double getNumOfBuggy() {
		return numOfBuggy;
	}

	protected void setNumOfBuggy(double numOfBuggy) {
		this.numOfBuggy = numOfBuggy;
	}

	protected double getNumOfClean() {
		return numOfClean;
	}

	protected void setNumOfClean(double numOfClean) {
		this.numOfClean = numOfClean;
	}

}

class EvaluationResult{
	double recall;
	double precision;
	double fMeasure;
	double MCC;
	double AUC;
	
	protected EvaluationResult() {
		this.recall = 0;
		this.precision = 0;
		this.fMeasure = 0;
		this.MCC = 0;
		this.AUC = 0;
	}

	protected double getRecall() {
		return recall;
	}

	protected void setRecall(double recall) {
		this.recall = recall;
	}

	protected double getPrecision() {
		return precision;
	}

	protected void setPrecision(double precision) {
		this.precision = precision;
	}

	protected double getfMeasure() {
		return fMeasure;
	}

	protected void setfMeasure(double fMeasure) {
		this.fMeasure = fMeasure;
	}

	protected double getMCC() {
		return MCC;
	}

	protected void setMCC(double mCC) {
		MCC = mCC;
	}

	protected double getAUC() {
		return AUC;
	}

	protected void setAUC(double aUC) {
		AUC = aUC;
	}
	
	
}