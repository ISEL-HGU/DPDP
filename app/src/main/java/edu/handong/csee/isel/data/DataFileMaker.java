package edu.handong.csee.isel.data;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import edu.handong.csee.isel.DPDPMain;
import edu.handong.csee.isel.ProjectInformation;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class DataFileMaker {
	ProjectInformation projectInformation;
	
	public DataFileMaker(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}

	public void makeDeveloperProfilingInstanceCSV(String mode) throws Exception {
		//read CSV
		String[] developerProfilingMetrics = new String[6];
		
		//make totalDevInstances directory
		File dir = null;
		String totalDeveloperInstanceCSV = null;
		if(mode.equals("train")) {
			dir = new File(projectInformation.getOutputPath());
			if(!dir.isDirectory()) {
				dir.mkdir();
			}
			//total developer Instance CSV path
			totalDeveloperInstanceCSV = dir.getAbsolutePath() + File.separator+"Developer_Profiling.csv";
		}else if(mode.equals("test")) {
			totalDeveloperInstanceCSV = projectInformation.getTestFolderPath()+File.separator+"ProilingInstances.csv";
			projectInformation.setTestDeveloperProfilingInstanceCSV(totalDeveloperInstanceCSV);
		}
		
		developerProfilingMetrics[0] = "-m";
		developerProfilingMetrics[1] = projectInformation.getDeveloperDataCSVPath();
		developerProfilingMetrics[2] = "-o";
		developerProfilingMetrics[3] = totalDeveloperInstanceCSV;
		developerProfilingMetrics[4] = "-p";
		developerProfilingMetrics[5] = projectInformation.getProjectName();

		DeveloperProfilingMetric developerProfilingMetric = new DeveloperProfilingMetric();
		developerProfilingMetric.run(developerProfilingMetrics);
	}
	
	public HashMap<String, String> makeDeveloperDefectInstanceArff(String mode) throws Exception {
		HashMap<String,String> developerDefectInstancePath = new HashMap<>();
		File dir = null;
		
		if(mode.equals("train")) {
			//make totalDevInstances directory
			dir = new File(projectInformation.getOutputPath() +File.separator+"totalDevDefectInstances");
			if(!dir.isDirectory()) {
				dir.mkdir();
			}
		}else if(mode.equals("test")) {
			dir = new File(projectInformation.getTestFolderPath()+File.separator+"DefectInstances");
			dir.mkdir();
			projectInformation.setTestDeveloperDefectInstanceArff(dir.getAbsolutePath());
		}
		
		//total developer Instance path
		String totalDevDefectInstancesForder = dir.getAbsolutePath();
		System.out.println(totalDevDefectInstancesForder);
		
		String defectDataArffPath;
		if(projectInformation.isBow()) {
			ExtractData.main(extractDataPargs(projectInformation.getDefectInstancePath(),projectInformation.getReferenceFolderPath(),projectInformation.isBow()));
			defectDataArffPath = ExtractData.getResultPath();
		}else {
			defectDataArffPath = projectInformation.getDefectInstancePath();
		}
		System.out.println(defectDataArffPath);
		
		try {
			DataSource source = new DataSource(defectDataArffPath);
			Instances data = source.getDataSet();
			
			//delete key column
			int[] toSelect = new int[data.numAttributes()-1];

			for (int i = 0, j = 0; i < data.numAttributes()-1; i++,j++) {
				toSelect[i] = j;
			}
			
			Remove removeFilter = new Remove();
			removeFilter.setAttributeIndicesArray(toSelect);
			removeFilter.setInvertSelection(true);
			removeFilter.setInputFormat(data);
			
			Instances newData = Filter.useFilter(data, removeFilter);

			//re arrange Attribute (make new BOW file)
			File newAttributeArff = new File(defectDataArffPath);
			ArrayList<String> arffAttribute = DefectAttribute.attribute;
			StringBuffer newAttributeContentBuf = new StringBuffer();
			
			arffAttribute.add(newData.attribute("meta_data-AuthorID").toString());
			
			for (String line : arffAttribute) {
				newAttributeContentBuf.append(line + "\n");
			}
			newAttributeContentBuf.append("\n@data\n");
			
			FileUtils.write(newAttributeArff, newAttributeContentBuf.toString(), "UTF-8");
			newAttributeContentBuf.delete(0, newAttributeContentBuf.length());

			Pattern instancesPattern = Pattern.compile("([0-9]+)\\s([^,^}]+)");
			ArrayList<String> attributeName_index = DefectAttribute.attribute_index;
			HashMap<Integer,Integer> oriAttrIdx_mergedAttrIdx = matchingOriginalAtrrIdxWithMergedAtrrIdx(attributeName_index, newData);
			ArrayList<String> instances = new ArrayList<>();
			
			for(int i = 0; i < newData.numInstances(); i++) {
				TreeMap<Integer,String> attributeIndex_value = new TreeMap<>();
				Matcher matcher = instancesPattern.matcher(newData.get(i).toString());
				while(matcher.find()) {
					int index = Integer.parseInt(matcher.group(1));
					String value = matcher.group(2);
					index = oriAttrIdx_mergedAttrIdx.get(index);
					attributeIndex_value.put(index, value);
				}
				//make new instances
				instances.add(makeChangedIndexInstance_BOW(attributeIndex_value));
			}
			
			int minimumCommit = 0;
			if(projectInformation.isLessThan10() == true) {
				minimumCommit = 10;
			}
			
			System.out.println(minimumCommit);
			
			for(String instance : instances) {
				newAttributeContentBuf.append(instance + "\n");
			}
			FileUtils.write(newAttributeArff, newAttributeContentBuf.toString(), "UTF-8", true);
			
			//split arff file according to each developer
			source = new DataSource(defectDataArffPath);
			newData = source.getDataSet();
			
			Instances filteredInstances = new Instances(newData, 0);
			
			ArrayList<String> developerDatas = new ArrayList<String>();
			String[] developerAttribute = filteredInstances.toString().split("\n");
			Attribute authorID = newData.attribute("meta_data-AuthorID");
			int index = authorID.index();

			for(int i = 0; i < authorID.numValues(); i++) {
				String developerID = parsingDeveloperName(authorID.value(i));
				String nominalToFilter = authorID.value(i);
				if(mode.equals("train")) {
					developerAttribute[index+2] = newAuthorIdAttribute(nominalToFilter,projectInformation.getProjectName());
				}else if (mode.equals("test")) {
					developerAttribute[index+2] = "";
				}
				
				for(Instance instance : newData) {
					if(instance.stringValue(index).equals(nominalToFilter)) {
						String developerData = newDeveloperData(instance.toString(),index);
						developerDatas.add(developerData);
					}
				}
				
				if(developerDatas.size() < minimumCommit) {
					DPDPMain.excludedDeveloper.add(developerID);
					DPDPMain.excludedDeveloper.add(nominalToFilter);
					continue;
				}

				File newArff = new File(totalDevDefectInstancesForder+File.separator+projectInformation.getProjectName()+"-"+developerID+".arff");
				StringBuffer newContentBuf = new StringBuffer();
				
				for(String s : developerAttribute) {
					newContentBuf.append(s + "\n");
				}
				
				for(String datas : developerDatas) {
					newContentBuf.append(datas + "\n");
				}
				
				FileUtils.write(newArff, newContentBuf.toString(), "UTF-8");
				developerDefectInstancePath.put(projectInformation.getProjectName()+"-"+developerID, newArff.getAbsolutePath());
				developerDatas.clear();
			}
		}
		catch(Exception e) {
			System.out.println("The data file is wrong");
			System.exit(0);
		}
		
		if(mode.equals("test")) {
			return developerDefectInstancePath;
		}
		return null;
	}

	private String newDeveloperData(String line, int index) {
		if((line.contains(","+index+" "))) { //index previous,index commitTime, index key} 
			String front = line.substring(0,line.lastIndexOf(","+index));
			line = front + "}";
		}
		return line;
	}

	private String newAuthorIdAttribute(String nominalToFilter, String projectName) {
		String authorAttribute = "@attribute meta_data-AuthorID {"+projectName+"-"+nominalToFilter+"}";
		return authorAttribute;
	}

	private String parsingDeveloperName(String stringValue) {
		String developerName = stringValue;
		if(stringValue.startsWith(" ")) {
			developerName = stringValue.substring(1, stringValue.length());
		}
		return developerName;
	}
	
	public static void deleteFile(String path) {
		File deleteFolder = new File(path);

		if(deleteFolder.exists()){
			File[] deleteFolderList = deleteFolder.listFiles();
			
			for (int i = 0; i < deleteFolderList.length; i++) {
				if(deleteFolderList[i].isFile()) {
					deleteFolderList[i].delete();
				}else {
					deleteFile(deleteFolderList[i].getPath());
				}
				deleteFolderList[i].delete(); 
			}
			deleteFolder.delete();
		}
	}
	
	private String[] extractDataPargs(String arffPath, String directoryPath, boolean noBOW) {

		String[] extratPDPargs = new String[3];
		extratPDPargs[0] = arffPath;
		extratPDPargs[1] = directoryPath;
		if(noBOW == false) {
			extratPDPargs[2] = "p";
		}else {
			extratPDPargs[2] = "bow";
		}

		return extratPDPargs;
	}

	public ArrayList<String> makeClusterArff() throws Exception {
		ArrayList<String> clusterArffPaths = new ArrayList<>();
		
		Pattern instancesPattern = Pattern.compile("([0-9]+)\\s([^,^}]+)");
		
		//cluster csv folder
		File clusterCSVfolder = new File(projectInformation.getDefectInstancePath()+File.separator+"ClusterCSV");
		
		//developer arff folder
		String developerArffFolder = projectInformation.getDefectInstancePath()+File.separator+"totalDevDefectInstances";
		
		File clusterModelFolder = new File(projectInformation.getDefectInstancePath() +File.separator+"ClusterArff");
		String clusterModelFolderPath = clusterModelFolder.getAbsolutePath();
		if(clusterModelFolder.isDirectory()) {
			deleteFile(clusterModelFolderPath);
		}
		clusterModelFolder.mkdir();
		
		//read cluster.csv file and save cluster_developerArff
		HashMap<String,ArrayList<String>> cluster_developerArff = new HashMap<>();
		File []fileList = clusterCSVfolder.listFiles();
//System.out.println(clusterCSVfolder);
		for(File file : fileList) {
			String clusterName = file.getName();
			clusterName = clusterName.substring(clusterName.lastIndexOf("/")+1,clusterName.lastIndexOf("."));
//System.out.println(clusterName);
			ArrayList<String> developerArff = new ArrayList<>();
			Reader in = new FileReader(file);
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);

			for (CSVRecord record : records) {
				developerArff.add(developerArffFolder+File.separator+record.get(0)+".arff");
			}
			cluster_developerArff.put(clusterName, developerArff);
		}
		
		//merge each cluster developers arff
		
		//merge 1) make total @attribute	
		for(String clusterName : cluster_developerArff.keySet()) {
			ArrayList<String> developerArffList = cluster_developerArff.get(clusterName);
			ArrayList<String> attributeName_index = DefectAttribute.attribute_index; //for change the attribute index of developers arff
			ArrayList<String> arffAttribute = DefectAttribute.attribute;//for print cluster arff file
System.out.println(clusterName);
			
			//make cluster arff file
			File newArff = new File(clusterModelFolderPath +File.separator+ clusterName +".arff");
			clusterArffPaths.add(newArff.getAbsolutePath());
			StringBuffer newContentBuf = new StringBuffer();

			for (String line : arffAttribute) {
				newContentBuf.append(line + "\n");
			}
			newContentBuf.append("\n@data\n");
			
			FileUtils.write(newArff, newContentBuf.toString(), "UTF-8", true);
			newContentBuf.delete(0, newContentBuf.length());
			
			//2) make instances of cluster.arff
			int numOfDeveloper = 0;
			ArrayList<String> instances = new ArrayList<>();
			
			for(String developerArff : developerArffList) {
				DataSource source = new DataSource(developerArff);
				Instances data = source.getDataSet();
				
				//make oriAttrIdx_mergedAttrIdx
				HashMap<Integer,Integer> oriAttrIdx_mergedAttrIdx = matchingOriginalAtrrIdxWithMergedAtrrIdx(attributeName_index, data);
//				for(int ori : oriAttrIdx_mergedAttrIdx.keySet()) {
//					System.out.println("ori : "+ori + "	mer : "+oriAttrIdx_mergedAttrIdx.get(ori));
//				}
				
				//Instances
				for(int i = 0; i < data.numInstances(); i++) {
					TreeMap<Integer,String> attributeIndex_value = new TreeMap<>();
					Matcher matcher = instancesPattern.matcher(data.get(i).toString());
					while(matcher.find()) {
						int index = Integer.parseInt(matcher.group(1));
						String value = matcher.group(2);
						index = oriAttrIdx_mergedAttrIdx.get(index);
						attributeIndex_value.put(index, value);
					}
					//make new instances
					instances.add(makeChangedIndexInstance(attributeIndex_value));
				}
				
				//print instances each 10 developers
				if((numOfDeveloper%10 == 0)|| (numOfDeveloper == developerArffList.size()-1)) {
					for(String instance : instances) {
						newContentBuf.append(instance + "\n");
					}
					FileUtils.write(newArff, newContentBuf.toString(), "UTF-8", true);
					newContentBuf.delete(0, newContentBuf.length());
					instances.clear();
				}
				numOfDeveloper++;
			}
		}
		System.out.println("Done Make Arff File\n");
		
//		System.out.println("Start make csv file");
//		for (String clusterArffPath : clusterArffPaths) {
//			String csvPath = clusterArffPath.substring(0,clusterArffPath.lastIndexOf("."));
//			DataSource source = new DataSource(clusterArffPath);
//			Instances data = source.getDataSet();
//			CSVSaver saver = new CSVSaver();
//			saver.setInstances(data);
//			saver.setFile(new File(csvPath+".csv"));
//			saver.writeBatch();
//		}
		
		return clusterArffPaths;
	}
	
	private HashMap<Integer, Integer> matchingOriginalAtrrIdxWithMergedAtrrIdx(ArrayList<String> attributeName_index, Instances data) {
		HashMap<Integer, Integer> oriAttrIdx_mergedAttrIdx = new HashMap<>();
		for(int j = 0; j < data.numAttributes(); j++) {
			Attribute attribute = data.attribute(j);
			int index = attributeName_index.indexOf(attribute.name());
			if(index == -1) {
				System.out.println("!!!!!!!!!!!!!There is new Attribute~!!!!!!!!!!!!!");
				System.out.println("Attribute Name = "+attribute.name());
			}
			oriAttrIdx_mergedAttrIdx.put(j, attributeName_index.indexOf(attribute.name()));
		}
		
		return oriAttrIdx_mergedAttrIdx;
	}
	
	private String makeChangedIndexInstance_BOW(TreeMap<Integer, String> attributeIndex_value) {
		String instance = "{";
		for(int index : attributeIndex_value.keySet()) {
			String value = attributeIndex_value.get(index);
			instance = instance+index+" "+value+",";
		}
		instance = instance.substring(0,instance.lastIndexOf(","))+"}";

		return instance;
	}

	private String makeChangedIndexInstance(TreeMap<Integer, String> attributeIndex_value) {
		String instance = "{";
		for(int index : attributeIndex_value.keySet()) {
			String value = attributeIndex_value.get(index);
			instance = instance+index+" "+value+",";
		}
		instance = instance.substring(0,instance.lastIndexOf(","))+"}";

		return instance;
	}

}
