package edu.handong.csee.isel.developer;

import edu.handong.csee.isel.ProjectInformation;

public class DeveloperInstanceMaker {
	ProjectInformation projectInformation;
	
	public DeveloperInstanceMaker(ProjectInformation projectInformation) {
		this.projectInformation = projectInformation;
	}

	public void makeDeveloperInstanceCSV() throws Exception {
		//read CSV
		String[] developerProfilingMetrics = new String[7];
//		System.out.println(projectInformation.getDeveloperProfilingInstanceCSVPath());
//		System.out.println(projectInformation.getReferenceFolderPath());
//		System.out.println();
		developerProfilingMetrics[0] = "-m";
		developerProfilingMetrics[1] = projectInformation.getDeveloperProfilingInstanceCSVPath();
		developerProfilingMetrics[2] = "-o";
		developerProfilingMetrics[3] = projectInformation.getReferenceFolderPath();
		developerProfilingMetrics[4] = "-w";
		developerProfilingMetrics[5] = "-p";
		developerProfilingMetrics[6] = projectInformation.getProjectName();

		DeveloperProfilingMetric developerProfilingMetric = new DeveloperProfilingMetric();
		developerProfilingMetric.run(developerProfilingMetrics);
		projectInformation.setDeveloperProfilingInstanceCSVPath(developerProfilingMetric.getOutpuCSV());
	}

}
