package annotation;

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;
import operator.variant.UK10KAnnotator;
import pipeline.Pipeline;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;




public class TestUK10K extends TestCase {
	
	File inputFile = new File("src/test/java/annotation/testUK10K.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");

	
	public void testUK10K() {
		

		try {
			Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
			

			ppl.initializePipeline();
			ppl.stopAllLogging();

			ppl.execute();

			//Check to see if UK10KAnnotator is adding the correct AF value annotations
			UK10KAnnotator annotator = (UK10KAnnotator)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
			//DELETEwhenDone annotator = (DELETEwhenDone)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");

			VariantPool vars = annotator.getVariants();
			
			
			//record SHOULD read: 1	54710	T	ATT
			//VariantRec var = vars.findRecord("1", 54711, "-" , "TT");
			//System.out.println(var);
			
			VariantRec var = vars.findRecord("1", 54715, "TCTTTCTTTCTTTC" , "-");
	
	//		VariantRec var = vars.findRecord("1", 111516, "TG", "T");
			Assert.assertTrue(var != null);
			//System.out.println(vars.findRecord("1", 50482, "GGT","G"));
			//System.out.println("out2");
			//System.out.println(var.getProperty(VariantRec.UK10K_ALLELE_FREQ));
			Assert.assertTrue(var.getProperty(VariantRec.UK10K_ALLELE_FREQ).equals(0.064004));
			
			//var = vars.findRecord("13", 28602256, "C", "T");
			//Assert.assertTrue(var != null);
			//Assert.assertTrue(var.getProperty(VariantRec.UK10K_ALLELE_FREQ).equals(0.3456));
			//Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).equalsIgnoreCase("FLT3"));
			//Assert.assertTrue(var.getAnnotation(VariantRec.NM_NUMBER).contains("NM_004119"));
			
		} catch (Exception ex) {
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
	}
}
