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

	File inputFile = new File("src/test/java/annotation/testBadRegionAnnotator.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");
	
	public void testTestBadRegions() {
		
		try {
			Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
			
			ppl.initializePipeline();
			ppl.stopAllLogging();
			ppl.execute();
			
			//Check to see if BadRegionsAnnotator is adding the correct annotation
			BadRegionAnnotator annotator = (BadRegionAnnotator)ppl.getObjectHandler().getObjectForLabel("BadRegionAnnotator");
		
			//Create variants to test correct annotation of BadRegionAnnotator using BadRegions Bed file
			//Ref and alt bases were chosen randomly for convenience since they
			//are not checked against the reference sequence

			//1	1576253	1576256	ProbBps0.05	0	.
			//should be true (SNV at first base of bed region)
			VariantRec var = new VariantRec("1", 1576254, 1576255, "A", "C"); 
			annotator.annotateVariant(var);
			Assert.assertTrue(var.getAnnotation(VariantRec.BAD_REGION).equals("true")); 
			//should be null (SNV before first base of first bed region)
			VariantRec var1= new VariantRec("1", 1576252, 1576253, "A", "C"); 
			annotator.annotateVariant(var1);
			Assert.assertNull(var1.getAnnotation(VariantRec.BAD_REGION)); 
			
			//2	174829302	174829305	ProbBps0.05	0	.
			//should be true (SNV at last base of bed region)
			VariantRec var2 = new VariantRec("2", 174829304, 174829305, "G", "T"); 
			annotator.annotateVariant(var2);
			Assert.assertTrue(var2.getAnnotation(VariantRec.BAD_REGION).equals("true")); 
			//should be null (SNV after last base in bed region)
			VariantRec var3= new VariantRec("2", 174829306, 174829307, "G", "T"); 
			annotator.annotateVariant(var3);
			Assert.assertNull(var3.getAnnotation(VariantRec.BAD_REGION)); 

			//5	151784729	151784733	ProbBps0.05	0	.
			//should be true (del includes first base of region)
			VariantRec var4 = new VariantRec("5", 151784721, 151784731, "AAAAAAAAAA", ""); 
			annotator.annotateVariant(var4);
			Assert.assertTrue(var4.getAnnotation(VariantRec.BAD_REGION).equals("true")); 
			//should be null (del before first base of region)
			VariantRec var5= new VariantRec("5", 151784719, 151784729, "AAAAAAAAAA", ""); 
			annotator.annotateVariant(var5);
			Assert.assertNull(var5.getAnnotation(VariantRec.BAD_REGION));

			//7	5429977	5429980	ProbBps0.05	0	.
			//should be true (del at last base of region)
			VariantRec var6 = new VariantRec("7", 5429979, 5429989, "CCCCCCCCC", ""); 
			annotator.annotateVariant(var6);
			Assert.assertTrue(var6.getAnnotation(VariantRec.BAD_REGION).equals("true")); 
			//should be null (del after last base of region)
			VariantRec var7 = new VariantRec("7", 5429981, 5429991, "CCCCCCCCC", ""); 
			annotator.annotateVariant(var7);
			Assert.assertNull(var7.getAnnotation(VariantRec.BAD_REGION));

			//15	74865016	74865019	ProbBps0.05	0	.
			//should be null (ins includes first base of region)
			VariantRec var8 = new VariantRec("15", 74865017, 74865017, "", "AAAAAAAAAA"); 
			annotator.annotateVariant(var8);
			Assert.assertTrue(var8.getAnnotation(VariantRec.BAD_REGION).equals("true")); 
			//should be null (ins before first base of region)
			VariantRec var9 = new VariantRec("15", 74865016, 74865016, "", "AAAAAAAAAA"); 
			annotator.annotateVariant(var9);
			Assert.assertNull(var9.getAnnotation(VariantRec.BAD_REGION));

			//19	7742994	7742997	ProbBps0.05	0	.
			//should be true (ins at last base of region)
			VariantRec var10 = new VariantRec("19", 7742997, 7742997, "", "CCCCCCCCC"); 
			annotator.annotateVariant(var10);
			Assert.assertTrue(var10.getAnnotation(VariantRec.BAD_REGION).equals("true")); 
			//should be null (ins after last base of region)
			VariantRec var11 = new VariantRec("19", 7742998, 7742998, "", "CCCCCCCCC"); 
			annotator.annotateVariant(var11);
			Assert.assertNull(var11.getAnnotation(VariantRec.BAD_REGION));

			//X	153697969	153697972	ProbBps0.05	0	.
			//should be true (deletion spanning past last bed item)
			VariantRec var12 = new VariantRec("X", 153697971, 153697981, "AAAAAAAAAA", ""); 
			annotator.annotateVariant(var12);
			//should be null (deletion starting past last bed item)
			Assert.assertTrue(var12.getAnnotation(VariantRec.BAD_REGION).equals("true")); 
			VariantRec var13 = new VariantRec("X", 153697973, 153697983, "AAAAAAAAAA", ""); 
			annotator.annotateVariant(var13);
			Assert.assertNull(var13.getAnnotation(VariantRec.BAD_REGION));
			
			//Three variants at the same position
			//11	114121369	114121373	ProbBps0.05	0	.
			VariantRec var14 = new VariantRec("11", 114121371, 114121372, "A", "C"); 
			VariantRec var15 = new VariantRec("11", 114121371, 114121372, "A", "G"); 
			VariantRec var16 = new VariantRec("11", 114121371, 114121372, "A", "T");
			annotator.annotateVariant(var14);
			annotator.annotateVariant(var15);
			annotator.annotateVariant(var16);

			Assert.assertTrue(var14.getAnnotation(VariantRec.BAD_REGION).equals("true")); 
			Assert.assertTrue(var15.getAnnotation(VariantRec.BAD_REGION).equals("true")); 
			Assert.assertTrue(var16.getAnnotation(VariantRec.BAD_REGION).equals("true")); 

		} catch (Exception ex){
		
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
	}

}
