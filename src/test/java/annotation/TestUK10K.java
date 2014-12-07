package annotation;

import java.io.File;

import junit.framework.TestCase;
import operator.variant.UK10KAnnotator;

//import org.junit.Assert;
import junit.framework.Assert;

import pipeline.Pipeline;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;




/**
 * Testing the UK10KAnnotator. Currently (11/4/2014) using the myeloid.vcf and the first ~1000
 * lines of the UK10K vcf file along with several others.
 * 
 * 
 * NOTE: In cases where you are checking indels, you will need to increment the pos +1
 * 	and strip off the first base in the ref and alt. If only a single base, put a dash.
 */
public class TestUK10K extends TestCase {
	
	
	File inputFile = new File("src/test/java/annotation/testUK10K.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");

	
	public void testUK10K() {
		

		try {
			Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
			
			ppl.setProperty("UK10K.path", "src/test/java/testvcfs/UK10K_1000+rand.vcf.gz");

			ppl.initializePipeline();
			ppl.stopAllLogging();

			ppl.execute();

			//Check to see if UK10KAnnotator is adding the correct AF value annotations
			UK10KAnnotator annotator = (UK10KAnnotator)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");

			VariantPool vars = annotator.getVariants();	
			

			VariantRec var = vars.findRecord("2", 25458546, "C" , "T");
			System.out.println("UK10K var:  "+var);
			Assert.assertTrue(var != null);
			Assert.assertTrue(var.getProperty(VariantRec.UK10K_ALLELE_FREQ).equals(0.588336));
			
			VariantRec var1 = vars.findRecord("X", 15836648, "T" , "G");
			Assert.assertTrue(var1 != null);
			Assert.assertTrue(var1.getProperty(VariantRec.UK10K_ALLELE_FREQ).equals(0.47082));
					
			VariantRec var4 = vars.findRecord("19", 10273297, "AA" , "-");
			Assert.assertTrue(var4 != null);
			Assert.assertTrue(var4.getProperty(VariantRec.UK10K_ALLELE_FREQ).equals(0.403729));
			
			
			VariantRec var5 = vars.findRecord("X", 15818115, "T" , "-");
			Assert.assertTrue(var5 != null);
			Assert.assertTrue(var5.getProperty(VariantRec.UK10K_ALLELE_FREQ).equals(0.469911));
			

			
		} catch (Exception ex) {
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
	}
}
