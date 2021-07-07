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
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class DataFileMaker {
	ProjectInformation projectInformation;
//	
//	private final static String developerIDPatternStr = "'(.+)'";
//	private final static Pattern developerIDPattern = Pattern.compile(developerIDPatternStr);
//	
//	private final static String firstKeyPatternStr = "@attribute\\sKey\\s\\{([^,]+)";
//	private final static Pattern firstKeyPattern = Pattern.compile(firstKeyPatternStr);
//
//	private final static String firstDeveloperIDPatternStr = ".+\\{'\\s([^,]+)',.+\\}"; 
//	private final static Pattern firstDeveloperIDPattern = Pattern.compile(firstDeveloperIDPatternStr);

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
			totalDeveloperInstanceCSV = projectInformation.getOutputPath() +File.separator+projectInformation.getProjectName()+File.separator+"ProilingInstances.csv";
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
	
	public void makeDeveloperDefectInstanceArff(String mode) throws Exception {
		
		File dir = null;
		if(mode.equals("train")) {
			//make totalDevInstances directory
			dir = new File(projectInformation.getOutputPath() +File.separator+"totalDevDefectInstances");
			if(!dir.isDirectory()) {
				dir.mkdir();
			}
		}else if(mode.equals("test")) {
			dir = new File(projectInformation.getOutputPath() +File.separator+projectInformation.getProjectName()+File.separator+"DefectInstances");
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
			Instances filteredInstances = new Instances(newData, 0);
			
			//split arff file according to each developer
			ArrayList<String> developerDatas = new ArrayList<String>();
			String[] developerAttribute = filteredInstances.toString().split("\n");
			Attribute authorID = newData.attribute("meta_data-AuthorID");
			int index = authorID.index();

			for(int i = 0; i < authorID.numValues(); i++) {
				String developerID = parsingDeveloperName(authorID.value(i));
				String nominalToFilter = authorID.value(i);
				developerAttribute[index+2] = newAuthorIdAttribute(nominalToFilter,projectInformation.getProjectName());
				
				for(Instance instance : newData) {
					if(instance.stringValue(index).equals(nominalToFilter)) {
						String developerData = newDeveloperData(instance.toString(),index);
						developerDatas.add(developerData);
					}
				}
				
				if(developerDatas.size() == 0) {
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
			}
		}catch(Exception e) {
			System.out.println("The data file is wrong");
			System.exit(0);
		}
	}
	
	private String newDeveloperData(String line, int index) {
		if((line.contains(","+index+" "))) { //index previous,index commitTime, index key} 
			String front = line.substring(0,line.lastIndexOf(","+index));
			String rear = line.substring(line.lastIndexOf(","+index)+1,line.length());
			rear = rear.substring(rear.indexOf(","),rear.length());
			line = front + rear;
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
		
		String instancesStr = "([0-9]+)\\s([^,^}]+)"; 
		Pattern instancesPattern = Pattern.compile(instancesStr);
		
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
		
		//read csv file and save cluster_developerArff
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
			HashMap<String, Integer> attributeName_index = new HashMap<>(); //for change the attribute index of developers arff
			ArrayList<String> arffAttribute = new ArrayList<>();//for print cluster arff file
			ArrayList<String> authorId = new ArrayList<>();//for meta_data-AuthorID
			ArrayList<String> instances = new ArrayList<>();
			int attributeIndex = 0;
			int attriAuthorIdIndex = 0;
			
//System.out.println(clusterName);

			for(String developerArff : developerArffList) {
System.out.println(developerArff);

				DataSource source = new DataSource(developerArff);
				Instances data = source.getDataSet();
				HashMap<Integer,Integer> oriAttrIdx_mergedAttrIdx = new HashMap<>();
				
				//save authorId
				String thisAuthorId = data.get(0).stringValue(data.attribute("meta_data-AuthorID"));
				authorId.add(thisAuthorId);

				//attribute
				if(attributeIndex == 0 ) {
					//init about attribte variables
					attriAuthorIdIndex = data.attribute("meta_data-AuthorID").index();
					attributeIndex = initAttributeVariables(attributeIndex, data, attributeName_index, arffAttribute, oriAttrIdx_mergedAttrIdx);
				}else {
					//check duplicate attribute (if new attribute, add it)
					attributeIndex = addNonDuplicatedAttribute(data, attributeIndex, attributeName_index, arffAttribute, oriAttrIdx_mergedAttrIdx);
				}
				
				//Instances
				for(int i = 0; i < data.numInstances(); i++) {
//System.out.println("original : ");
//System.out.println(data.get(i).toString());
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
			}
			
			//make new meta_data-AuthorID
			makeNewAuthorIdInAttribute(arffAttribute, authorId, attriAuthorIdIndex);
			
			//make cluster arff file
			File newArff = new File(clusterModelFolderPath +File.separator+ clusterName +".arff");
			clusterArffPaths.add(newArff.getAbsolutePath());
			StringBuffer newContentBuf = new StringBuffer();

			newContentBuf.append("@relation weka.filters.unsupervised.instance.NonSparseToSparse-weka.filters.unsupervised.attribute.Remove-V-R1-220\n\n");
			//write attribute
			for (String line : arffAttribute) {
				if(line.startsWith("@attribute meta_data-commitTime")) continue;
				if(line.startsWith("@attribute Key {")) continue;
				newContentBuf.append(line + "\n");
			}
			
			newContentBuf.append("\n@data\n");

			for(String data : instances) {
				newContentBuf.append(data + "\n");
			}

			FileUtils.write(newArff, newContentBuf.toString(), "UTF-8");
			
		}
		System.out.println("Done Make Arff File");
		
		return clusterArffPaths;
	}
	
	private void makeNewAuthorIdInAttribute(ArrayList<String> arffAttribute, ArrayList<String> authorIds, int attriAuthorIdIndex) {
		String authorIDAttribute = "@attribute meta_data-AuthorID {";
		for(String authorId : authorIds) {
			authorIDAttribute = authorIDAttribute + authorId + ",";
		}
		authorIDAttribute = authorIDAttribute.substring(0,authorIDAttribute.length()-1);
		authorIDAttribute = authorIDAttribute+"}";
		arffAttribute.set(attriAuthorIdIndex, authorIDAttribute);
	}

	private String makeChangedIndexInstance(TreeMap<Integer, String> attributeIndex_value) {
		String instance = "{";
		for(int index : attributeIndex_value.keySet()) {
			String value = attributeIndex_value.get(index);
			instance = instance+index+" "+value+",";
		}
		instance = instance.substring(0,instance.length()-1);
		instance = instance+"}";
		return instance;
	}

	private int addNonDuplicatedAttribute(Instances data, int attributeIndex,
			HashMap<String, Integer> attributeName_index, ArrayList<String> arffAttribute, HashMap<Integer, Integer> originalAttriIndex_mergedAttriIndex) {
		
		for(int j = 0; j < data.numAttributes(); j++) {
			Attribute attribute = data.attribute(j);
			if(! (attributeName_index.containsKey(attribute.name())) ) {
				arffAttribute.add(attributeIndex,attribute.toString());
				attributeName_index.put(attribute.name(),attributeIndex);
				attributeIndex++;
			}
			originalAttriIndex_mergedAttriIndex.put(j, attributeName_index.get(attribute.name()));
		}
		
		return attributeIndex;
	}

	private int initAttributeVariables(int attributeIndex, Instances data,
			HashMap<String, Integer> attributeName_index, ArrayList<String> arffAttribute, HashMap<Integer, Integer> originalAttriIndex_mergedAttriIndex) {
		//save string of attribute
		for(attributeIndex = 0; attributeIndex < data.numAttributes(); attributeIndex++) {
			Attribute attribute = data.attribute(attributeIndex);
			arffAttribute.add(attributeIndex,attribute.toString());
			attributeName_index.put(attribute.name(), attributeIndex);
			originalAttriIndex_mergedAttriIndex.put(attributeIndex,attributeIndex);
		}
		return attributeIndex;
	}

}
