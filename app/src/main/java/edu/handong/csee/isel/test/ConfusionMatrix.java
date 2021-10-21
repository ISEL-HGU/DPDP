package edu.handong.csee.isel.test;

public class ConfusionMatrix {
	public static String[] evaluationHeader = {"hierachy","Cluster","ID","NOC","NOB","Precision","Recall","Fmeasure","MCC","TP","FP","TN","FN"};
	
	String cluster; // use only evaluateDeveloper
	
	int numOfClean;
	int numOfBuggy;
	
	double TP;
	double FP;
	double TN;
	double FN;
	
	ConfusionMatrix(){
		this.numOfClean = 0;
		this.numOfBuggy = 0;
		
		this.TP = 0;
		this.FP = 0;
		this.TN = 0;
		this.FN = 0;
	}

	
	
	protected int getNumOfClean() {
		return numOfClean;
	}



	protected void setNumOfClean(int numOfClean) {
		this.numOfClean = this.numOfClean + numOfClean;
	}



	protected int getNumOfBuggy() {
		return numOfBuggy;
	}



	protected void setNumOfBuggy(int numOfBuggy) {
		this.numOfBuggy = this.numOfBuggy + numOfBuggy;
	}



	protected double getTP() {
		return TP;
	}



	protected void setTP(double tP) {
		TP = TP + tP;
	}



	protected double getFP() {
		return FP;
	}



	protected void setFP(double fP) {
		FP = FP + fP;
	}



	protected double getTN() {
		return TN;
	}



	protected void setTN(double tN) {
		TN = TN + tN;
	}



	protected double getFN() {
		return FN;
	}



	protected void setFN(double fN) {
		FN = FN + fN;
	}



	protected String getCluster() {
		return cluster;
	}

	protected void setCluster(String cluster) {
		this.cluster = cluster;
	}
}
