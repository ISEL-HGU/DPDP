package edu.handong.csee.isel.metrictest;

public class DeveloperScatteringMetric {
	String authorId;
	float structuralScattering;
	float semanticScattering;
	
	DeveloperScatteringMetric(){
		this.authorId = null;
		this.structuralScattering = 0;
		this.semanticScattering = 0;
	}

	protected String getAuthorId() {
		return authorId;
	}

	protected void setAuthorId(String authorId) {
		this.authorId = authorId;
	}

	protected float getStructuralScattering() {
		return structuralScattering;
	}

	protected void setStructuralScattering(float structuralScattering) {
		this.structuralScattering = structuralScattering;
	}

	protected float getSemanticScattering() {
		return semanticScattering;
	}

	protected void setSemanticScattering(float semanticScattering) {
		this.semanticScattering = semanticScattering;
	}
	
}
