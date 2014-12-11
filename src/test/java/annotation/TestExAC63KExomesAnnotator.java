package annotation;

import java.io.File;

import junit.framework.TestCase;
import org.junit.Assert;

import operator.variant.ExAC63KExomesAnnotator;

import pipeline.Pipeline;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

// Test the ExAC 63k exomes annotator, using the first 1000 lines of the ExAC 63k VCF


public class TestExAC63KExomesAnnotator extends TestCase {

	File inputVCFTemplate = new File("src/test/java/annotation/testExAC63k_VCF.xml");
	File inputCSVTemplate = new File("src/test/java/annotation/testExAC63k_CSV.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");

	public void testExAC() {
		// Test VCF
		try {
			//Run pipeline on "src/test/java/testvcfs/myeloid.vcf"
			Pipeline pplVCF = new Pipeline(inputVCFTemplate, propertiesFile.getAbsolutePath());			
			pplVCF.setProperty("63k.db.path", "src/test/java/testvcfs/ExAC.r0.2.sites.vep_1000lines.vcf.gz");
			pplVCF.initializePipeline();
			pplVCF.stopAllLogging();
			pplVCF.execute();

			//Get variants input into annotator from XML element
			ExAC63KExomesAnnotator annotator = (ExAC63KExomesAnnotator)pplVCF.getObjectHandler().getObjectForLabel("GeneAnnotate");
			VariantPool vars = annotator.getVariants();
			Assert.assertEquals(90, vars.size());
				
		} catch (Exception ex) {
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	
		//Test CSV
		try {
			//Run pipeline on "src/test/java/annotation/gatksingle_annotated.csv"
			Pipeline pplCSV = new Pipeline(inputCSVTemplate, propertiesFile.getAbsolutePath());			
			pplCSV.setProperty("63k.db.path", "src/test/java/testvcfs/ExAC.r0.2.sites.vep_1000lines.vcf.gz");
			pplCSV.initializePipeline();
			pplCSV.stopAllLogging();
			pplCSV.execute();

			//Get variants input into annotator from XML element
			ExAC63KExomesAnnotator annotator = (ExAC63KExomesAnnotator)pplCSV.getObjectHandler().getObjectForLabel("GeneAnnotate");
			VariantPool vars = annotator.getVariants();
			
			//Check all variants are in the pool
			Assert.assertEquals(889, vars.size());
			
		} catch (Exception ex) {
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
}

