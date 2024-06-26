package edu.handong.csee.isel.test;

import weka.classifiers.evaluation.Prediction;

public class PredictionResult {
	
	String authorId;
	String clusterType;
	int hierachy;
	String prediction;
	String actual;
	String model;
	String algorithm;
	boolean match;
	Prediction predictionObject;
	
	PredictionResult(){
		this.authorId = null;
		this.clusterType = null;
		this.hierachy = 0;
		this.prediction = null;
		this.actual = null;
		this.model = null;
		this.match = false;
		this.algorithm = null;
		this.predictionObject = null;
	}

	protected String getAuthorId() {
		return authorId;
	}

	protected void setAuthorId(String authorId) {
		this.authorId = authorId;
	}

	protected String getClusterType() {
		return clusterType;
	}

	protected void setClusterType(String clusterType) {
		this.clusterType = clusterType;
	}

	protected int getHierachy() {
		return hierachy;
	}

	protected void setHierachy(int hierachy) {
		this.hierachy = hierachy;
	}

	protected String getPredict() {
		return prediction;
	}

	protected void setPredict(String predict) {
		this.prediction = predict;
	}

	protected String getLabel() {
		return actual;
	}

	protected void setLabel(String label) {
		this.actual = label;
	}

	protected String getModel() {
		return model;
	}

	protected void setModel(String model) {
		this.model = model;
	}

	protected boolean isMatch() {
		return match;
	}

	protected void setMatch(boolean match) {
		this.match = match;
	}

	protected String getAlgorithm() {
		return algorithm;
	}

	protected void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	protected Prediction getPredictionObject() {
		return predictionObject;
	}

	protected void setPredictionObject(Prediction eva) {
		this.predictionObject = eva;
	}
	
}
