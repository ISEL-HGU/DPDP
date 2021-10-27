package edu.handong.csee.isel.test;

public class ConfusionMatrix {
	
	String cluster; // use only evaluateDeveloper
	
	int numOfClean;
	int numOfBuggy;
	
	double TP;
	double FP;
	double TN;
	double FN;
	
	public ConfusionMatrix(){
		this.numOfClean = 0;
		this.numOfBuggy = 0;
		
		this.TP = 0;
		this.FP = 0;
		this.TN = 0;
		this.FN = 0;
	}

	
	
	public int getNumOfClean() {
		return numOfClean;
	}



	public void setNumOfClean(int numOfClean) {
		this.numOfClean = this.numOfClean + numOfClean;
	}



	public int getNumOfBuggy() {
		return numOfBuggy;
	}



	public void setNumOfBuggy(int numOfBuggy) {
		this.numOfBuggy = this.numOfBuggy + numOfBuggy;
	}



	public double getTP() {
		return TP;
	}



	public void setTP(double tP) {
		TP = TP + tP;
	}



	public double getFP() {
		return FP;
	}



	public void setFP(double fP) {
		FP = FP + fP;
	}



	public double getTN() {
		return TN;
	}



	public void setTN(double tN) {
		TN = TN + tN;
	}



	public double getFN() {
		return FN;
	}



	public void setFN(double fN) {
		FN = FN + fN;
	}

	public String getCluster() {
		return cluster;
	}

	public void setCluster(String cluster) {
		this.cluster = cluster;
	}
}
