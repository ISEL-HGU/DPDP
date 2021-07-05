package edu.handong.csee.isel.model;

public class ClusterModelInfo {
    public static String[] CSVHeader = {"HashKey","ClusterName","Algorithm","total","Buggy", "Clean", "EvaluationName", "classImbalanceAlgo", "totalImb", "BuggyImb","CleanImb","PTproperty","PTmin","PTmax","PTstep"};

	double numOfBuggy;
	double numOfClean;
	double numOfBuggy_solveImb;
	double numOfClean_solveImb;
	String parameterTuning;
	String classImbalanceAlgo;
	
	public ClusterModelInfo() {
		this.numOfBuggy = 0;
		this.numOfClean = 0;
		this.numOfBuggy_solveImb = 0;
		this.numOfClean_solveImb = 0;
		this.parameterTuning = null;
		this.classImbalanceAlgo = null;
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

	protected double getNumOfBuggy_solveImb() {
		return numOfBuggy_solveImb;
	}

	protected void setNumOfBuggy_solveImb(double numOfBuggy_solveImb) {
		this.numOfBuggy_solveImb = numOfBuggy_solveImb;
	}

	protected double getNumOfClean_solveImb() {
		return numOfClean_solveImb;
	}

	protected void setNumOfClean_solveImb(double numOfClean_solveImb) {
		this.numOfClean_solveImb = numOfClean_solveImb;
	}

	protected String getParameterTuning() {
		return parameterTuning;
	}

	protected void setParameterTuning(String parameterTuning) {
		this.parameterTuning = parameterTuning;
	}

	protected String getClassImbalanceAlgo() {
		return classImbalanceAlgo;
	}

	protected void setClassImbalanceAlgo(String classImbalanceAlgo) {
		this.classImbalanceAlgo = classImbalanceAlgo;
	}
	
	
	
}
