package annotation;

import java.io.File;

import junit.framework.TestCase;
import operator.variant.LowComplexityRegionsAnnotator;

import org.junit.Assert;

import pipeline.Pipeline;
import buffer.variant.VariantRec;


/**
 * LowComplexityRegionsAnnotator unit test
 * 
 * This test validates the LowComplexityRegions annotator "LowComplexityRegionsAnnotator.java".
 * A truncated version of the low complexity regions bed file
 *  was created to test the annotator.
 * A random 0.1% prob of selection was applied to each bed region
 * Only selected regions were kept in the truncated version of the bed file
 *   
 * @author Jacob Durtschi
 * Modified from code by Keith Simmon
 */

public class TestLowComplexityRegionsAnnotator extends TestCase {

	File inputFile = new File("src/test/java/annotation/testLowComplexityRegionsAnnotator.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");
	
	public void testTestLowComplexityRegionsAnnotator() {
		
		try {
			Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
			
			ppl.initializePipeline();
			ppl.stopAllLogging();
			ppl.execute();
			
			//Check to see if LowComplexityAnnotator is adding the correct annotation
			LowComplexityRegionsAnnotator annotator = (LowComplexityRegionsAnnotator)ppl.getObjectHandler().getObjectForLabel("LowComplexityRegionsAnnotator");
		
			//Create variants to test correct annotation of LowComplexityRegionsAnnotator using LowComplexityRegions Bed file
			//Ref and alt bases were chosen randomly for convenience since they
			//are not checked against the reference sequence

			//1	774884	774894
			//should be true (SNV at first base of bed region)
			VariantRec var = new VariantRec("1", 774885, 774886, "A", "C"); 
			annotator.annotateVariant(var);
			Assert.assertTrue(var.getAnnotation(VariantRec.LOW_COMPLEX_REGION).equals("true")); 
			//should be null (SNV before first base of first bed region)
			VariantRec var1= new VariantRec("1", 774884, 774885, "A", "C"); 
			annotator.annotateVariant(var1);
			Assert.assertNull(var1.getAnnotation(VariantRec.LOW_COMPLEX_REGION)); 
			
			//2	231805975	231805986
			//should be true (SNV at last base of bed region)
			VariantRec var2 = new VariantRec("2", 231805986, 231805987, "G", "T"); 
			annotator.annotateVariant(var2);
			Assert.assertTrue(var2.getAnnotation(VariantRec.LOW_COMPLEX_REGION).equals("true")); 
			//should be null (SNV after last base in bed region)
			VariantRec var3= new VariantRec("2", 231805987, 231805988, "G", "T"); 
			annotator.annotateVariant(var3);
			Assert.assertNull(var3.getAnnotation(VariantRec.LOW_COMPLEX_REGION)); 

			//5	172790873	172790883
			//should be true (del includes first base of region)
			VariantRec var4 = new VariantRec("5", 172790865, 172790875, "AAAAAAAAAA", ""); 
			annotator.annotateVariant(var4);
			Assert.assertTrue(var4.getAnnotation(VariantRec.LOW_COMPLEX_REGION).equals("true")); 
			//should be null (del before first base of region)
			VariantRec var5= new VariantRec("5", 172790864, 172790874, "AAAAAAAAAA", ""); 
			annotator.annotateVariant(var5);
			Assert.assertNull(var5.getAnnotation(VariantRec.LOW_COMPLEX_REGION));

			//7	107575347	107575365
			//should be true (del at last base of region)
			VariantRec var6 = new VariantRec("7", 107575365, 107575375, "CCCCCCCCC", ""); 
			annotator.annotateVariant(var6);
			Assert.assertTrue(var6.getAnnotation(VariantRec.LOW_COMPLEX_REGION).equals("true")); 
			//should be null (del after last base of region)
			VariantRec var7 = new VariantRec("7", 107575366, 107575376, "CCCCCCCCC", ""); 
			annotator.annotateVariant(var7);
			Assert.assertNull(var7.getAnnotation(VariantRec.LOW_COMPLEX_REGION));

			//15	95010634	95010644
			//should be null (ins includes first base of region)
			VariantRec var8 = new VariantRec("15", 95010635, 95010635, "", "AAAAAAAAAA"); 
			annotator.annotateVariant(var8);
			Assert.assertTrue(var8.getAnnotation(VariantRec.LOW_COMPLEX_REGION).equals("true")); 
			//should be null (ins before first base of region)
			VariantRec var9 = new VariantRec("15", 95010634, 95010634, "", "AAAAAAAAAA"); 
			annotator.annotateVariant(var9);
			Assert.assertNull(var9.getAnnotation(VariantRec.LOW_COMPLEX_REGION));

			//19	52894767	52894776
			//should be true (ins at last base of region)
			VariantRec var10 = new VariantRec("19", 52894776, 52894776, "", "CCCCCCCCC"); 
			annotator.annotateVariant(var10);
			Assert.assertTrue(var10.getAnnotation(VariantRec.LOW_COMPLEX_REGION).equals("true")); 
			//should be null (ins after last base of region)
			VariantRec var11 = new VariantRec("19", 52894777, 52894777, "", "CCCCCCCCC"); 
			annotator.annotateVariant(var11);
			Assert.assertNull(var11.getAnnotation(VariantRec.LOW_COMPLEX_REGION));

			//Y	28691737	28691744
			//should be true (deletion spanning past last bed item)
			VariantRec var12 = new VariantRec("Y", 28691744, 28691754, "AAAAAAAAAA", ""); 
			annotator.annotateVariant(var12);
			//should be null (deletion starting past last bed item)
			Assert.assertTrue(var12.getAnnotation(VariantRec.LOW_COMPLEX_REGION).equals("true")); 
			VariantRec var13 = new VariantRec("Y", 28691745, 28691746, "AAAAAAAAAA", ""); 
			annotator.annotateVariant(var13);
			Assert.assertNull(var13.getAnnotation(VariantRec.LOW_COMPLEX_REGION));
			
			//Three variants at the same position
			//11	10844734	10844745
			VariantRec var14 = new VariantRec("11", 10844736, 10844737, "A", "C"); 
			VariantRec var15 = new VariantRec("11", 10844736, 10844737, "A", "G"); 
			VariantRec var16 = new VariantRec("11", 10844736, 10844737, "A", "T");
			annotator.annotateVariant(var14);
			annotator.annotateVariant(var15);
			annotator.annotateVariant(var16);

			Assert.assertTrue(var14.getAnnotation(VariantRec.LOW_COMPLEX_REGION).equals("true")); 
			Assert.assertTrue(var15.getAnnotation(VariantRec.LOW_COMPLEX_REGION).equals("true")); 
			Assert.assertTrue(var16.getAnnotation(VariantRec.LOW_COMPLEX_REGION).equals("true")); 

		} catch (Exception ex){
		
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
	}

}
