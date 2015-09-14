package annotation;

import java.io.File;

import junit.framework.TestCase;
import operator.variant.ESP6500Annotator;

import org.junit.Assert;
import org.junit.Test;

import pipeline.Pipeline;
import util.vcfParser.VCFParser;
import buffer.variant.VariantRec;

public class testESP6500Annotator extends TestCase {
	File inputFile = new File("src/test/java/annotation/testESP6500.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml"); //do I need?
	ESP6500Annotator annotator;
	boolean thrown;

	public void setUp() {
		try {
			Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
			ppl.setProperty("esp.path", "C:\\Users\\23885\\Desktop\\pipeline_testing\\salt_home\\resources\\dbNSFP\\dbNSFP2.9_2015_09_11.tab.gz");

			ppl.initializePipeline();
			ppl.stopAllLogging();
			ppl.execute();
			annotator = (ESP6500Annotator) ppl.getObjectHandler().getObjectForLabel("ESPAnnotator");

		} catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}

	@Test
	public void testVanillaAnnotation() {
		System.out.println("hey");
		try {
			
			VariantRec var1 = new VariantRec("15", 43678542, 43678542, "CGTATATATAT", "CATATATAT");
			var1 = VCFParser.normalizeVariant(var1);
			System.out.println(var1);
			annotator.annotateVariant(var1);
			
			Assert.assertEquals(47.6671, var1.getProperty(VariantRec.EXOMES_FREQ), 0);
			Assert.assertEquals(25.28, var1.getProperty(VariantRec.EXOMES_FREQ_EA), 0);
			Assert.assertEquals(30.6513, var1.getProperty(VariantRec.EXOMES_FREQ_AA), 0);
			
			Assert.assertEquals((1891.0/1976.0), var1.getProperty(VariantRec.EXOMES_EA_HOMREF), 0);
			Assert.assertEquals((1891.0/65.0), var1.getProperty(VariantRec.EXOMES_EA_HET), 0);
			Assert.assertEquals((1891.0/20.0), var1.getProperty(VariantRec.EXOMES_EA_HOMALT), 0);
			
			Assert.assertEquals((138.0/61.0), var1.getProperty(VariantRec.EXOMES_AA_HOMREF), 0);
			Assert.assertEquals((138.0/17.0), var1.getProperty(VariantRec.EXOMES_AA_HET), 0);
			Assert.assertEquals((138.0/60.0), var1.getProperty(VariantRec.EXOMES_AA_HOMALT), 0);
			
		} catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
		Assert.assertFalse(thrown);
	}

	@Test
	public void testMultiAlts() {
		
	}
	
	@Test
	public void testMalformedVariant() {
		
	}
	
}


