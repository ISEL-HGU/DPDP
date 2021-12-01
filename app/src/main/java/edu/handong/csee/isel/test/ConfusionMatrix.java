package edu.handong.csee.isel.test;

import java.util.ArrayList;
import weka.classifiers.evaluation.Prediction;

public class ConfusionMatrix {
	
	String cluster; // use only evaluateDeveloper
	
	int numOfClean;
	int numOfBuggy;
	
	double TP;
	double FP;
	double TN;
	double FN;
	
	ArrayList<Prediction> predictionObjects;
	
	public ConfusionMatrix(){
		this.numOfClean = 0;
		this.numOfBuggy = 0;
		
		this.TP = 0;
		this.FP = 0;
		this.TN = 0;
		this.FN = 0;
		
		this.predictionObjects = new ArrayList<>();
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



	public ArrayList<Prediction> getPredictionObjects() {
		return predictionObjects;
	}



	public void setPredictionObjects(Prediction prediction) {
		this.predictionObjects.add(prediction);
	}
	
	public void setPredictionObjects(ArrayList<Prediction> prediction) {
		this.predictionObjects.addAll(prediction);
	}
}
