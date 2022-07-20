package edu.handong.csee.isel.data;

import java.util.Map;

public class DeveloperInfo {
    public static String[] CSVHeader = {"ID","totalCommit","totalCommitPath", "meanEditedLineInCommit", "meanEditedLineInCommitPath", "varianceOfCommit", "varianceOfCommitPath", "meanOfAddedLineOfCommit","meanOfAddedLineOfCommitPath","meanOfDeletedLineOfCommit","meanOfDeletedLineOfCommitPath","meanOfDistributionModifiedLineOfCommit","meanOfNumOfSubsystem","meanOfNumOfDirectories","meanOfNumOfFiles","meanOfLT","meanOfEXP","meanOfREXP","meanOfSEXP","structural","semantic","proportionBFC","Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat","0h","1h","2h","3h","4h","5h","6h","7h","8h","9h","10h","11h","12h","13h","14h","15h","16h","17h","18h","19h","20h","21h","22h","23h"};

    public DeveloperInfo(String id
    		, double totalCommit
    		, double totalCommitPath
    		, double meanEditedLineInCommit
    		, double meanEditedLineInCommitPath
    		, double varianceOfCommit
    		, double varianceOfCommitPath
            , double meanOfAddedLineOfCommit
            , double meanOfAddedLineOfCommitPath
            , double meanOfDeletedLineOfCommit
            , double meanOfDeletedLineOfCommitPath
            , double meanOfDistributionModifiedLineOfCommit
            , double meanOfNumOfSubsystem
            , double meanOfNumOfDirectories
            , double meanOfNumOfFiles
            , double meanLT
            , double meanEXP
            , double meanREXP
            , double meanSEXP
            , double structural
            , double semantic
            , double proportionBFC
            , Map<WeekDay,Double> weekRatioMap, Map<Integer,Double> hourMap) {
        ID = id;
        this.totalCommit = totalCommit;
        this.totalCommitPath = totalCommitPath;
        this.meanEditedLineInCommit = meanEditedLineInCommit;
        this.meanEditedLineInCommitPath = meanEditedLineInCommitPath;
        this.varianceOfCommit = varianceOfCommit;
        this.varianceOfCommitPath = varianceOfCommitPath;
        this.weekRatioMap = weekRatioMap;
        this.hourMap =hourMap;

        this.meanOfAddedLineOfCommit = meanOfAddedLineOfCommit;
        this.meanOfAddedLineOfCommitPath = meanOfAddedLineOfCommitPath;
        this.meanOfDeletedLineOfCommit = meanOfDeletedLineOfCommit;
        this.meanOfDeletedLineOfCommitPath = meanOfDeletedLineOfCommitPath;
        this.meanOfDistributionModifiedLineOfCommit = meanOfDistributionModifiedLineOfCommit;
        this.meanOfNumOfSubsystem = meanOfNumOfSubsystem;
        this.meanOfNumOfDirectories = meanOfNumOfDirectories;
        this.meanOfNumOfFiles = meanOfNumOfFiles;
        
        this.meanLT = meanLT;
        this.meanEXP = meanEXP;
        this.meanREXP = meanREXP;
        this.meanSEXP = meanSEXP;
        this.proportionBFC = proportionBFC;
        this.structural = structural;
        this.semantic = semantic;
    }



    static public enum WeekDay {Sun, Mon, Tue, Wed, Thu, Fri, Sat}


    public final String ID;
    public final double totalCommit;
    public final double totalCommitPath;
    public final double meanEditedLineInCommit;
    public final double meanEditedLineInCommitPath;
    public final double varianceOfCommit;
    public final double varianceOfCommitPath;

    public final double meanOfAddedLineOfCommit;
    public final double meanOfAddedLineOfCommitPath;
    public final double meanOfDeletedLineOfCommit;
    public final double meanOfDeletedLineOfCommitPath;
    public final double meanOfDistributionModifiedLineOfCommit;
    public final double meanOfNumOfSubsystem;
    public final double meanOfNumOfDirectories;
    public final double meanOfNumOfFiles;
    public final double meanLT;
    public final double meanEXP;
    public final double meanREXP;
    public final double meanSEXP;
    public final double proportionBFC;
    public final double structural;
    public final double semantic;

    /*
    meanOfAddedLineOfCommit
    meanOfAddedLineOfCommitPath
    meanOfDeletedLineOfCommit
    meanOfDeletedLineOfCommitPath
    meanOfDistributionModifiedLineOfCommit
    meanOfDistributionModifiedLineOfCommitPath
    meanOfNumOfSubsystem
    meanOfNumOfDirectories
    meanOfNumOfFiles
     */
    
    

    public final Map<WeekDay,Double> weekRatioMap;
    public final Map<Integer,Double> hourMap;

}
