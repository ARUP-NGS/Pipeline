package qc;

import java.io.File;
import junit.framework.TestCase;
import operator.qc.QCtoJSON;
import org.junit.Assert;
import pipeline.Pipeline;
import buffer.ArupBEDFile;
import buffer.CSVFile;
import json.JSONArray;
import json.JSONObject;


/**
 * QCtoJSON noCall regions unit test
 * 
 * This test validates the noCall regions section of the QCtoJSON operator "QCtoJSON.java".
 * requires snpEff and assumes a sym link "snpEffDirPath" in the run dir
 * currently requires two test pipeline templates and two test noCall csv files
 *   
 * @author Jacob Durtschi
 *
 */

public class TestQCtoJSONnoCall extends TestCase {

	public static final String SNPEFF_DIR = "snpeff.dir";
	
	File inputFile = new File("src/test/java/qc/testQCtoJSONnoCall.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");
	CSVFile nocallCSV = new CSVFile(new File("src/test/java/qc/testQCtoJSONnoCall.txt"));
	ArupBEDFile nocallArupBed = new ArupBEDFile(new File("src/test/java/qc/testQCtoJSONnoCall.arup.bed"));
	File inputFile2 = new File("src/test/java/qc/testQCtoJSONnoCall2.xml");
	CSVFile nocallCSV2 = new CSVFile(new File("src/test/java/qc/testQCtoJSONnoCall2.txt"));

	private Pipeline preparePipeline(File inFile) {
		Pipeline ppl = null;
		String pipelinePropsFile = System.getProperty("pipelineProps");
		if (pipelinePropsFile != null) { // We are given a properties file.
			ppl = new Pipeline(inFile, pipelinePropsFile);
			String snpEffDirPath = (String) ppl.getProperty(SNPEFF_DIR);
			if (snpEffDirPath == null) {
				throw new IllegalArgumentException("No path to snpEff found, please specify " + SNPEFF_DIR);
			}
			if (!(new File(snpEffDirPath).exists())) {
				throw new IllegalArgumentException("No file found at snpEff path : " + SNPEFF_DIR);
			}

		} else { //User didn't provide pipeline_properties.xml file.
			File snpEffDir = new File("snpEffDirLink");
			if (!snpEffDir.exists()) {
				throw new IllegalStateException(
						"No snpEffDirLink link found. Can't run this test since you don't have SnpEff installed. You must create a link called 'snpEffDirLink' in the main Pipeline directory to the SnpEff directory to use this.");
			}
			ppl = new Pipeline(inFile, propertiesFile.getAbsolutePath());
			ppl.setProperty("snpeff.dir", snpEffDir.getAbsolutePath());
		}	
		return ppl;
	}	
	public void testQCtoJSONnoCall() {

		try {
			//prepare 2 pipeline objects for two different tests
			Pipeline ppl = preparePipeline(inputFile.getAbsoluteFile());
			ppl.initializePipeline();
			ppl.stopAllLogging();
			//ppl.execute();  do not actually execute pipeline, we'll only test the noCall regions piece for now.
			//grab the QCtoJSON object for testing
			QCtoJSON qc = (QCtoJSON)ppl.getObjectHandler().getObjectForLabel("QCtoJSON");
			//perform the nocall snpeff piece of QCtoJSON operator
			//use ARUP bed file for this first one
			nocallArupBed.buildIntervalsMap(true);
			JSONObject nocalls = new JSONObject(qc.noCallsToJSONWithSnpEff(nocallCSV, nocallArupBed));


			//now the second pipeline object...
			Pipeline ppl2 = preparePipeline(inputFile2.getAbsoluteFile());
			ppl2.initializePipeline();
			ppl2.stopAllLogging();
			//ppl.execute();  do not actually execute pipeline, we'll only test the noCall regions piece for now.
			//grab the QCtoJSON object for testing
			QCtoJSON qc2 = (QCtoJSON)ppl2.getObjectHandler().getObjectForLabel("QCtoJSON");
			//perform the nocall snpeff piece of QCtoJSON operator
			//this time don't use ARUP bed file
			Boolean useArupBED = false;
			JSONObject nocalls2 = new JSONObject(qc2.noCallsToJSONWithSnpEff(nocallCSV2, useArupBED));

			//Test first instance
			//Test annotations 
			JSONArray regions = nocalls.getJSONArray("regions");

			//verify that count (number of regions) and extent (number of bases) have been properly counted
			Assert.assertTrue(nocalls.getInt("interval.count") == 14); 
			Assert.assertTrue(nocalls.getInt("no.call.extent") == 75609); 

			//1	2160000	2160070	LOW_COVERAGE	0.0	#Upstream
			Assert.assertTrue(regions.getJSONObject(0).getString("chr").equals("1"));
			Assert.assertTrue(regions.getJSONObject(0).getInt("start") == 2160000);
			Assert.assertTrue(regions.getJSONObject(0).getInt("end") == 2160070);
			Assert.assertTrue(regions.getJSONObject(0).getInt("size") == 70);
			Assert.assertTrue(regions.getJSONObject(0).getString("reason").equals("No_coverage"));
			Assert.assertTrue(regions.getJSONObject(0).getDouble("mean.coverage") == 0.0);
			Assert.assertTrue(regions.getJSONObject(0).getString("gene").equals(""));

			//1	2160000	2160133	LOW_COVERAGE	0.0 # Upstream butting against exon 1
			Assert.assertTrue(regions.getJSONObject(1).getString("chr").equals("1"));
			Assert.assertTrue(regions.getJSONObject(1).getInt("start") == 2160000);
			Assert.assertTrue(regions.getJSONObject(1).getInt("end") == 2160133);
			Assert.assertTrue(regions.getJSONObject(1).getInt("size") == 133);
			Assert.assertTrue(regions.getJSONObject(1).getString("reason").equals("No_coverage"));
			Assert.assertTrue(regions.getJSONObject(1).getDouble("mean.coverage") == 0.0);
			Assert.assertTrue(regions.getJSONObject(1).getString("gene").equals(""));
			
			//1	2160110	2160170	NO_COVERAGE	0.0 # Upstream into 5 utr
			Assert.assertTrue(regions.getJSONObject(2).getString("chr").equals("1"));
			Assert.assertTrue(regions.getJSONObject(2).getInt("start") == 2160110);
			Assert.assertTrue(regions.getJSONObject(2).getInt("end") == 2160170);
			Assert.assertTrue(regions.getJSONObject(2).getInt("size") == 60);
			Assert.assertTrue(regions.getJSONObject(2).getString("reason").equals("No_coverage"));
			Assert.assertTrue(regions.getJSONObject(2).getDouble("mean.coverage") == 0.0);
			Assert.assertTrue(regions.getJSONObject(2).getString("gene").equals("SKI (NM_003036.3.1) 5 UTR"));

			//1	2160110	2161400	NO_COVERAGE	0.0 # Upstream into 5 utr, exon 1, intron 1
			Assert.assertTrue(regions.getJSONObject(3).getString("chr").equals("1"));
			Assert.assertTrue(regions.getJSONObject(3).getInt("start") == 2160110);
			Assert.assertTrue(regions.getJSONObject(3).getInt("end") == 2161400);
			Assert.assertTrue(regions.getJSONObject(3).getInt("size") == 1290);
			Assert.assertTrue(regions.getJSONObject(3).getString("reason").equals("No_coverage"));
			Assert.assertTrue(regions.getJSONObject(3).getDouble("mean.coverage") == 0.0);
			Assert.assertTrue(regions.getJSONObject(3).getString("gene").equals("SKI (NM_003036.3.1) 5 UTR, Coding exon 1, Intron 1"));
			
			//1	2160133	2160140	LOW_COVERAGE	2.22 # start of 5 utr, (no coding)
			Assert.assertTrue(regions.getJSONObject(4).getString("chr").equals("1"));
			Assert.assertTrue(regions.getJSONObject(4).getInt("start") == 2160133);
			Assert.assertTrue(regions.getJSONObject(4).getInt("end") == 2160140);
			Assert.assertTrue(regions.getJSONObject(4).getInt("size") == 7);
			Assert.assertTrue(regions.getJSONObject(4).getString("reason").equals("Low_coverage"));
			Assert.assertTrue(regions.getJSONObject(4).getDouble("mean.coverage") == 2.22);
			Assert.assertTrue(regions.getJSONObject(4).getString("gene").equals("SKI (NM_003036.3.1) 5 UTR"));

			//1	2160150	2160220	LOW_COVERAGE	2.22 #	5 utr, into coding region exon 1
			Assert.assertTrue(regions.getJSONObject(5).getString("chr").equals("1"));
			Assert.assertTrue(regions.getJSONObject(5).getInt("start") == 2160150);
			Assert.assertTrue(regions.getJSONObject(5).getInt("end") == 2160220);
			Assert.assertTrue(regions.getJSONObject(5).getInt("size") == 70);
			Assert.assertTrue(regions.getJSONObject(5).getString("reason").equals("Low_coverage"));
			Assert.assertTrue(regions.getJSONObject(5).getDouble("mean.coverage") == 2.22);
			Assert.assertTrue(regions.getJSONObject(5).getString("gene").equals("SKI (NM_003036.3.1) 5 UTR, Coding exon 1"));
			
			
			//1	2160205	2160222	LOW_COVERAGE	5.32 # start of Coding exon 1, (no UTR)
			Assert.assertTrue(regions.getJSONObject(6).getString("chr").equals("1"));
			Assert.assertTrue(regions.getJSONObject(6).getInt("start") == 2160205);
			Assert.assertTrue(regions.getJSONObject(6).getInt("end") == 2160222);
			Assert.assertTrue(regions.getJSONObject(6).getInt("size") == 17);
			Assert.assertTrue(regions.getJSONObject(6).getString("reason").equals("Low_coverage"));
			Assert.assertTrue(regions.getJSONObject(6).getDouble("mean.coverage") == 5.32);
			Assert.assertTrue(regions.getJSONObject(6).getString("gene").equals("SKI (NM_003036.3.1) Coding exon 1"));
			
			//1	2161164	2161174	LOW_COVERAGE	11	# end of Coding exon 1, (no UTR)
			Assert.assertTrue(regions.getJSONObject(7).getString("chr").equals("1"));
			Assert.assertTrue(regions.getJSONObject(7).getInt("start") == 2161164);
			Assert.assertTrue(regions.getJSONObject(7).getInt("end") == 2161174);
			Assert.assertTrue(regions.getJSONObject(7).getInt("size") == 10);
			Assert.assertTrue(regions.getJSONObject(7).getString("reason").equals("Low_coverage"));
			Assert.assertTrue(regions.getJSONObject(7).getDouble("mean.coverage") == 11.0);
			Assert.assertTrue(regions.getJSONObject(7).getString("gene").equals("SKI (NM_003036.3.1) Coding exon 1"));
			
			//1	2161164	2161250	LOW_COVERAGE	11	# end of Coding exon 1 into intron 1, (no UTR)
			Assert.assertTrue(regions.getJSONObject(8).getString("chr").equals("1"));
			Assert.assertTrue(regions.getJSONObject(8).getInt("start") == 2161164);
			Assert.assertTrue(regions.getJSONObject(8).getInt("end") == 2161250);
			Assert.assertTrue(regions.getJSONObject(8).getInt("size") == 86);
			Assert.assertTrue(regions.getJSONObject(8).getString("reason").equals("Low_coverage"));
			Assert.assertTrue(regions.getJSONObject(8).getDouble("mean.coverage") == 11.0);
			Assert.assertTrue(regions.getJSONObject(8).getString("gene").equals("SKI (NM_003036.3.1) Coding exon 1, Intron 1"));
			
			//1	2161164	2234500	LOW_COVERAGE	11	# end of Coding exon 1 into coding exon 2, (no UTR)
			Assert.assertTrue(regions.getJSONObject(9).getString("chr").equals("1"));
			Assert.assertTrue(regions.getJSONObject(9).getInt("start") == 2161164);
			Assert.assertTrue(regions.getJSONObject(9).getInt("end") == 2234500);
			Assert.assertTrue(regions.getJSONObject(9).getInt("size") == 73336);
			Assert.assertTrue(regions.getJSONObject(9).getString("reason").equals("Low_coverage"));
			Assert.assertTrue(regions.getJSONObject(9).getDouble("mean.coverage") == 11.0);
			Assert.assertTrue(regions.getJSONObject(9).getString("gene").equals("SKI (NM_003036.3.1) Coding exon 1, Coding exon 2, Intron 1"));

			//1	2234000	2234010	LOW_COVERAGE	11	# intron 1 only
			Assert.assertTrue(regions.getJSONObject(10).getString("chr").equals("1"));
			Assert.assertTrue(regions.getJSONObject(10).getInt("start") == 2234000);
			Assert.assertTrue(regions.getJSONObject(10).getInt("end") == 2234010);
			Assert.assertTrue(regions.getJSONObject(10).getInt("size") == 10);
			Assert.assertTrue(regions.getJSONObject(10).getString("reason").equals("Low_coverage"));
			Assert.assertTrue(regions.getJSONObject(10).getDouble("mean.coverage") == 11.0);
			Assert.assertTrue(regions.getJSONObject(10).getString("gene").equals("SKI (NM_003036.3.1) Intron 1"));

			//1	2234020	2234030	LOW_COVERAGE	11	# intron 1 only but not covered by the arup bed file
			Assert.assertTrue(regions.getJSONObject(11).getString("chr").equals("1"));
			Assert.assertTrue(regions.getJSONObject(11).getInt("start") == 2234020);
			Assert.assertTrue(regions.getJSONObject(11).getInt("end") == 2234030);
			Assert.assertTrue(regions.getJSONObject(11).getInt("size") == 10);
			Assert.assertTrue(regions.getJSONObject(11).getString("reason").equals("Low_coverage"));
			Assert.assertTrue(regions.getJSONObject(11).getDouble("mean.coverage") == 11.0);
			Assert.assertTrue(regions.getJSONObject(11).getString("gene").equals(""));
			
			//1	2234000	2234500	LOW_COVERAGE	11	# intron 1 into exon 2
			Assert.assertTrue(regions.getJSONObject(12).getString("chr").equals("1"));
			Assert.assertTrue(regions.getJSONObject(12).getInt("start") == 2234000);
			Assert.assertTrue(regions.getJSONObject(12).getInt("end") == 2234500);
			Assert.assertTrue(regions.getJSONObject(12).getInt("size") == 500);
			Assert.assertTrue(regions.getJSONObject(12).getString("reason").equals("Low_coverage"));
			Assert.assertTrue(regions.getJSONObject(12).getDouble("mean.coverage") == 11.0);
			Assert.assertTrue(regions.getJSONObject(12).getString("gene").equals("SKI (NM_003036.3.1) Coding exon 2, Intron 1"));
			
			//1	2234723	2234733	LOW_COVERAGE	11	# in Coding exon 3
			Assert.assertTrue(regions.getJSONObject(13).getString("chr").equals("1"));
			Assert.assertTrue(regions.getJSONObject(13).getInt("start") == 2234723);
			Assert.assertTrue(regions.getJSONObject(13).getInt("end") == 2234733);
			Assert.assertTrue(regions.getJSONObject(13).getInt("size") == 10);
			Assert.assertTrue(regions.getJSONObject(13).getString("reason").equals("Low_coverage"));
			Assert.assertTrue(regions.getJSONObject(13).getDouble("mean.coverage") == 11.0);
			Assert.assertTrue(regions.getJSONObject(13).getString("gene").equals("SKI (NM_003036.3.1) Coding exon 3"));

			//Test second instance
			//Test annotations
			JSONArray regions2 = nocalls2.getJSONArray("regions");

			//verify that count (number of regions) and extent (number of bases) have been properly counted
			Assert.assertTrue(nocalls2.getInt("interval.count") == 3); 
			Assert.assertTrue(nocalls2.getInt("no.call.extent") == 220); 

			
			//Gene ACOT7 codes on reverse strand, 3 different transcripts
			//1	6420720 6420730 NO_COVERAGE	0.0	# 5utr on tr NM_181865.2.1 and intron 1 on other two
			Assert.assertTrue(regions2.getJSONObject(0).getString("chr").equals("1"));
			Assert.assertTrue(regions2.getJSONObject(0).getInt("start") == 6420720);
			Assert.assertTrue(regions2.getJSONObject(0).getInt("end") == 6420730);
			Assert.assertTrue(regions2.getJSONObject(0).getInt("size") == 10);
			Assert.assertTrue(regions2.getJSONObject(0).getString("reason").equals("No_coverage"));
			Assert.assertTrue(regions2.getJSONObject(0).getDouble("mean.coverage") == 0.0);
			Assert.assertTrue(regions2.getJSONObject(0).getString("gene").equals("ACOT7 (NM_181865.2.1) 5 UTR"));
			
			//1	6445600 6445610 NO_COVERAGE	0.0	# coding exon on tr NM_181864.2.1 and intron on second and upstream on third
			Assert.assertTrue(regions2.getJSONObject(1).getString("chr").equals("1"));
			Assert.assertTrue(regions2.getJSONObject(1).getInt("start") == 6445600);
			Assert.assertTrue(regions2.getJSONObject(1).getInt("end") == 6445610);
			Assert.assertTrue(regions2.getJSONObject(1).getInt("size") == 10);
			Assert.assertTrue(regions2.getJSONObject(1).getString("reason").equals("No_coverage"));
			Assert.assertTrue(regions2.getJSONObject(1).getDouble("mean.coverage") == 0.0);
			Assert.assertTrue(regions2.getJSONObject(1).getString("gene").equals("ACOT7 (NM_181864.2.1) Coding exon 1"));
			
			//1	6453300 6453500 NO_COVERAGE	0.0	# coding exon 1, 5utr and intron 1 on tr NM_007274.3.1 and upstream on other two
			Assert.assertTrue(regions2.getJSONObject(2).getString("chr").equals("1"));
			Assert.assertTrue(regions2.getJSONObject(2).getInt("start") == 6453300);
			Assert.assertTrue(regions2.getJSONObject(2).getInt("end") == 6453500);
			Assert.assertTrue(regions2.getJSONObject(2).getInt("size") == 200);
			Assert.assertTrue(regions2.getJSONObject(2).getString("reason").equals("No_coverage"));
			Assert.assertTrue(regions2.getJSONObject(2).getDouble("mean.coverage") == 0.0);
			Assert.assertTrue(regions2.getJSONObject(2).getString("gene").equals("ACOT7 (NM_007274.3.1) 5 UTR, Coding exon 1, Intron 1"));
			
		} catch (Exception ex){
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
}
