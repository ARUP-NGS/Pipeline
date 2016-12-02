package annotation;

import java.io.File;

import junit.framework.TestCase;
import operator.variant.BadRegionAnnotator;

import org.junit.Assert;

import pipeline.Pipeline;
import buffer.variant.VariantRec;


/**
 * BadRegionAnnotator unit test
 * 
 * This test validates the BadRegion annotator "BadRegionAnnotator.java".  A truncated version of the bad regions bed file
 *  was created to test the annotator.
 * A random 1% prob of selection was applied to each bed region
 * Only selected regions were kept in the truncated version of the bed file
 *   
 * @author Jacob Durtschi
 * Modified from code by Keith Simmon
 */

public class TestBadRegionAnnotator extends TestCase {

	File inputFile = new File("src/test/java/annotation/testBadRegionsAnnotator.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");
	
	public void testTestBadRegions() {
		
		try {
			Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
			
			ppl.setProperty("badBED.path", "src/test/java/testBEDs/b37_probBps0.05Pad1bp_v1.0.random1pct.bed"); 
			ppl.initializePipeline();
			ppl.stopAllLogging();
			ppl.execute();
			
			//Check to see if BadRegionsAnnotator is adding the correct annotation
			BadRegionAnnotator annotator = (BadRegionAnnotator)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
		
			//Create variants to test variants within the DBscSNC file
			//1	1576253	1576256	ProbBps0.05	0	.
			//should be true (SNV)
			VariantRec var = new VariantRec("1", 1576254, 1576254, "A", "C"); 
			annotator.annotateVariant(var);
			Assert.assertTrue(var.getProperty(VariantRec.BAD_REGION).equals("true")); 
			//should be null
			VariantRec var1= new VariantRec("1", 1576260, 1576260, "A", "C"); 
			annotator.annotateVariant(var1);
			Assert.assertNull(var1.getProperty(VariantRec.BAD_REGION)); 
			
			//2	174829302	174829305	ProbBps0.05	0	.
			//should be true
			VariantRec var2 = new VariantRec("2", 174829303, 174829303, "G", "T"); 
			annotator.annotateVariant(var2);
			Assert.assertTrue(var2.getProperty(VariantRec.BAD_REGION).equals("true")); 
			//should be null
			VariantRec var3= new VariantRec("2", 174829302, 174829302, "G", "T"); 
			annotator.annotateVariant(var3);
			Assert.assertNull(var3.getProperty(VariantRec.BAD_REGION)); 

			//5	151784729	151784733	ProbBps0.05	0	.
			//should be true (del includes first base of region)
			VariantRec var4 = new VariantRec("5", 151784720, 151784730, "AAAAAAAAAA", ""); 
			annotator.annotateVariant(var4);
			Assert.assertTrue(var4.getProperty(VariantRec.BAD_REGION).equals("true")); 
			//should be null
			VariantRec var5= new VariantRec("5", 151784715, 151784725, "AAAAAAAAAA", ""); 
			annotator.annotateVariant(var5);
			Assert.assertNull(var5.getProperty(VariantRec.BAD_REGION));

			//11	17098032	17098035	ProbBps0.05	0	.
			//should be true (ins at last base of region)
			VariantRec var6 = new VariantRec("11", 17098035, 17098035, "", "CCCCCCCCCC"); 
			annotator.annotateVariant(var6);
			Assert.assertTrue(var6.getProperty(VariantRec.BAD_REGION).equals("true")); 
			//should be null
			VariantRec var7 = new VariantRec("11", 17098040, 17098040, "", "CCCCCCCCCC"); 
			annotator.annotateVariant(var7);
			Assert.assertNull(var7.getProperty(VariantRec.BAD_REGION));

			//X	153697969	153697972	ProbBps0.05	0	.
			//should be true (deletion spanning past last bed item)
			VariantRec var8 = new VariantRec("X", 153697971, 153697981, "AAAAAAAAAA", ""); 
			annotator.annotateVariant(var8);
			Assert.assertTrue(var8.getProperty(VariantRec.BAD_REGION).equals("true")); 
			//should be null (deletion past last bed item)
			VariantRec var9 = new VariantRec("X", 153697971, 153697981, "AAAAAAAAAA", ""); 
			annotator.annotateVariant(var9);
			Assert.assertNull(var9.getProperty(VariantRec.BAD_REGION));
			
			//Three variants at the same position
			//1       24785368        T       A           0.999978160008217       0.97
			//1       24785368        T       C             0.0131107838506396      0.126
			//1       24785368        T       G            0.99451484988481        0.834
			VariantRec var10 = new VariantRec("1", 1576254, 1576254, "A", "C"); 
			VariantRec var11 = new VariantRec("1", 1576254, 1576254, "A", "C"); 
			VariantRec var12 = new VariantRec("1", 1576254, 1576254, "A", "C"); 
			annotator.annotateVariant(var10);
			annotator.annotateVariant(var11);
			annotator.annotateVariant(var12);

			Assert.assertTrue(var10.getProperty(VariantRec.BAD_REGION).equals("true")); 
			Assert.assertTrue(var11.getProperty(VariantRec.BAD_REGION).equals("true")); 
			Assert.assertTrue(var12.getProperty(VariantRec.BAD_REGION).equals("true")); 
			
		} catch (Exception ex){
		
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
	}

}
