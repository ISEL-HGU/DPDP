package edu.handong.csee.isel;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import edu.handong.csee.isel.data.DeveloperInfo;
import edu.handong.csee.isel.data.MetaData;

public class Utils {
	public static MetaData readMetadataCSV(String metadataPath) throws IOException {
        ArrayList<HashMap<String, String>> metricToValueMapList = new ArrayList<>();

        Reader in = new FileReader(metadataPath);
        Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader().parse(in);

        for (CSVRecord record : records) {
        	if(DPDPMain.excludedDeveloper.contains(record.get("AuthorID"))) {
        		continue;
        	}

            HashMap<String, String> metricToValueMap = new HashMap<>();

            for (String metric : MetaData.headers) {

                metricToValueMap.put(metric, record.get(metric));
            }

            metricToValueMapList.add(metricToValueMap);
        }

        MetaData metaData = new MetaData(Arrays.asList(DeveloperInfo.CSVHeader), metricToValueMapList);
        return metaData;
    }
}
