package util.Comparators;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import json.JSONException;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.CSVFile;
import buffer.FileBuffer;

import com.google.common.base.Joiner;

import java.util.logging.Logger;


/*
 * Compares two annotation CSV files.
 * Reports the number of variants shared and the numbers of variants unique to each.
 * Reports the number of annotations shared and the numbers of annotations uniue to each.
 * 
 */
public class CompareAnnotationCSVs extends IOOperator{
	
	private HashMap<String, Object> CSVCompare(String csvFile1, String csvFile2) throws IOException{
		HashMap<String, Object> compareSummary = new HashMap<String, Object>();
		//Load csv1 into a String array
		BufferedReader csvReader1 = new BufferedReader(new FileReader(csvFile1));
        String str;
        List<String> list1 = new ArrayList<String>();
        while((str = csvReader1.readLine()) != null){
            list1.add(str);
        }
        String[] csvLines1 = list1.toArray(new String[0]);
        //Load csv2 into a String array
		BufferedReader csvReader2 = new BufferedReader(new FileReader(csvFile1));
        List<String> list2 = new ArrayList<String>();
        while((str = csvReader2.readLine()) != null){
            list2.add(str);
        }
        String[] csvLines2 = list2.toArray(new String[0]);
        
        String[] csvLocs1 = new String[csvLines1.length];
        String[] csvLocs2 = new String[csvLines2.length];
        int i=0;
        for(String line : csvLines1) {
        	if(line.startsWith("#"))
        		continue;
        	csvLocs1[i] = Joiner.on("\t").join(Arrays.asList(line.split("\t")).subList(0, 2).toArray()); 
        	i+=1;
        }
        i=0;
        for(String line : csvLines2) {
        	if(line.startsWith("#"))
        		continue;
        	csvLocs2[i] = Joiner.on("\t").join(Arrays.asList(line.split("\t")).subList(0, 2).toArray());
        	i+=1;
        }
        
        HashMap<String, Integer> positionResults = SharedVars(csvLocs1, csvLocs2);
        HashMap<String, Integer> zygosityResults = ZygosityCompare(csvLines1, csvLines2);
        compareSummary.put("positionResults", positionResults);
        compareSummary.put("zygosityResults", zygosityResults);

		return compareSummary;
	}

	private HashMap<String, Integer> SharedVars(String[] csvLocs1, String[] csvLocs2){
        //Grab first 3 elements in in the row and join them into a string to check for unique locations.
		HashMap<String, Integer> positionResults = new HashMap<>();

        String[] varList1 = new HashSet<String>(Arrays.asList(csvLocs1)).toArray(new String[csvLocs1.length]);
        String[] varList2 = new HashSet<String>(Arrays.asList(csvLocs1)).toArray(new String[csvLocs1.length]);
        int sharedLocs = 0;
        int uniqLocs1 = 0;
        int uniqLocs2 = 0;
        for( String loc1 : varList1){
        	if(Arrays.asList(varList2).contains(loc1))
        		sharedLocs += 1;
        	else
        		uniqLocs1 += 1;
        }
        for( String loc2 : varList2) {
        	if(!Arrays.asList(varList1).contains(loc2))
        		uniqLocs2 += 1;
        }
        positionResults.put("sharedVariants", sharedLocs);
        positionResults.put("uniq1Variants", uniqLocs1);
        positionResults.put("uniq2Variants", uniqLocs2);
		return positionResults;
	}

	private HashMap<String, Integer> ZygosityCompare(String[] csvLines1, String[] csvLines2){
		HashMap<String, Integer> zygosityResults = new HashMap<>();
        String[] csvLocs1 = new String[csvLines1.length];
        String[] csvLocs2 = new String[csvLines2.length];
        String[] zygosity1 = new String[csvLines1.length];
        String[] zygosity2 = new String[csvLines2.length];
        int i=0;
        // Create zygosity and position arrays
        for(String line : csvLines1) {
        	if(line.startsWith("#"))
        		continue;
        	csvLocs1[i] = Joiner.on("\t").join(Arrays.asList(line.split("\t")).subList(0, 2).toArray());
        	zygosity1[i] = line.split("\t")[7];
        	i+=1;
        }
        i=0;
        for(String line : csvLines2) {
        	if(line.startsWith("#"))
        		continue;
        	csvLocs2[i] = Joiner.on("\t").join(Arrays.asList(line.split("\t")).subList(0, 2).toArray());
        	zygosity2[i] = line.split("\t")[7];
        	i+=1;
        }
        // Create list of variants to facilitate comparison of zygosity
        String[] varList1 = new HashSet<String>(Arrays.asList(csvLocs1)).toArray(new String[csvLocs1.length]);
        String[] varList2 = new HashSet<String>(Arrays.asList(csvLocs1)).toArray(new String[csvLocs1.length]);
		return zygosityResults;
	}

	
	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		List<FileBuffer> CSVs = this.getAllInputBuffersForClass(CSVFile.class);
		if(CSVs.size() != 2){
			throw new OperationFailedException("Exactly two CSVs required for this operator.", this);
		}
		String CSV1 = CSVs.get(0).getAbsolutePath();
		String CSV2 = CSVs.get(1).getAbsolutePath();
		
		HashMap<String, Object> Results = CSVCompare(CSV1, CSV2);
		
	}
	
}