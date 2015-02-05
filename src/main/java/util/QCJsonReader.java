package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import math.Histogram;
import net.sf.samtools.util.DateParser;
import util.prereviewDataGen.AnalysisTypeConverter;
import util.reviewDir.ManifestParseException;
import util.reviewDir.ReviewDirectory;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * A smallish utility to read QC data from qc.json files
 * @author brendan
 *
 */
public class QCJsonReader {

	static DecimalFormat formatter = new DecimalFormat("0.0##");
	static DecimalFormat smallFormatter = new DecimalFormat("0.00000");
	
	public static JSONObject toJSONObj(String path) throws IOException, JSONException {
		File file = new File(path);
		if (file.isDirectory()) {
			//If file is a directory, see if it's in the 'reviewdir' format
			File qcDir = new File(file.getAbsoluteFile() + "/qc/");
			if (qcDir.exists() && qcDir.isDirectory()) {
				File qcFile = new File(qcDir.getPath() + "/qc.json");
				if (qcFile.exists()) {
					file = qcFile;
				}
			}
			
		}
		
		StringBuilder str = new StringBuilder();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		while(line != null) {
			str.append(line);
			line = reader.readLine();
		}
		reader.close();
		return new JSONObject(str.toString());
	}
	
	private static String safeDouble(JSONObject obj, String key) {
		if (obj.has(key)) {
			try {
				return "" + formatter.format(obj.getDouble(key));
			} catch (JSONException e) {
				return "Error";
			}
		}
		else {
			return "Not found";
		}
	}
	
	private static String safeInt(JSONObject obj, String key) {
		if (obj.has(key)) {
			try {
				return "" + obj.getInt(key);
			} catch (JSONException e) {
				return "Error";
			}
		}
		else {
			return "Not found";
		}
	}
	
	private static String safeLong(JSONObject obj, String key) {
		if (obj.has(key)) {
			try {
				return "" + obj.getLong(key);
			} catch (JSONException e) {
				return "Error";
			}
		}
		else {
			return "Not found";
		}
	}
	
	/**
	 * Display some quick summary information about the QC data
	 * @param paths
	 */
	public static void performSummary(List<String> paths, PrintStream output) {
		
		for(String path : paths) {
			try {
				JSONObject obj = toJSONObj(path);
				String sampleInfo = sampleIDFromManifest(new File(path + "/sampleManifest.txt"));
				output.println("\n\nSummary for : " + sampleInfo);
				output.println("\t Bases & Alignment summary:");
				if (obj.has("raw.bam.metrics")) {
					JSONObject rawBamMetrics = obj.getJSONObject("raw.bam.metrics");
					JSONObject finalBamMetrics = obj.getJSONObject("final.bam.metrics");
					
					Long baseCount = rawBamMetrics.getLong("bases.read");
					Long baseQ30 = rawBamMetrics.getLong("bases.above.q30");
					Long baseQ10 = rawBamMetrics.getLong("bases.above.q10");
					
					output.println("Raw bases read: " + baseCount);
					output.println("Raw % > Q30 : " + formatter.format(100.0*(double)baseQ30 / (double)baseCount));
					output.println("Raw % > Q10 : " + formatter.format(100.0*(double)baseQ10 / (double)baseCount));
					
					long totRawReads = rawBamMetrics.getLong("total.reads");
					long unmappedRawReads = rawBamMetrics.getLong("unmapped.reads");
					long totFinalReads = finalBamMetrics.getLong("total.reads");
					long unmappedFinalReads = finalBamMetrics.getLong("unmapped.reads");
					double rawUnmappedFrac = (double)unmappedRawReads / (double) totRawReads;
					double finalUnmappedFrac = (double)unmappedFinalReads / (double) totFinalReads;
					output.println("Total raw/final reads: " + totRawReads +",  " + totFinalReads);
					output.println("Frac PCR dups removed: " + formatter.format(1.0 - (double)totFinalReads/(double)totRawReads));
					output.println("Fraction unmapped raw/final : " + formatter.format(rawUnmappedFrac) +",  " + formatter.format(finalUnmappedFrac));
				}
				else {
					output.println("No raw base metrics found");
				}
				
				output.println("\n\t Coverage summary:");
				if (obj.has("raw.coverage.metrics") && obj.has("final.coverage.metrics")) {
					
					JSONObject rawCov = obj.getJSONObject("raw.coverage.metrics");
					JSONObject finalCov = obj.getJSONObject("final.coverage.metrics");
					
					JSONArray rawFracCov = rawCov.getJSONArray("fraction.above.cov.cutoff");
					JSONArray finalFracCov = finalCov.getJSONArray("fraction.above.cov.cutoff");
					JSONArray rawCovCutoff = rawCov.getJSONArray("coverage.cutoffs");
					JSONArray finalCovCutoff = rawCov.getJSONArray("coverage.cutoffs");

					if (finalCovCutoff.length() != rawCovCutoff.length()) {
						//This would be really weird, but not impossible I guess
						System.err.println("Raw and final coverage cutoffs are not the same!!");
					}
					
					output.println("\t Raw \t Final:");
					for(int i=0; i<rawFracCov.length(); i++) {
						output.println("% > " + rawCovCutoff.getInt(i) + ":\t" + formatter.format(rawFracCov.getDouble(i)) + "\t" + formatter.format(finalFracCov.getDouble(i)));
					}
			
				}
				else {
					System.out.println("No raw coverage metrics found");
				}
				
				
				//Variant rundown
				if (obj.has("variant.metrics") ) {
					output.println("\n\t Variant summary:");
					JSONObject varMetrics = obj.getJSONObject("variant.metrics");
					output.println("Total variants: " + safeInt(varMetrics, "total.vars"));
					output.println("Total snps: " + safeInt(varMetrics, "total.snps"));
					output.println("Overall TT: " + safeDouble(varMetrics, "total.tt.ratio"));
					output.println("Known / Novel TT: " + safeDouble(varMetrics, "known.tt") + " / " + safeDouble(varMetrics, "novel.tt"));
					Double totVars = varMetrics.getDouble("total.vars");
					Double knowns = varMetrics.getDouble("total.known");
					if (totVars > 0) {
						Double novelFrac = 1.0 - knowns / totVars;
						output.println("Fraction novel: " + formatter.format(novelFrac));
					}
					//output.println("Total novels: " + safeDouble(varMetrics, " "));
					
					if (obj.has("capture.extent")) {
						long extent = obj.getLong("capture.extent");
						double varsPerBaseCalled = totVars / (double)extent;
						output.println("Vars per base called: " + smallFormatter.format(varsPerBaseCalled));
					}
					else {
						output.println("Vars per base called: No capture.extent available");
					}
					
				}
				else {
					output.println("No variant metrics found");
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	public static void main(String[] args) {
		
		if (args.length==0) {
			System.err.println("Enter <command> qcfile1 [qcfile2 ...]");
			return;
		}
		
		String command = args[0];
		List<String> paths = new ArrayList<String>();
		for(int i=1; i<args.length; i++) {
			paths.add(args[i]);
		}
		
		if (command.startsWith("sum")) {
			performSummary(paths, System.out);
			return;
		}
		
		if (command.startsWith("comp")) {
			try {
				performComparison(paths, System.out);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		
		if (command.startsWith("qa")) {
			performQAIndicatorsByMonth(paths, System.out, null);
			return;
		}
		
		if (command.startsWith("valid")) {
			performTableize(paths, System.out);
			return;
		}
		
		if (command.startsWith("varSum")) {
			performVarSummary(paths, System.out);
			return;
		}
		
		if (command.startsWith("covSum")) {
			performCovSummary(paths, System.out);
			return;
		}
		
		if (command.startsWith("covGraph")) {
			performCovGraph(paths, System.out);
			return;
		}
		
		if (command.startsWith("qcList")) {
			performQCList(paths, System.out, null);
			return;
		}
		
		if (command.startsWith("emit")) {
			paths.remove(0); //remove first element, it's the emit string
			performEmit(args[1], paths, System.out);
			return;
		}
		
		System.err.println("Unrecognized command");
		
	}

	/**
	 * Emit a single qc metric with no other output. The 
	 * @param paths
	 * @param out
	 * @param converter
	 */
	public static void performEmit(String metric, List<String> paths, PrintStream out) {
		for(String path : paths) {
			try {
				JSONObject obj = toJSONObj(path);
				
				String[] mPath = metric.split(":");
				boolean lastIsArray = false;
				try {
					Integer index = Integer.parseInt(mPath[mPath.length-1]);
					lastIsArray = true;
					for(int i=0; i<mPath.length-2; i++){
						obj = obj.getJSONObject(mPath[i]);
					}
					JSONArray array = obj.getJSONArray(mPath[mPath.length-2]);
					out.println(path + ":\t" + array.get(index).toString());
				} catch (NumberFormatException ex) {
					//No big deal, just not an array index
				}
				
				if (!lastIsArray) {
					for(int i=0; i<mPath.length-1; i++){
						obj = obj.getJSONObject(mPath[i]);
					}

					out.println(path + ":\t" + obj.get(mPath[mPath.length-1]).toString());
				}
				
				
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static Double mean(List<Double> vals) {
		Double sum = 0d;
		Double count = 0d;
		for(Double val : vals) {
			if (val != null && (!Double.isNaN(val))) {
				count++;
				sum += val;
			}
		}
		if (count > 0)
			return sum / count;
		else 
			return Double.NaN;
	}

	public static void performQAIndicatorsByMonth(List<String> paths, PrintStream out, AnalysisTypeConverter converter) {
		
		Map<String, List<String>> groupedByMonth = new HashMap<String, List<String>>();
	    SimpleDateFormat sd = new SimpleDateFormat("MMMM");

		//GO through all input paths and group by calendar month before performing analysis
		for(String path : paths) {
			try {
				File manifestFile = new File(path + "/sampleManifest.txt");
				Map<String, String> manifest = readManifest(manifestFile);
				Date analysisDate = new Date( Long.parseLong(manifest.get("analysis.start.time")));
				String month = sd.format(analysisDate);
				
				List<String> pathsInMonth = groupedByMonth.get(month);
				if (pathsInMonth == null) {
					pathsInMonth = new ArrayList<String>();
					groupedByMonth.put(month, pathsInMonth);
				}
				pathsInMonth.add(path);
			}
			catch (Exception ex) {
				
			}
		}
		
		
		//Now all paths are grouped by month, so iterate the map and perform QA for each month separately
		for(String month : groupedByMonth.keySet()) {
			List<String> monthPaths = groupedByMonth.get(month);
			
			out.println("\n\n Month: " + month);
			performQAIndicators(monthPaths, out, converter);
		}
	}
	
	/**
	 * Average Sequencing Depth of Target Regions
X	Mean coverage
X	Percent Bases with Base Quality > 30
X	Percent Variants that are Indels
X	Percent Targeted Bases with > 15x Coverage	
X	Percent of Variants not in 1000 Genomes
X	Variants per Mega Base
X	Mean total variant count
+	Number of exons < 15x mean coverage	

Number of Sanger Requests Total (Average per Sample)
Number of Sanger Requests Confirmed (Average per Sample)
Number of Sanger Requests not Confirmed (Average per Sample)

	 * @param paths
	 * @param out
	 * @param converter
	 */
	public static void performQAIndicators(List<String> paths, PrintStream out, AnalysisTypeConverter converter) {
		Map<String, QCInfoList> analysisMap = new HashMap<String, QCInfoList>(); //Mapping from analysis types to groups of qc metrics
		Date bugFixDate = DateParser.parse("2014-08-01");
		for(String path : paths) {
			try {
				File manifestFile = new File(path + "/sampleManifest.txt");
				Map<String, String> manifest = readManifest(manifestFile);
				Date analysisDate = new Date( Long.parseLong(manifest.get("analysis.start.time")));
				String analysisType = analysisTypeFromManifest(manifestFile).replace(" (v. 1.0)", "");
				if (converter != null) {
					analysisType = converter.convert(analysisType);
				}
				
				//Skip everything except aort's and both types of mitos. 
				if (!(analysisType.toLowerCase().contains("aort") 
						|| analysisType.toLowerCase().contains("mito")
						|| analysisType.toLowerCase().contains("noonan")
						|| analysisType.toLowerCase().contains("gicapan")
						|| analysisType.toLowerCase().contains("exome"))) {
					continue;
				}
				QCInfoList qcList = analysisMap.get(analysisType);
				if (qcList == null) {
					qcList = new QCInfoList();
					analysisMap.put(analysisType, qcList);
				}
				
				JSONObject obj = toJSONObj(path);
				JSONObject finalCov = obj.getJSONObject("final.coverage.metrics");
				JSONArray fracAbove = finalCov.getJSONArray("fraction.above.index");		

				Double mean = finalCov.getDouble("mean.coverage");
				double above0 = fracAbove.getDouble(2);
				
				Double above15 = fracAbove.getDouble(15);
				Double above200 = fracAbove.getDouble(200);
				qcList.add("mean.coverage", mean);
				//qcList.add("no.coverage", 100.0*(1.0-above0));
				
				if (analysisType.toLowerCase().contains("genome")) {
					qcList.add("frac.above.200", 100.0*above200);
				} else {
					qcList.add("frac.above.15", 100.0*above15);
				}
//				
				
				
				JSONObject rawBam = obj.getJSONObject("raw.bam.metrics");
				Double rawReadCount = rawBam.getDouble("total.reads");
				double basesRead = rawBam.getDouble("bases.read");
//				qcList.add("bases.above.q10", 100.0*rawBam.getDouble("bases.above.q10")/basesRead);
//				qcList.add("bases.above.q20", 100.0*rawBam.getDouble("bases.above.q20")/basesRead);
				
				double q30Bases = 100.0*rawBam.getDouble("bases.above.q30")/basesRead;
				
				//Fix known bug in q30 base calculation that was fixed around august 1 2014
				if (q30Bases < 10.0 && analysisDate.before(bugFixDate)) {
					q30Bases = q30Bases*10;
				}
				qcList.add("bases.above.q30", q30Bases);
				//qcList.add("reads.on.target", 100.0* (1.0-rawBam.getDouble("unmapped.reads")/rawReadCount));
				
				
				JSONObject finalBam = obj.getJSONObject("final.bam.metrics");
				Double finalReadCount = finalBam.getDouble("total.reads");
				double percentDups = (rawReadCount - finalReadCount)/rawReadCount;
				//qcList.add("percent.dups", 100.0*percentDups);
				
				Double insertionCount = Double.NaN;
				Double deletionCount = Double.NaN;
				Double snpCount = Double.NaN;
				Double varCount = Double.NaN;
				Double tstv = Double.NaN;
				Double knownVars = Double.NaN;
				Double novelFrac = Double.NaN;
				Double varsPerBaseCalled = Double.NaN;
				JSONObject variants = null;
				try {
					variants = obj.getJSONObject("variant.metrics");
					snpCount = variants.getDouble("total.snps");
					varCount = variants.getDouble("total.vars");
					insertionCount = variants.getDouble("total.insertions");
					deletionCount = variants.getDouble("total.deletions");
					tstv = variants.getDouble("total.tt.ratio");
					knownVars = variants.getDouble("total.known");
					
					//double percentInsertions = insertionCount / varCount * 100.0;
					//qcList.add("insertion.frac", percentInsertions);
					
					//double percentDeletions = deletionCount / varCount * 100.0;
					//qcList.add("deletion.frac", percentDeletions);

					double percentIndels = (insertionCount + deletionCount)/varCount * 100.0;
					qcList.add("indel.percent", percentIndels);
					
					//qcList.add("tstv.ratio", tstv);					
				}
				catch (JSONException e) {

				}
				try {
					qcList.add("total.variants", varCount);
					
					if (obj.has("capture.extent")) {
						long extent = obj.getLong("capture.extent");
						varsPerBaseCalled = varCount / (double)extent;
						qcList.add("vars.per.megabase", varsPerBaseCalled*1e6);
						//qcList.add("targeted.bases", new Double(extent));
					}
				}
				catch (JSONException e) {

				}
				
				//qcList.add("snp.frac", 100.0*snpCount/varCount);
				
				if (varCount > 0) {
					novelFrac = (1.0 - knownVars/varCount)*100.0;
					qcList.add("novel.vars.fraction", novelFrac);
				}
				else {
					qcList.add("novel.vars.fraction", 0.0);
				}
		
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
		
		for(String analType : analysisMap.keySet()) {
			QCInfoList qcItems = analysisMap.get(analType);
			List<String> sortedKeys = new ArrayList<String>();
			sortedKeys.addAll( qcItems.keys());
			Collections.sort(sortedKeys);
			int count = qcItems.getValsForMetric( sortedKeys.get(0) ).size();
		
			out.println("\n" + analType + "\n Samples found: " + count);
			
			
			for(String metric : sortedKeys) {
				if (metric.equals("total.variants")) out.print("Average Total Number of Variants");
				if (metric.equals("novel.vars.fraction")) out.print("Percent of Variants Considered Novel");
				if (metric.equals("snp.frac")) out.print("Percent Variants Considered Single Nucleotide Variants");
				if (metric.equals("indel.percent")) out.print("Percent Variants that are Indels");
				if (metric.equals("vars.per.megabase")) out.print("Variants per megabase");
				if (metric.equals("tstv.ratio")) out.print("Transition Transversion Ratio");
				if (metric.equals("mean.coverage")) out.print("Average Sequencing Depth of Target Regions");
				if (metric.equals("no.coverage")) out.print("Percent Targeted Bases with No Coverage");
				if (metric.equals("frac.below.10")) out.print("Percent Targeted Bases with < 10x Coverage");
				if (metric.equals("frac.above.10")) out.print("% Bases > 10 coverage");
				if (metric.equals("frac.above.15")) out.print("% Bases > 15 coverage");
				if (metric.equals("frac.above.200")) out.print("% Bases > 200 coverage");
				if (metric.equals("bases.above.q30")) out.print("Percent Bases with Base Quality > 30");
				if (metric.equals("frac.above.0")) out.print("Percent Targeted Bases Covered");
				if (metric.equals("targeted.bases")) out.print("Targeted Bases");
				//if (metric.equals("percent.dups")) out.print("% PCR duplicates removed");
				//if (metric.equals("reads.on.target")) out.print("Percent Sequence Reads Mapped to Reference");
				
				List<Double> vals = qcItems.getValsForMetric(metric);
				
				Double mean = mean(vals);
				if (Math.abs(mean)<0.01) {
					out.println(":\t" + smallFormatter.format(mean));
				} else {
					out.println(":\t" + formatter.format(mean));	
				}
				
			}
		}
	}

	/**
	 * Create a table of data from the given input paths that is formatted just like the prereview data table
	 * in NGS.Web, for easy importing of data. It should look something like the following:
	 * 
	 * @param paths
	 * @param out
	 */	
	public static void performQCList(List<String> paths, PrintStream out, AnalysisTypeConverter converter) {
		Map<String, QCInfoList> analysisMap = new HashMap<String, QCInfoList>(); //Mapping from analysis types to groups of qc metrics
		
		for(String path : paths) {
			try {
				File manifestFile = new File(path + "/sampleManifest.txt");
				Map<String, String> manifest = readManifest(manifestFile);
				String analysisType = analysisTypeFromManifest(manifestFile).replace(" (v. 1.0)", "");
				if (converter != null) {
					analysisType = converter.convert(analysisType);
				}
				QCInfoList qcList = analysisMap.get(analysisType);
				if (qcList == null) {
					qcList = new QCInfoList();
					analysisMap.put(analysisType, qcList);
				}
				
				
				JSONObject obj = toJSONObj(path);
				JSONObject finalCov = obj.getJSONObject("final.coverage.metrics");
				JSONArray fracAbove = finalCov.getJSONArray("fraction.above.index");		

				Double mean = finalCov.getDouble("mean.coverage");
				Double above0 = fracAbove.getDouble(0);
				Double above20 = fracAbove.getDouble(20);
				Double above50 = fracAbove.getDouble(50);
				qcList.add("mean.coverage", mean);
				qcList.add("frac.above.0", above0);
				qcList.add("frac.above.20", above20);
				qcList.add("frac.above.50", above50);
				
				
				
				JSONObject rawBam = obj.getJSONObject("raw.bam.metrics");
				Double rawReadCount = rawBam.getDouble("total.reads");
				qcList.add("total.reads", rawReadCount);
				double basesRead = rawBam.getDouble("bases.read");
				qcList.add("bases.above.q10", rawBam.getDouble("bases.above.q10")/basesRead);
				qcList.add("bases.above.q20", rawBam.getDouble("bases.above.q20")/basesRead);
				qcList.add("bases.above.q30", rawBam.getDouble("bases.above.q30")/basesRead);
				qcList.add("unmapped.reads", rawBam.getDouble("unmapped.reads")/rawReadCount);
				
				
				JSONObject finalBam = obj.getJSONObject("final.bam.metrics");
				Double finalReadCount = finalBam.getDouble("total.reads");
				double percentDups = (rawReadCount - finalReadCount)/rawReadCount;
				qcList.add("percent.dups", percentDups);
				
				int indelCount = -1;
				Double snpCount = Double.NaN;
				Double varCount = Double.NaN;
				Double tstv = Double.NaN;
				Double knownSnps = Double.NaN;
				Double novelFrac = Double.NaN;
				JSONObject variants = null;
				try {
					variants = obj.getJSONObject("variant.metrics");
				}
				catch (JSONException e) {

				}
				try {
					varCount = variants.getDouble("total.vars");
					qcList.add("total.vars", varCount);
				}
				catch (JSONException e) {

				}
				try {
					snpCount = variants.getDouble("total.snps");
					qcList.add("total.snps", snpCount);
				}
				catch (JSONException e) {

				}
				indelCount = (int)(varCount - snpCount);
//				try {
//					knownSnps = variants.getDouble("total.known");
//					qcList.add("known.snps", knownSnps);
//				}
//				catch (JSONException e) {
//
//				}
				
				if (snpCount > 0) {
					novelFrac = 1.0 - knownSnps/snpCount;
				}
				
				try {
					tstv = variants.getDouble("total.tt.ratio");
					qcList.add("total.tt.ratio", tstv);
				}
				catch (JSONException e) {

				}
				
				try {
					tstv = variants.getDouble("known.tt");
					qcList.add("known.tt", tstv);
				}
				catch (JSONException e) {

				}
				
				try {
					tstv = variants.getDouble("novel.tt");
					qcList.add("novel.tt", tstv);
				}
				catch (JSONException e) {

				}

				
				
				//NoCalls stuff...
				try {
					JSONObject nocalls = obj.getJSONObject("nocalls");
					int count = nocalls.getInt("interval.count");
					int extent = nocalls.getInt("no.call.extent");
					qcList.add("interval.count", new Double(count));
					qcList.add("no.call.extent", new Double(extent));
				} catch(Exception ex) {
					
				}
				
				//System.out.println(toSampleName(path) + "\t" + rawReadCount + "\t" + mean + "\t" + formatter.format(percentDups) + "\t" + above15 + "\t" + snpCount + "\t" + indelCount + "\t" + formatter.format(novelFrac) + "\t" + formatter.format(tstv));

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
		
		for(String analType : analysisMap.keySet()) {
			QCInfoList qcItems = analysisMap.get(analType);
			List<String> sortedKeys = new ArrayList<String>();
			sortedKeys.addAll( qcItems.keys());
			Collections.sort(sortedKeys);
			int count = qcItems.getValsForMetric( sortedKeys.get(0) ).size();
		
			out.println("\nAnalysis type: " + analType + " samples found: " + count);
			if (count < 10) {
				out.println("Not enough samples, skipping " + analType);
				continue;
			}
			
			for(String metric : sortedKeys) {
				out.print(analType + "\t");
				
				if (metric.equals("total.snps")) out.print("Total SNPs");				
				if (metric.equals("total.vars")) out.print("Total variants");
				if (metric.equals("known.snps")) out.print("Known SNPs");
				if (metric.equals("total.tt.ratio")) out.print("Overall Ti/Tv");
				if (metric.equals("known.tt")) out.print("Known Ti/Tv");
				if (metric.equals("novel.tt")) out.print("Novel Ti/Tv");
				if (metric.equals("mean.coverage")) out.print("Mean coverage");
				if (metric.equals("total.reads")) out.print("Total reads");
				if (metric.equals("bases.above.q30")) out.print("Bases above Q30");
				if (metric.equals("bases.above.q20")) out.print("Bases above Q20");
				if (metric.equals("bases.above.q10")) out.print("Bases above Q10");
				if (metric.equals("frac.above.0")) out.print("Fraction above 0X");
				if (metric.equals("frac.above.20")) out.print("Fraction above 20X");
				if (metric.equals("frac.above.50")) out.print("Fraction above 50X");
				if (metric.equals("percent.dups")) out.print("PCR dups. removed");
				if (metric.equals("unmapped.reads")) out.print("Unmapped reads");
				if (metric.equals("interval.count")) out.print("Number of no-call regions");
				if (metric.equals("no.call.extent")) out.print("Number no-call bases");
				
				out.print("\t" + metric + "\t");
				List<Double> vals = qcItems.getValsForMetric(metric);
				String formattedList = formatQCListVals(vals);
				out.print(formattedList);
				
				if (metric.equals("total.snps")
						|| metric.equals("total.vars")
						|| metric.equals("known.tt")
						|| metric.equals("novel.tt")
						|| metric.equals("total.tt.ratio")) {
					out.print("Variant metrics\tvariant.metrics");				
				}
				if (metric.equals("mean.coverage")) {
					out.print("Coverage\tfinal.coverage.metrics");
				}
				if (metric.equals("total.reads")) {
					out.print("Coverage\traw.bam.metrics");
				}
				if (metric.equals("percent.dups")) {
					out.print("BAM Metrics\tNULL");
				}
				if (metric.equals("unmapped.reads")) {
					out.print("BAM Metrics\tNULL");
				}
				if (metric.startsWith("no.call") || metric.startsWith("interval.count")) {
					out.print("BAM Metrics\tnocalls");
				}
				if (metric.startsWith("bases.above")) {
					out.print("BAM Metrics\traw.bam.metrics");
				}
				if (metric.startsWith("frac.above")) {
					out.print("Coverage\tNULL");
				}
				out.println();
			}
		}
	}
	
	//Nicely format a long list of qc values
	private static String formatQCListVals(List<Double> vals) {
		DecimalFormat formatter = new DecimalFormat("0.0##");
		if (vals.size() < 3) {
			return "Not enough data (" + vals.size() + " elements)";
		}
		Collections.sort(vals);
		Double min = vals.get(0);
		Double max = vals.get( vals.size() - 1 );
		Histogram histo = new Histogram(min, max, vals.size());
		for(Double x : vals) {
			histo.addValue(x);
		}
		
		return formatter.format(histo.lowerHPD(0.025)) + "\t" + formatter.format(histo.lowerHPD(0.05)) + "\t" + formatter.format(histo.upperHPD(0.05)) + "\t" + formatter.format(histo.upperHPD(0.025)) + "\t"; 
	}
	
	private static List<String> compareForKey(VariantPool poolA, VariantPool poolB, String annoKey, PrintStream out) {
		int missingInB =0;
		List<String> misMatches = new ArrayList<String>();
		int perfectMatches = 0;
		int notAnnotated = 0;
		for(String contig : poolA.getContigs()) {
			
			for(VariantRec aVar : poolA.getVariantsForContig(contig)) {
				VariantRec bVar = poolB.findRecord(aVar.getContig(), aVar.getStart(), aVar.getRef(), aVar.getAlt());
				if (bVar == null) {
					missingInB++;
				} else {
					String valA = aVar.getPropertyOrAnnotation(annoKey);
					String valB = bVar.getPropertyOrAnnotation(annoKey);
					if ((valA == null && valB == null) || (valA.equals("-") && valB.equals("-"))) {
						notAnnotated++;
						continue;
					}
					if (valA.equals(valB)) {
						perfectMatches++;
					} else {
						misMatches.add(aVar.toString() + "\t" + valA + " != " + valB);
					}
				}
			}
		}
		
		
		
		
		
		if (misMatches.size()>0) {
			out.println("*********************\n Found " + misMatches.size() + " mismatching annotations for key: " + annoKey + "\n First 10 mismatches:");
			for(int i=0; i<Math.min(10, misMatches.size()); i++) {
				out.println("\t" + misMatches.get(i));
			}
			out.println("*****************");
		} else {
			out.println("No discordant annotations for key " + annoKey);
		}
		
		return misMatches;
	}
	
	private static void performComparison(List<String> paths, PrintStream out) throws IOException {
		if (paths.size() != 2) {
			out.println("Please enter two directories to compare.");
			return;
		}
	
		
		
		try {
			
			ReviewDirectory dirA = new ReviewDirectory(paths.get(0));
			ReviewDirectory dirB = new ReviewDirectory(paths.get(1));
			
			String sampleAId = dirA.getSampleName();
			String sampleBId = dirB.getSampleName();
			out.println("Comparing " + sampleAId + " to " + sampleBId);
			
			JSONObject qcA = toJSONObj(paths.get(0));
			JSONObject qcB = toJSONObj(paths.get(1));
			
			out.println("Metric\t" + sampleAId + "\t" + sampleBId);
			
			JSONObject finalCovA = qcA.getJSONObject("final.coverage.metrics");
			JSONArray fracAboveA = finalCovA.getJSONArray("fraction.above.index");		

			JSONObject finalCovB = qcB.getJSONObject("final.coverage.metrics");
			JSONArray fracAboveB = finalCovB.getJSONArray("fraction.above.index");	
			
			Double meanA = finalCovA.getDouble("mean.coverage");
			String above15A = formatter.format(fracAboveA.getDouble(15));
			String above25A = formatter.format(fracAboveA.getDouble(25));
			String above50A = formatter.format(fracAboveA.getDouble(50));

			Double meanB = finalCovB.getDouble("mean.coverage");
			String above15B = formatter.format(fracAboveB.getDouble(15));
			String above25B = formatter.format(fracAboveB.getDouble(25));
			String above50B = formatter.format(fracAboveB.getDouble(50));
			
			out.println("Mean coverage:\t" + meanA + "\t" + meanB);
			out.println("       % > 15:\t" + above15A + "\t" + above15B);
			out.println("       % > 25:\t" + above25A + "\t" + above25B);
			out.println("       % > 50:\t" + above50A + "\t" + above50B);
			
			VariantPool vcfVarsA = dirA.getVariantsFromVCF();
			VariantPool vcfVarsB = dirB.getVariantsFromVCF();
			
			out.println("VCF Variants:");
			out.println("\tTotal variants\tSNPs\tIndels\tHets");
			out.println(dirA.getSampleName() +"\t" + vcfVarsA.size() + '\t' + vcfVarsA.countSNPs() + "\t" + (vcfVarsA.countInsertions()+vcfVarsA.countDeletions()) + "\t" + ("" + 100.0*vcfVarsA.countHeteros()/vcfVarsA.size()).substring(0, 5));
			out.println(dirB.getSampleName() +"\t" + vcfVarsB.size() + '\t' + vcfVarsB.countSNPs() + "\t" + (vcfVarsB.countInsertions()+vcfVarsA.countDeletions()) + "\t" + ("" + 100.0*vcfVarsA.countHeteros()/vcfVarsB.size()).substring(0, 5));
			
			if (vcfVarsA.size() != vcfVarsB.size()) {
				out.println("****************************************");
				out.println("Differing numbers of variants identified in VCF files!");
				out.println("****************************************\n");
			}
			
			VariantPool csvVarsA = dirA.getVariantsFromCSV();
			VariantPool csvVarsB = dirB.getVariantsFromCSV();
			
			if (csvVarsA.size() != vcfVarsA.size()) {
				out.println("******************\n WARNING: VCF variant count (" + vcfVarsA.size() + ") not equal to CSV variant count (" + csvVarsA.size() + ") in sample " + sampleAId + "\n*****************************");
			}
			if (csvVarsB.size() != vcfVarsB.size()) {
				out.println("******************\n WARNING: VCF variant count (" + vcfVarsB.size() + ") not equal to CSV variant count  (" + csvVarsB.size() + ") in sample " + sampleBId);
			}
			
			
			compareForKey(csvVarsA, csvVarsB, VariantRec.POP_FREQUENCY, out);
			compareForKey(csvVarsA, csvVarsB, VariantRec.EXOMES_FREQ, out);
			compareForKey(csvVarsA, csvVarsB, VariantRec.GENE_NAME, out);
			compareForKey(csvVarsA, csvVarsB, VariantRec.HGMD_HIT, out);
			compareForKey(csvVarsA, csvVarsB, VariantRec.CDOT, out);
			
			
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ManifestParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}

	private static void performTableize(List<String> paths, PrintStream out) {
		System.out.println("Sample	Total reads	Mean coverage	%Duplicates	%Bases > 15	SNPs	Indels	Fraction novel SNPs	Ts / Tv");
		for(String path : paths) {
			try {
				JSONObject obj = toJSONObj(path);
				JSONObject finalCov = obj.getJSONObject("final.coverage.metrics");
				JSONArray fracAbove = finalCov.getJSONArray("fraction.above.index");		

				Double mean = finalCov.getDouble("mean.coverage");
				String above15 = formatter.format(fracAbove.getDouble(15));

				JSONObject rawBam = obj.getJSONObject("raw.bam.metrics");
				Double rawReadCount = rawBam.getDouble("total.reads");

				JSONObject finalBam = obj.getJSONObject("final.bam.metrics");
				Double finalReadCount = finalBam.getDouble("total.reads");
				double percentDups = (rawReadCount - finalReadCount)/rawReadCount;
				
				int indelCount = -1;
				Double snpCount = Double.NaN;
				Double varCount = Double.NaN;
				Double tstv = Double.NaN;
				Double knownSnps = Double.NaN;
				Double novelFrac = Double.NaN;
				JSONObject variants = null;
				try {
					variants = obj.getJSONObject("variant.metrics");
				}
				catch (JSONException e) {

				}
				try {
					varCount = variants.getDouble("total.vars");
				}
				catch (JSONException e) {

				}
				try {
					snpCount = variants.getDouble("total.snps");
				}
				catch (JSONException e) {

				}
				indelCount = (int)(varCount - snpCount);
				try {
					knownSnps = variants.getDouble("total.known");
				}
				catch (JSONException e) {

				}
				novelFrac = 1.0 - knownSnps/snpCount;
				try {
					tstv = variants.getDouble("total.tt.ratio");
				}
				catch (JSONException e) {

				}

				System.out.println(toSampleName(path) + "\t" + rawReadCount + "\t" + mean + "\t" + formatter.format(percentDups) + "\t" + above15 + "\t" + snpCount + "\t" + indelCount + "\t" + formatter.format(novelFrac) + "\t" + formatter.format(tstv));

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	}
	
	private static void performCovSummary(List<String> paths, PrintStream out) {
		TextTable data = new TextTable(new String[]{"Mean", ">5", ">10", ">20", ">50"});
		for(String path : paths) {
			try {
				JSONObject obj = toJSONObj(path);
				if (obj.has("final.coverage.metrics")) {
					JSONObject finalCov = obj.getJSONObject("final.coverage.metrics");
					JSONArray fracAbove = finalCov.getJSONArray("fraction.above.index");
					Double mean = finalCov.getDouble("mean.coverage");
					String[] covs = new String[5];
					covs[0] = formatter.format(mean);
					covs[1] = formatter.format(fracAbove.getDouble(5));
					covs[2] = formatter.format(fracAbove.getDouble(10));
					covs[3] = formatter.format(fracAbove.getDouble(20));
					covs[4] = formatter.format(fracAbove.getDouble(50));
					data.addColumn(toSampleName(path), covs);
				}
				else {
					data.addColumn(toSampleName(path), new String[]{"?","?","?","?","?"});
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		out.println(data.toString());
	}
	
	private static void performCovGraph(List<String> paths, PrintStream out) {
		TextTable data = new TextTable(new String[]{"Mean", ">5", ">10", ">20", ">50"});
		List<JSONArray> covs = new ArrayList<JSONArray>();
		for(String path : paths) {
			try {
				JSONObject obj = toJSONObj(path);
				if (obj.has("final.coverage.metrics")) {
					JSONObject finalCov = obj.getJSONObject("final.coverage.metrics");
					JSONArray fracAbove = finalCov.getJSONArray("fraction.above.index");
					covs.add(fracAbove);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		for(int i=0; i<covs.get(0).length(); i++) {
			for(JSONArray arr : covs) {
				try {
					out.print(arr.getDouble(i) + "\t");
				}
				catch (JSONException jex) {
					out.print("?" + "\t");
				}
			}
			out.println();
		}
		 
	}

	private static String toSampleName(String path) {
		File file = new File(path);
		if (file.isDirectory()) {
			File manifestFile = new File(file.getPath() + "/sampleManifest.txt");
			if (manifestFile.exists()) {
				try {
					return sampleIDFromManifest(manifestFile);
				} catch (IOException e) {
					String shortPath = path.split("/")[0];
					return shortPath.replace(".reviewdir", "");			
				}
			}
		}
		
		String shortPath = path.split("/")[0];
		return shortPath.replace(".reviewdir", "");
	}

	public static Map<String, String> readManifest(File manifestFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(manifestFile));
		Map<String, String> pairs = new HashMap<String, String>();
		String line = reader.readLine();
		while(line != null) {
			String[] toks = line.split("=");
			if (toks.length==2) {
				pairs.put(toks[0], toks[1]);
			}
			
			line = reader.readLine();
		}
		reader.close();
		return pairs;
	}
	
	private static String analysisTypeFromManifest(File manifestFile) throws IOException {
		Map<String, String> vals = readManifest(manifestFile);
		String analysisType = vals.get("analysis.type");
		if (analysisType == null) 
			analysisType = "?";
		return analysisType;
	}
	
	private static String sampleIDFromManifest(File manifestFile) throws IOException {
		Map<String, String> vals = readManifest(manifestFile);
		String sampleId = vals.get("sample.name");
		String analysisType = vals.get("analysis.type");
		if (sampleId == null)
			sampleId = "?";
		if (analysisType == null) 
			analysisType = "?";
		return sampleId + ", " + analysisType;
	}

	private static void performVarSummary(List<String> paths, PrintStream out) {
		TextTable data = new TextTable(new String[]{"Total.vars", "Het%", "SNPS",  "% Novel", "Ti/Tv ratio",  "Novel Ti/Tv", "Vars / Base"});
		for(String path : paths) {
			try {
				JSONObject obj = toJSONObj(path);
				String[] col = new String[7];
				if (obj.has("variant.metrics")) {
					JSONObject vars = obj.getJSONObject("variant.metrics");
					col[0] = safeInt(vars, "total.vars");
					
					try {
						int tot = vars.getInt("total.vars");
						int hets = vars.getInt("total.het.vars"); 
						double hetFrac = (double) hets / (double) tot;
						col[1] = formatter.format(100.0*hetFrac);
					}
					catch (JSONException ex) {
						col[1] = "?";
					}
					
					
					col[2] = safeInt(vars, "total.snps");

					Double totVars = vars.getDouble("total.vars");
					Double knowns = vars.getDouble("total.known");
					if (totVars != null && knowns != null && totVars > 0) {
						Double novelFrac = 1.0 - knowns / totVars;
						col[3] = "" + formatter.format(novelFrac);
					}
					else {
						col[3] = "0";
					}
					col[4] = safeDouble(vars, "total.tt.ratio");
					col[5] = safeDouble(vars, "novel.tt");

					//output.println("Total novels: " + safeDouble(varMetrics, " "));

					if (obj.has("capture.extent")) {
						long extent = obj.getLong("capture.extent");
						double varsPerBaseCalled = totVars / (double)extent;
						col[6] = smallFormatter.format(varsPerBaseCalled);	
					}
					else {
						col[6] = "?";
					}

					data.addColumn(toSampleName(path), col);
				}
				else {
					data.addColumn(toSampleName(path), new String[]{"?","?","?","?","?","?","?"});
				}
			} catch (IOException e) {
				data.addColumn(toSampleName(path), new String[]{"?","?","?","?","?","?"});
				e.printStackTrace();
			} catch (JSONException e) {
				data.addColumn(toSampleName(path), new String[]{"?","?","?","?","?","?"});
				e.printStackTrace();
			}
		}
		
		out.println(data.toString());
	}
	
	
	static class QCInfoList {
		Map<String, List<Double>> items = new HashMap<String, List<Double>>();
		
		public void add(String metric, Double val) {
			List<Double> itemList = items.get(metric);
			if (itemList == null) {
				itemList = new ArrayList<Double>(8);
				items.put(metric, itemList);
			}
			itemList.add(val);
		}
		
		public List<Double> getValsForMetric(String metric) {
			return items.get(metric);
		}
		
		public Collection<String> keys() {
			return items.keySet();
		}
		
	}
}
