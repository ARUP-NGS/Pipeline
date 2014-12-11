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
			//Run pipeline on "src/test/java/testvcfs/gatksingle.vcf"
			Pipeline pplVCF = new Pipeline(inputVCFTemplate, propertiesFile.getAbsolutePath());			
			pplVCF.setProperty("63k.db.path", "src/test/java/testvcfs/ExAC.r0.2.sites.vep_1000lines.vcf.gz");
			pplVCF.initializePipeline();
			pplVCF.stopAllLogging();
			pplVCF.execute();

			//Get variants input into annotator from XML element
			ExAC63KExomesAnnotator annotator = (ExAC63KExomesAnnotator)pplVCF.getObjectHandler().getObjectForLabel("GeneAnnotate");
			VariantPool vars = annotator.getVariants();
			Assert.assertEquals(889, vars.size());
			
			VariantRec var = vars.findRecord("1", 17407, "G" , "A");
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_FREQ).equals(0.033));
			Double val= var.getProperty(VariantRec.EXOMES_63K_AC_HET);
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_AFR_FREQ).equals((double) 30/(double) 1000));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_AMR_FREQ).equals((double) 16/(double) 280));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_EAS_FREQ).equals((double) 29/(double) 412));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_FIN_FREQ).equals((double) 3/(double) 112));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_NFE_FREQ).equals((double) 269/(double) 2990));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_SAS_FREQ).equals((double) 54/(double) 394));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_OTH_FREQ).equals((double) 4/(double) 56));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_AC_HET).equals((double) 405 /(double) 13316));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_AFR_HET).equals((double) 30/(double) 1000));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_AMR_HET).equals((double) 16/(double) 280));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_EAS_HET).equals((double) 29/(double) 412));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_FIN_HET).equals((double) 3/(double) 112));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_NFE_HET).equals((double) 269/(double) 2990));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_SAS_HET).equals((double) 54/(double) 394));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_OTH_HET).equals((double) 4/(double) 56));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_AC_HOM).equals((double) 0/(double) 13316));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_AFR_HOM).equals((double) 0/(double) 1000));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_AMR_HOM).equals((double) 0/(double) 280));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_EAS_HOM).equals((double) 0/(double) 412));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_FIN_HOM).equals((double) 0/(double) 112));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_NFE_HOM).equals((double) 0/(double) 2990));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_SAS_HOM).equals((double) 0/(double) 394));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_OTH_HOM).equals((double) 0/(double) 56));
				
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

