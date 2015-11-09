package util.comparators;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

import com.google.common.base.Joiner;

import json.JSONObject;
import pipeline.Pipeline;
import util.CompressGZIP;
import util.reviewDir.ReviewDirectory;

/*
 * Compares two annotation CSV files.
 * Reports the number of variants shared and the numbers of variants unique to each.
 * Reports the number of annotations shared and the numbers of annotations uniue to each.
 * 
 */
public class AnnotationComparator extends ReviewDirComparator {

	public AnnotationComparator(ReviewDirectory rd1, ReviewDirectory rd2, String analysisHeader) {
		super(rd1, rd2, analysisHeader);
	}
	
	@Override
	public void performComparison() {
		String CSV1 = rd1.getSampleManifest().getAnnotatedVars().getAbsolutePath();
		String CSV2 = rd2.getSampleManifest().getAnnotatedVars().getAbsolutePath();

		this.compareCSV(CSV1, CSV2);
	}
	
	private void compareCSV(String csvFile1, String csvFile2) {
		//Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		String[] csvLines1 = loadCSV(csvFile1);
		String[] csvLines2 = loadCSV(csvFile2);
		
		String[] csvLocs1 = getPosList(csvLines1);
		String[] csvLocs2 = getPosList(csvLines2);
		
		//LinkedHashMap<String, Integer> positionResults = SharedVarsCounts(csvLocs1, csvLocs2);
		//System.out.println(positionResults);
		
		// Retrieve variant positions shared between both to facilitate further
		// comparisons.
		ArrayList<ArrayList<String>> sharedLines = SharedVarLines(csvLines1, csvLines2, csvLocs1, csvLocs2);
		String[] sharedLines1 = new String[sharedLines.get(0).toArray().length];
		Object[] sharedLinesList1 = sharedLines.get(0).toArray();
		int counter = 0;
		for (Object obj : sharedLinesList1) {
			String s = (String) obj;
			sharedLines1[counter] = s;
			counter += 1;
		}
		String[] sharedLines2 = new String[sharedLines.get(0).toArray().length];
		counter = 0;
		Object[] sharedLinesList2 = sharedLines.get(1).toArray();
		for (Object obj : sharedLinesList2) {
			String s = (String) obj;
			sharedLines2[counter] = s;
			counter += 1;
		}
		System.out.println(sharedLines);

		LinkedHashMap<String, Object> compareSummary = new LinkedHashMap<String, Object>();
		
		logger.info("Comparing zygosity calls.");
		LinkedHashMap<String, Integer> zygosityResults = ZygosityCompare(
				sharedLines1, sharedLines2);
		
		logger.info("Comparing all fields.");
		LinkedHashMap<String, ArrayList<String>> fullComparison = FullComparison(
				sharedLines1, sharedLines2);
		
		logger.info("Now building summary");
/*		if (positionResults != null){
			//logger.info("Position intersection completed successfully. Adding to the comparison summary.");
			compareSummary.put("position.results", positionResults);
		}
		else
			logger.info("Position intersection is null. Not adding to comparison summary.");*/

		if (zygosityResults != null){
			//logger.info("Zygosity comparison completed successfully. Adding to the comparison summary.");
			compareSummary.put("zygosity.results", zygosityResults);
		}
		else
			logger.info("Zygosity comparison failed. Not adding to comparison summary.");
		if (fullComparison != null){
			//logger.info("Full comparison completed successfully. Adding to the comparison summary.");
			compareSummary.put("full.results", fullComparison);
		}
		else
			logger.info("Full comparison failed. Not adding to comparison summary.");
	}

	private LinkedHashMap<String, ArrayList<String>> FullComparison( String[] sharedLines1, String[] sharedLines2) {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		String[] header = sharedLines1[0].split("\t");
		Integer numCols = header.length;
		Integer numRows = sharedLines1.length;
		Integer numCols2 = sharedLines2[0].split("\t").length;
		if (numCols != numCols2) {
			//logger.info("Number of columns in csv 1 is " + numCols
			//		+ ", while number in csv 2 is " + numCols2
			//		+ ". It seems that these aren't the same.");
		}
		LinkedHashMap<String, ArrayList<String>> diffMap = new LinkedHashMap<String, ArrayList<String>>();
		Integer col = 0;
		Integer[] annotationSum = new Integer[numCols];
		Integer[] varSum = new Integer[numRows];
		for (int i = 0; i < varSum.length; i++)
			varSum[i] = 0;
		for (int i = 0; i < annotationSum.length; i++)
			annotationSum[i] = 0;
		while (col < numCols) {
			ArrayList<String> diffList = new ArrayList<String>();
			Integer row = 0;
			while (row < numRows) {
				if (!sharedLines1[row].split("\t")[col]
						.equals(sharedLines2[row].split("\t")[col])) {
					varSum[row]++;
					annotationSum[col]++;
					diffList.add(Joiner.on("\t").join(
							Arrays.asList(sharedLines1[row].split("\t"))
							.subList(0, 4)));

				}
				row++;
			}
			diffMap.put(header[col], diffList);
			col++;
		}
		double colAvgFrac = 0;
		Integer colSumDiff = 0;
		double varAvgFrac = 0;
		double varSumDiff = 0;
		ArrayList<String> colCntStr = new ArrayList<String>();
		ArrayList<String> varCntStr = new ArrayList<String>();
		ArrayList<String> colPctStr = new ArrayList<String>();
		ArrayList<String> varPctStr = new ArrayList<String>();
		for (Integer i = 0; i < numCols; i++) {
			colPctStr.add(String.format("%.2f", 100 * (double) annotationSum[i]
					/ (double) numRows)
					+ "%");
			colCntStr.add(String.format("%d", annotationSum[i]));
			colSumDiff += annotationSum[i];
			colAvgFrac += (double) annotationSum[i] / (double) numRows;
		}
		for (Integer i = 0; i < numRows; i++) {
			varPctStr.add(String.format("%.2f", 100 * (double) varSum[i]
					/ (double) numCols)
					+ "%");
			varCntStr.add(String.format("%d", varSum[i]));
			varSumDiff += varSum[i];
			varAvgFrac += (double) varSum[i] / (double) numCols;
		}
		colAvgFrac = colAvgFrac / numCols;
		//logger.info("Average annotation has this fraction of entries with differences between csvs: "
		//		+ colAvgFrac);

		//addNewEntry("Average # annotation differences", String.valueOf(colAvgFrac), "-", "");

		varAvgFrac = varAvgFrac / numRows;
		logger.info("Average variant has this fraction of entries with differences between csvs: "
				+ varAvgFrac);

		//addNewEntry("Average variant has this fraction of entries with differences between csvs", String.valueOf(varAvgFrac), "-", "");

		Integer maxVarDiffs = 0;
		String maxVarLoc = "N/A";
		Integer maxAnnotDiffs = 0;
		String maxAnnot = "N/A";
		for (int k = 0; k < annotationSum.length; k++) {
			if (annotationSum[k] > maxVarDiffs) {
				maxVarDiffs = annotationSum[k];
				maxVarLoc = Joiner.on("\t").join(
						Arrays.asList(sharedLines1[k].split("\t"))
						.subList(0, 4));
			}
		}
		for (int k = 0; k < varSum.length; k++) {
			if (varSum[k] > maxAnnotDiffs) {
				maxAnnotDiffs = varSum[k];
				maxAnnot = header[k];
			}
		}
		logger.info("Variant with the most differences between input files is located at "
				+ maxVarLoc + " with " + maxVarDiffs + " changes.");
		logger.info("Annotation with the most differences between input files is "
				+ maxAnnot + " with " + maxAnnotDiffs + " changes.");

		diffMap.put("columnPcts", colPctStr);
		diffMap.put("colCounts", colCntStr);
		diffMap.put("varPcts", varPctStr);
		diffMap.put("varCounts", varCntStr);

		return diffMap;
	}

	private ArrayList<ArrayList<String>> SharedVarLines(String[] csvLines1, String[] csvLines2, String[] csvLocs1, String[] csvLocs2) {
		ArrayList<String> sharedLines1 = new ArrayList<String>();
		ArrayList<String> sharedLines2 = new ArrayList<String>();
		int sharedCount = 0;
		String[] varList1 = new HashSet<String>(Arrays.asList(csvLocs1))
				.toArray(new String[csvLocs1.length]);
		String[] varList2 = new HashSet<String>(Arrays.asList(csvLocs1))
				.toArray(new String[csvLocs2.length]);
		for (String line : csvLines1) {
			if (Arrays.asList(csvLocs2).contains(
					Joiner.on("\t").join(
							Arrays.asList(line.split("\t")).subList(0, 4)
							.toArray()))) {
				sharedLines1.add(line);
				sharedCount += 1;
			}
		}
		for (String line : csvLines2) {
			if (Arrays.asList(csvLocs1).contains(
					Joiner.on("\t").join(
							Arrays.asList(line.split("\t")).subList(0, 4)
							.toArray())))
				sharedLines2.add(line);
		}
		ArrayList<ArrayList<String>> sharedLinesArray = new ArrayList<ArrayList<String>>();
		sharedLinesArray.add(sharedLines1);
		sharedLinesArray.add(sharedLines2);
		int uniq1 = csvLines1.length - sharedCount;
		int uniq2 = csvLines2.length - sharedCount;
		return sharedLinesArray;
	}

	private LinkedHashMap<String, Integer> SharedVarsCounts(String[] csvLocs1, String[] csvLocs2) {
		// Grab first 3 elements in the row and join them into a string to
		// check for unique locations.
		LinkedHashMap<String, Integer> positionResults = new LinkedHashMap<String, Integer>();

		String[] varList1 = new HashSet<String>(Arrays.asList(csvLocs1))
				.toArray(new String[csvLocs1.length]);
		String[] varList2 = new HashSet<String>(Arrays.asList(csvLocs1))
				.toArray(new String[csvLocs1.length]);
		int sharedLocs = 0;
		int uniqLocs1 = 0;
		int uniqLocs2 = 0;
		for (String loc1 : varList1) {
			if (Arrays.asList(varList2).contains(loc1))
				sharedLocs += 1;
			else
				uniqLocs1 += 1;
		}
		for (String loc2 : varList2) {
			if (!Arrays.asList(varList1).contains(loc2))
				uniqLocs2 += 1;
		}
		positionResults.put("sharedVariants", sharedLocs);
		positionResults.put("uniq1Variants", uniqLocs1);
		positionResults.put("uniq2Variants", uniqLocs2);
		return positionResults;
	}

	private LinkedHashMap<String, Integer> ZygosityCompare(
			String[] sharedLines1, String[] sharedLines2) {
		LinkedHashMap<String, Integer> zygosityResults = new LinkedHashMap<String, Integer>();
		int diffs = 0;
		try {
			assert sharedLines1.length == sharedLines2.length;
		} catch (AssertionError e) {
			throw new AssertionError(
					"The two string arrays provided are of unequal length. This never happens in normal usage");
		}
		String[] zygosity1 = new String[sharedLines1.length];
		String[] zygosity2 = new String[sharedLines1.length];
		int i = 0;
		for (String line1 : sharedLines1)
			zygosity1[i] = line1.split("\t")[7];
		for (String line2 : sharedLines2)
			zygosity2[i] = line2.split("\t")[7];
		if (!zygosity2[i].equals(zygosity1[i]))
			diffs += 1;
		zygosityResults.put("discord", diffs);
		zygosityResults.put("agreement", sharedLines1.length - diffs);
		return zygosityResults;
	}

	private String[] loadCSV(String csvFile) {
		List<String> lineList = new ArrayList<String>();
		try{
			
			BufferedReader csvReader = new BufferedReader(new FileReader(csvFile));
			String str;
			while ((str = csvReader.readLine()) != null) {
				lineList.add(str);
			}
			csvReader.close();
		} catch (IOException e) {
			System.out.println("Error reading file.");
			e.printStackTrace();
		}
		return lineList.toArray(new String[0]);
	}
	
	private String[] getPosList(String[] csvLines) {
		String[] csvLocs = new String[csvLines.length];
		Integer i = 0;
		for (String line : csvLines) {
			// if(line.startsWith("#"))
			// continue;
			csvLocs[i] = Joiner.on("\t").join(
					Arrays.asList(line.split("\t")).subList(0, 4).toArray());
			i += 1;
		}
		return csvLocs;
	}
	
}