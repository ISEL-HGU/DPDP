package edu.handong.csee.isel.test;

import java.util.ArrayList;
import java.util.HashMap;

public class EvaluationInformation {
	
	String Developer;
	String Algorithm;
	String modelHash;
	int num_of_clean;
	int num_of_buggy;
	int clusterNumber;
	
	double PDP_precision;
	double PDP_recall;
	double PDP_fmeasure;
	double PDP_auc;
	double PDP_mcc;
	
	double DPDP_precision;
	double DPDP_recall;
	double DPDP_fmeasure;
	double DPDP_auc;
	double DPDP_mcc;
	
	String DPDP_Precision_WTL;
	String DPDP_Recall_WTL;
	String DPDP_FMeasure_WTL;
	String DPDP_auc_WTL;
	String DPDP_mcc_WTL;
	
	double PDP_TP;
	double PDP_FP;
	double PDP_TN;
	double PDP_FN;
	
	double DPDP_TP;
	double DPDP_FP;
	double DPDP_TN;
	double DPDP_FN;
	
	protected EvaluationInformation() {
		Developer = null;
		Algorithm = null;
		this.modelHash = null;
		this.num_of_clean = 0;
		this.num_of_buggy = 0;
		this.clusterNumber = 0;
		PDP_precision = 0;
		PDP_recall = 0;
		PDP_fmeasure = 0;
		PDP_auc = 0;
		PDP_mcc = 0;
		DPDP_precision = 0;
		DPDP_recall = 0;
		DPDP_fmeasure = 0;
		DPDP_auc = 0;
		DPDP_mcc = 0;
		DPDP_Precision_WTL = null;
		DPDP_Recall_WTL = null;
		DPDP_FMeasure_WTL = null;
		DPDP_auc_WTL = null;
		DPDP_mcc_WTL = null;
		this.PDP_TP = 0;
		this.PDP_FP = 0;
		this.PDP_TN = 0;
		this.PDP_FN = 0;
		this.DPDP_TP = 0;
		this.DPDP_FP = 0;
		this.DPDP_TN = 0;
		this.DPDP_FN = 0;
	}
	
	protected String getDeveloper() {
		return Developer;
	}
	protected void setDeveloper(String developer) {
		Developer = developer;
	}
	protected String getAlgorithm() {
		return Algorithm;
	}
	protected void setAlgorithm(String algorithm) {
		Algorithm = algorithm;
	}
	protected String getModelHash() {
		return modelHash;
	}
	protected void setModelHash(String modelHash) {
		this.modelHash = modelHash;
	}
	protected int getNum_of_clean() {
		return num_of_clean;
	}
	protected void setNum_of_clean(int num_of_clean) {
		this.num_of_clean = num_of_clean;
	}
	protected int getNum_of_buggy() {
		return num_of_buggy;
	}
	protected void setNum_of_buggy(int num_of_buggy) {
		this.num_of_buggy = num_of_buggy;
	}
	protected int getClusterNumber() {
		return clusterNumber;
	}
	protected void setClusterNumber(int clusterNumber) {
		this.clusterNumber = clusterNumber;
	}
	protected double getPDP_precision() {
		return PDP_precision;
	}
	protected void setPDP_precision(double pDP_precision) {
		PDP_precision = pDP_precision;
	}
	protected double getPDP_recall() {
		return PDP_recall;
	}
	protected void setPDP_recall(double pDP_recall) {
		PDP_recall = pDP_recall;
	}
	protected double getPDP_fmeasure() {
		return PDP_fmeasure;
	}
	protected void setPDP_fmeasure(double pDP_fmeasure) {
		PDP_fmeasure = pDP_fmeasure;
	}
	protected double getPDP_auc() {
		return PDP_auc;
	}
	protected void setPDP_auc(double pDP_auc) {
		PDP_auc = pDP_auc;
	}
	protected double getPDP_mcc() {
		return PDP_mcc;
	}
	protected void setPDP_mcc(double pDP_mcc) {
		PDP_mcc = pDP_mcc;
	}
	protected double getDPDP_precision() {
		return DPDP_precision;
	}
	protected void setDPDP_precision(double dPDP_precision) {
		DPDP_precision = dPDP_precision;
	}
	protected double getDPDP_recall() {
		return DPDP_recall;
	}
	protected void setDPDP_recall(double dPDP_recall) {
		DPDP_recall = dPDP_recall;
	}
	protected double getDPDP_fmeasure() {
		return DPDP_fmeasure;
	}
	protected void setDPDP_fmeasure(double dPDP_fmeasure) {
		DPDP_fmeasure = dPDP_fmeasure;
	}
	protected double getDPDP_auc() {
		return DPDP_auc;
	}
	protected void setDPDP_auc(double dPDP_auc) {
		DPDP_auc = dPDP_auc;
	}
	protected double getDPDP_mcc() {
		return DPDP_mcc;
	}
	protected void setDPDP_mcc(double dPDP_mcc) {
		DPDP_mcc = dPDP_mcc;
	}
	protected String getDPDP_Precision_WTL() {
		return DPDP_Precision_WTL;
	}
	protected void setDPDP_Precision_WTL(String dPDP_Precision_WTL) {
		DPDP_Precision_WTL = dPDP_Precision_WTL;
	}
	protected String getDPDP_Recall_WTL() {
		return DPDP_Recall_WTL;
	}
	protected void setDPDP_Recall_WTL(String dPDP_Recall_WTL) {
		DPDP_Recall_WTL = dPDP_Recall_WTL;
	}
	protected String getDPDP_FMeasure_WTL() {
		return DPDP_FMeasure_WTL;
	}
	protected void setDPDP_FMeasure_WTL(String dPDP_FMeasure_WTL) {
		DPDP_FMeasure_WTL = dPDP_FMeasure_WTL;
	}
	protected String getDPDP_auc_WTL() {
		return DPDP_auc_WTL;
	}
	protected void setDPDP_auc_WTL(String dPDP_auc_WTL) {
		DPDP_auc_WTL = dPDP_auc_WTL;
	}
	protected String getDPDP_mcc_WTL() {
		return DPDP_mcc_WTL;
	}
	protected void setDPDP_mcc_WTL(String dPDP_mcc_WTL) {
		DPDP_mcc_WTL = dPDP_mcc_WTL;
	}

	protected double getPDP_TP() {
		return PDP_TP;
	}

	protected void setPDP_TP(double pDP_TP) {
		PDP_TP = pDP_TP;
	}

	protected double getPDP_FP() {
		return PDP_FP;
	}

	protected void setPDP_FP(double pDP_FP) {
		PDP_FP = pDP_FP;
	}

	protected double getPDP_TN() {
		return PDP_TN;
	}

	protected void setPDP_TN(double pDP_TN) {
		PDP_TN = pDP_TN;
	}

	protected double getDPDP_TP() {
		return DPDP_TP;
	}

	protected void setDPDP_TP(double dPDP_TP) {
		DPDP_TP = dPDP_TP;
	}

	protected double getDPDP_FP() {
		return DPDP_FP;
	}

	protected void setDPDP_FP(double dPDP_FP) {
		DPDP_FP = dPDP_FP;
	}

	protected double getDPDP_TN() {
		return DPDP_TN;
	}

	protected void setDPDP_TN(double dPDP_TN) {
		DPDP_TN = dPDP_TN;
	}

	protected double getPDP_FN() {
		return PDP_FN;
	}

	protected void setPDP_FN(double pDP_FN) {
		PDP_FN = pDP_FN;
	}

	protected double getDPDP_FN() {
		return DPDP_FN;
	}

	protected void setDPDP_FN(double dPDP_FN) {
		DPDP_FN = dPDP_FN;
	}
	
}

class EvaluationGlobalValue{
	public static String[] CSVHeader = {
			"Developer",
			"Algorithm",
			"clusterNumber",
			"modelHash",
			"num of clean",
			"num of buggy",
			"PDP precision",
			"PDP recall",
			"PDP fmeasure",
			"PDP auc",
			"PDP mcc",
			"DPDP precision",
			"DPDP recall",
			"DPDP fmeasure",
			"DPDP auc",
			"DPDP mcc",
			"DPDP Precision W/T/L",
			"DPDP Recall W/T/L",
			"DPDP FMeasure W/T/L",
			"DPDP auc W/T/L",
			"DPDP mcc W/T/L"
			};
	
	int clusterNumber;
	String Algorithm;
	
	int num_of_clean;
	int num_of_buggy;
	
	double PDP_TP;
	double PDP_FP;
	double PDP_TN;
	double PDP_FN;
	
	double DPDP_TP;
	double DPDP_FP;
	double DPDP_TN;
	double DPDP_FN;
	
	HashMap<String, Integer> p_WTL;
	HashMap<String, Integer> r_WTL;
	HashMap<String, Integer> f_WTL;
	HashMap<String, Integer> a_WTL;
	HashMap<String, Integer> m_WTL;
	
	EvaluationGlobalValue(){
		clusterNumber = 0;
		Algorithm = null;
		
		num_of_clean = 0;
		num_of_clean = 0;
		
		PDP_TP = 0;
		PDP_FP = 0;
		PDP_TN = 0;
		PDP_FN = 0;
		
		DPDP_TP = 0;
		DPDP_FP = 0;
		DPDP_TN = 0;
		DPDP_FN = 0;
		
		p_WTL = new HashMap<>() {{
			put("win", 0);
			put("tie", 0);
			put("loss", 0);
		}};
		r_WTL = new HashMap<>() {{
			put("win", 0);
			put("tie", 0);
			put("loss", 0);
		}};
		f_WTL = new HashMap<>() {{
			put("win", 0);
			put("tie", 0);
			put("loss", 0);
		}};
		a_WTL = new HashMap<>() {{
			put("win", 0);
			put("tie", 0);
			put("loss", 0);
		}};
		m_WTL = new HashMap<>() {{
			put("win", 0);
			put("tie", 0);
			put("loss", 0);
		}};
	}
	
	protected double getPDP_TP() {
		return PDP_TP;
	}
	protected void setPDP_TP(double pDP_TP) {
		this.PDP_TP = this.PDP_TP + pDP_TP;
	}
	protected double getPDP_FP() {
		return PDP_FP;
	}
	protected void setPDP_FP(double pDP_FP) {
		this.PDP_FP = this.PDP_FP + pDP_FP;
	}
	protected double getPDP_TN() {
		return PDP_TN;
	}
	protected void setPDP_TN(double pDP_TN) {
		this.PDP_TN = this.PDP_TN + pDP_TN;
	}
	protected double getPDP_FN() {
		return PDP_FN;
	}
	protected void setPDP_FN(double pDP_FN) {
		this.PDP_FN = this.PDP_FN + pDP_FN;
	}
	protected double getDPDP_TP() {
		return DPDP_TP;
	}
	protected void setDPDP_TP(double dPDP_TP) {
		this.DPDP_TP = this.DPDP_TP + dPDP_TP;
	}
	protected double getDPDP_FP() {
		return DPDP_FP;
	}
	protected void setDPDP_FP(double dPDP_FP) {
		this.DPDP_FP = this.DPDP_FP + dPDP_FP;
	}
	protected double getDPDP_TN() {
		return DPDP_TN;
	}
	protected void setDPDP_TN(double dPDP_TN) {
		this.DPDP_TN = this.DPDP_TN + dPDP_TN;
	}
	protected double getDPDP_FN() {
		return DPDP_FN;
	}
	protected void setDPDP_FN(double dPDP_FN) {
		this.DPDP_FN = this.DPDP_FN + dPDP_FN;
	}
	protected String getAlgorithm() {
		return Algorithm;
	}

	protected void setAlgorithm(String algorithm) {
		Algorithm = algorithm;
	}

	protected int getNum_of_clean() {
		return num_of_clean;
	}

	protected void setNum_of_clean(int num_of_clean) {
		this.num_of_clean = this.num_of_clean + num_of_clean;
	}

	protected int getNum_of_buggy() {
		return num_of_buggy;
	}

	protected void setNum_of_buggy(int num_of_buggy) {
		this.num_of_buggy = this.num_of_buggy + num_of_buggy;
	}

	protected int getClusterNumber() {
		return clusterNumber;
	}

	protected void setClusterNumber(int clusterNumber) {
		this.clusterNumber = clusterNumber;
	}

	protected int getP_WTL(String key) {
		return p_WTL.get(key);
	}

	protected void setP_WTL(String key, int value) {
		this.p_WTL.put(key, p_WTL.get(key)+value);
	}

	protected int getR_WTL(String key) {
		return r_WTL.get(key);
	}

	protected void setR_WTL(String key, int value) {
		this.r_WTL.put(key, r_WTL.get(key)+value);
	}

	protected int getF_WTL(String key) {
		return f_WTL.get(key);
	}

	protected void setF_WTL(String key, int value) {
		this.f_WTL.put(key, f_WTL.get(key)+value);
	}

	protected int getA_WTL(String key) {
		return a_WTL.get(key);
	}

	protected void setA_WTL(String key, int value) {
		this.a_WTL.put(key, a_WTL.get(key)+value);
	}

	protected int getM_WTL(String key) {
		return m_WTL.get(key);
	}

	protected void setM_WTL(String key, int value) {
		this.m_WTL.put(key, m_WTL.get(key)+value);
	}
	
}