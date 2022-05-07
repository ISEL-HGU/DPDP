package edu.handong.csee.isel.data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import edu.handong.csee.isel.ProjectInformation;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class DataFileMaker {
	ProjectInformation projectInformation;

	public DataFileMaker(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}

	public void makeDeveloperProfilingCSV() throws Exception {
		//read CSV
		String[] developerProfilingMetrics = new String[8];

		//make totalDevInstances directory
		String totalDeveloperInstanceCSV = getDirPathToSaveCSVfiles(projectInformation);
System.out.println(projectInformation.getDeveloperDataCSVPath());
System.out.println(totalDeveloperInstanceCSV);
System.out.println(projectInformation.getProjectName());
System.out.println(projectInformation.getLocationOfClusterModels());
		developerProfilingMetrics[0] = "-m";
		developerProfilingMetrics[1] = projectInformation.getDeveloperDataCSVPath();
		developerProfilingMetrics[2] = "-o";
		developerProfilingMetrics[3] = totalDeveloperInstanceCSV;
		developerProfilingMetrics[4] = "-p";
		developerProfilingMetrics[5] = projectInformation.getProjectName();
		developerProfilingMetrics[6] = "-r";
		developerProfilingMetrics[7] = projectInformation.getLocationOfClusterModels();
		

		DeveloperProfilingMetric developerProfilingMetric = new DeveloperProfilingMetric();
		developerProfilingMetric.run(developerProfilingMetrics);
	System.exit(0);
	}

	public void makeDeveloperArff() throws Exception {

		HashMap<String,String> developerDefectInstancePath = new HashMap<>();

		// developer arff directory path
		String totalDevDefectInstancesForder = getDirPathToSaveArffFiles(projectInformation);

		//preprocess arff file(remove BOW and re-arrange attribute according to define in order of DefectAttribute.attribute)
		String preprocessedArffPath = preprocessArffFile(projectInformation);

		try {//split arff file according to each developer

			//read preprocessed arff file
			DataSource source = new DataSource(preprocessedArffPath);
			Instances data = source.getDataSet();

			//init new instances with only attribute
			Instances filteredInstances = new Instances(data, 0);

			ArrayList<String> developerDatas = new ArrayList<String>();
			String[] developerAttribute = filteredInstances.toString().split("\n");
			Attribute authorID = data.attribute("meta_data-AuthorID");
			int authorIDindex = authorID.index();

			for(int i = 0; i < authorID.numValues(); i++) {
				String developerID = parsingDeveloperName(authorID.value(i));
				String authorIDstringInInstance = authorID.value(i);

				for(Instance instance : data) {

					//check if authorId is in a instance (because first authorId of the attribute is not expressed as a instance value)
					if(instance.stringValue(authorIDindex).equals(authorIDstringInInstance)) {
						String developerData = removeAuthorIDinInstance(instance.toString(),authorIDindex);
						developerDatas.add(developerData);
					}
				}

				//make developer arff file
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
	}

	private String preprocessArffFile(ProjectInformation projectInformation) throws Exception {
		String preprocessedArffPath = null;

		//임시!!!DPMiner에서 key값을 절대 겹칠것 같지 않은 값으로 바꾸기 
		changeKeyIntometaKey(projectInformation.getInputPath());
		
		//process bow option
		if(projectInformation.isBow()) { 
			//removeBOWAttribute(input arff path,output arff path)
			preprocessedArffPath = removeBOWAttribute(projectInformation.getInputPath(), projectInformation.getReferenceFolderPath(), projectInformation.getProjectName());
		}else {
			preprocessedArffPath = projectInformation.getInputPath();
		}

		//re-arrange attribute according to define in order of DefectAttribute.attribute
		preprocessedArffPath = rearrangeAttributeOrder(preprocessedArffPath);

		return preprocessedArffPath;
	}

	private void changeKeyIntometaKey(String inputPath) throws IOException {
		String content = FileUtils.readFileToString(new File(inputPath), "UTF-8");
		String[] lines = content.split("\n");
		
		for(int i = 0; i<lines.length; i++) {
			if(lines[i].startsWith("@attribute Key {")) {
				lines[i] = lines[i].replace("@attribute Key {", "@attribute isel_meata_data-Key {");
				break;
			}
		}
		
		File newAttributeArff = new File(inputPath);
		StringBuffer newAttributeContentBuf = new StringBuffer();
		
		for(String instance : lines) {
			newAttributeContentBuf.append(instance + "\n");
		}
		FileUtils.write(newAttributeArff, newAttributeContentBuf.toString(), "UTF-8");
	}

	private String rearrangeAttributeOrder(String preprocessedArffPath) throws Exception {
		ArrayList<String> arffAttribute = DefectAttribute.attribute;
		ArrayList<String> attribute_name = DefectAttribute.attribute_name;

		//read arff file to weka format (to parsing index of arff file)
		DataSource source = new DataSource(preprocessedArffPath);
		Instances newData = source.getDataSet();

		//parsing sparse type index and value
		Pattern instancesPattern = Pattern.compile("([0-9]+)\\s([^,^}]+)");
		HashMap<Integer,Integer> oriAttrIdx_mergedAttrIdx = matchingOriginalAtrrIdxWithMergedAtrrIdx(attribute_name, newData);
		ArrayList<String> rearrangedInstances = new ArrayList<>();

		for(int i = 0; i < newData.numInstances(); i++) {
			TreeMap<Integer,String> attributeIndex_value = new TreeMap<>();
			Matcher matcher = instancesPattern.matcher(newData.get(i).toString());

			//parsing a instance (ex {0 clean} -> index : 0, value : clean
			while(matcher.find()) {
				int index = Integer.parseInt(matcher.group(1));
				String value = matcher.group(2);
				index = oriAttrIdx_mergedAttrIdx.get(index);
				attributeIndex_value.put(index, value);
			}
			//make re arranged instances
			rearrangedInstances.add(rearrangeInstanceOrder(attributeIndex_value));
		}

		//write the attribute of arranged arff
		File newAttributeArff = new File(preprocessedArffPath);
		StringBuffer newAttributeContentBuf = new StringBuffer();

		for (String line : arffAttribute) {
			newAttributeContentBuf.append(line + "\n");
		}
		newAttributeContentBuf.append(newData.attribute("meta_data-AuthorID").toString()+ "\n");
		newAttributeContentBuf.append("\n@data\n");

		for(String instance : rearrangedInstances) {
			newAttributeContentBuf.append(instance + "\n");
		}
		FileUtils.write(newAttributeArff, newAttributeContentBuf.toString(), "UTF-8");

		return preprocessedArffPath;
	}

	private String removeBOWAttribute(String inputArffPath, String outputArffPath, String projectName) throws Exception {
		ArrayList<String> attribute_name = DefectAttribute.attribute_name;
		DataSource source = new DataSource(inputArffPath);
		Instances data = source.getDataSet();
		ArrayList<Integer> attribute_index = parsingValidAttributeIndex(data,attribute_name);

		//delete key, commitTime and BOW attribute
		int[] toSelect = new int[attribute_index.size()];

		for (int i = 0, j = 0; i < attribute_index.size(); i++,j++) {
			toSelect[i] = attribute_index.get(j);
		}

		Remove removeFilter = new Remove();
		removeFilter.setAttributeIndicesArray(toSelect);
		removeFilter.setInvertSelection(true);
		removeFilter.setInputFormat(data);

		//apply filter
		Instances newData = Filter.useFilter(data, removeFilter);

		//saved filtered result
		ArffSaver saver = new ArffSaver();
		saver.setInstances(newData);
		outputArffPath = outputArffPath + File.separator + projectName + "-bow.arff";
		saver.setFile(new File(outputArffPath));
		saver.writeBatch();

		return outputArffPath;
	}

	private ArrayList<Integer> parsingValidAttributeIndex(Instances data, ArrayList<String> attribute_name) {
		ArrayList<Integer> attribute_index = new ArrayList<>();

		for(int j = 0; j < data.numAttributes(); j++) {
			Attribute attribute = data.attribute(j);
			if(attribute_name.contains(attribute.name())) {
				attribute_index.add(j);
			}
		}

		return attribute_index;
	}

	private String getDirPathToSaveCSVfiles(ProjectInformation projectInformation) {
		File dir = null;
		String totalDeveloperInstanceCSV = null;

		if(!projectInformation.isTestSubOption_once()) {
			dir = new File(projectInformation.getOutputPath());
			if(!dir.isDirectory()) {
				dir.mkdir();
			}
			//total developer Instance CSV path
			totalDeveloperInstanceCSV = dir.getAbsolutePath() + File.separator+"Developer_Profiling.csv";
		}else if(projectInformation.isTestSubOption_once()) {
			totalDeveloperInstanceCSV = projectInformation.getTestFolderPath()+File.separator+"ProilingInstances.csv";
			projectInformation.setTestDeveloperProfilingInstanceCSV(totalDeveloperInstanceCSV);
		}
		return totalDeveloperInstanceCSV;
	}

	private String getDirPathToSaveArffFiles(ProjectInformation projectInformation) {
		File dir = null;
		
		if(!projectInformation.isTestSubOption_once()) {
			//make totalDevInstances directory
			dir = new File(projectInformation.getOutputPath() +File.separator+"totalDevDefectInstances");
			if(!dir.isDirectory()) {
				dir.mkdir();
			}
		}else if(projectInformation.isTestSubOption_once()) {
			File testDir = new File(projectInformation.getOutputPath() +File.separator+projectInformation.getProjectName()+"_test");
			String directoryPath = testDir.getAbsolutePath();
			
			if(testDir.isDirectory()) {
				deleteFile(directoryPath);
			}
			testDir.mkdir();
			projectInformation.setTestFolderPath(testDir.getAbsolutePath());
			
			String folderPath = testDir.getAbsolutePath();
			dir = new File(folderPath+File.separator+"DefectInstances");
			dir.mkdir();
			
			projectInformation.setTestDeveloperDefectInstanceArff(dir.getAbsolutePath());
		}

		return dir.getAbsolutePath();
	}

	private void reduceBICValue(String defectDataArffPath) throws Exception {
		DataSource source = new DataSource(defectDataArffPath);
		Instances data = source.getDataSet();
		data.setClassIndex(0);

		Attribute numOfBIC = data.attribute("meta_data-numOfBIC");

		for(Instance instance : data) {
			if(instance.value(numOfBIC) > 0) {
				instance.setValue(numOfBIC, instance.value(numOfBIC)-1);
			}
		}

		ArffSaver saver = new ArffSaver();
		saver.setInstances(data);
		saver.setFile(new File(defectDataArffPath));
		saver.writeBatch();

	}

	private String removeAuthorIDinInstance(String line, int index) {
		if((line.contains(","+index+" "))) { //{index c-vector,index meta} 
			String front = line.substring(0,line.lastIndexOf(","+index));
			line = front + "}";
		}
		return line;
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

	public ArrayList<String> makeClusterArffForTraining() throws Exception {
		ArrayList<String> clusterArffPaths = new ArrayList<>();

		Pattern instancesPattern = Pattern.compile("([0-9]+)\\s([^,^}]+)");

		//cluster csv folder
		File clusterCSVfolder = new File(projectInformation.getInputPath()+File.separator+"ClusterCSV");

		//developer arff folder
		String developerArffFolder = projectInformation.getInputPath()+File.separator+"totalDevDefectInstances";

		File clusterModelFolder = new File(projectInformation.getInputPath() +File.separator+"ClusterArff");
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
			ArrayList<String> attribute_name = DefectAttribute.attribute_name; //for change the attribute index of developers arff
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
				HashMap<Integer,Integer> oriAttrIdx_mergedAttrIdx = matchingOriginalAtrrIdxWithMergedAtrrIdx(attribute_name, data);
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

	private HashMap<Integer, Integer> matchingOriginalAtrrIdxWithMergedAtrrIdx(ArrayList<String> attribute_name, Instances data) {
		HashMap<Integer, Integer> oriAttrIdx_mergedAttrIdx = new HashMap<>();
		for(int j = 0; j < data.numAttributes(); j++) {
			Attribute attribute = data.attribute(j);
			int index = attribute_name.indexOf(attribute.name());
			if(index == -1) {
				System.out.println("!!!!!!!!!!!!!There is new Attribute~!!!!!!!!!!!!!");
				System.out.println("Attribute Name = "+attribute.name());
			}
			oriAttrIdx_mergedAttrIdx.put(j, attribute_name.indexOf(attribute.name()));
		}

		return oriAttrIdx_mergedAttrIdx;
	}

	private String rearrangeInstanceOrder(TreeMap<Integer, String> attributeIndex_value) {
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
