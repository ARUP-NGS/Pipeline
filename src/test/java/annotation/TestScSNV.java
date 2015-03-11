package annotation;

import java.io.File;
import junit.framework.TestCase;
import org.junit.Assert;
import operator.variant.ScSNVAnnotate;
import pipeline.Pipeline;
import buffer.variant.VariantRec;


/**
 * ScSNV unit test
 * 
 * This test validates the ScSNV annotator "ScSNVAnnotata.java".  A truncated version of the database was created
 * to test the annotator.
 * 
 * The truncated database was created by using a sorted random int array between 0 and 15030435 with 1000 elements.
 * The corresponding lines of the file were extracted from the file to create the truncated db which was then 
 * tabix indexed. 
 *   
 * @author Keith Simmon
 *
 */

public class TestScSNV extends TestCase {

	
	File inputFile = new File("src/test/java/annotation/testScSNV.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");
	
	public void testTestScSnc() {
		
		try {
			Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
			
			ppl.setProperty("dbScSNV.path", "/home/ksimmon/ARUP_git/Pipeline/src/test/java/testcsvs/dbscSNV_cat_1-22XY_100.tab.bgz"); 
			ppl.initializePipeline();
			ppl.stopAllLogging();
			ppl.execute();
			
			//Check to see if UK10KAnnotator is adding the correct AF value annotations
			ScSNVAnnotate annotator = (ScSNVAnnotate)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
		
			//1       860327  A       C       n       y       upstream        SAMD11  .       .       UTR5    ENSG00000187634 .       .       0.00430955476585136     0.04
			//Create variants to test variants within the DBscSNC file
			//scores null
			VariantRec var = new VariantRec("1", 860327, 860327, "A", "C"); 
			annotator.annotateVariant(var);
			Assert.assertNull(var.getProperty(VariantRec.scSNV_ada)); 
			Assert.assertNull(var.getProperty(VariantRec.scSNV_rf)); 
			
			
			
			//chr 1 scores 0.998702789872202       0.922
			VariantRec var1 = new VariantRec("1", 7846900, 7846900, "A", "T"); 
			annotator.annotateVariant(var1);
			Assert.assertTrue(var1.getProperty(VariantRec.scSNV_ada).equals(0.998702789872202)); 
			Assert.assertTrue(var1.getProperty(VariantRec.scSNV_rf).equals(.922)); 
			
			
			
			
			//chr 2 scores  0.996337829495143       0.428
			VariantRec var2 = new VariantRec("2", 96542261, 96542261, "C", "G"); 
			annotator.annotateVariant(var2);
			Assert.assertTrue(var2.getProperty(VariantRec.scSNV_ada).equals(0.996337829495143)); 
			Assert.assertTrue(var2.getProperty(VariantRec.scSNV_rf).equals(0.428)); 
			
			//chr 2 scores 0.0186767150217838      .
			//rf score should be null
			VariantRec var3 = new VariantRec("2", 153520487, 153520487, "G", "A"); 
			annotator.annotateVariant(var3);
			Assert.assertTrue(var3.getProperty(VariantRec.scSNV_ada).equals(0.0186767150217838)); 
			Assert.assertNull(var3.getProperty(VariantRec.scSNV_rf)); 
			
			
			//chr 4 scores 4.53814986019981E-5     .
		    //rf score should be null
			VariantRec var4 = new VariantRec("4", 71065869, 71065869, "A", "G"); 
			annotator.annotateVariant(var4);
			Assert.assertTrue(var4.getProperty(VariantRec.scSNV_ada).equals(Double.parseDouble("4.53814986019981E-5"))); 
			Assert.assertNull(var4.getProperty(VariantRec.scSNV_rf)); 
			
			
			
			//chr 6 scores 1.39980422642338E-4     0.0
			VariantRec var5 = new VariantRec("6", 150111201, 150111201, "G", "T"); 
			annotator.annotateVariant(var5);
			Assert.assertTrue(var5.getProperty(VariantRec.scSNV_ada).equals(Double.parseDouble("1.39980422642338E-4"))); 
			Assert.assertTrue(var5.getProperty(VariantRec.scSNV_rf).equals(0.0)); 
			
			//modified from above
			//chr X scores 0.878218844657696       0.608
			VariantRec var6 = new VariantRec("X", 101572115, 101572115, "T", "G"); 
			annotator.annotateVariant(var6);
			Assert.assertTrue(var6.getProperty(VariantRec.scSNV_ada).equals(0.878218844657696)); 
			Assert.assertTrue(var6.getProperty(VariantRec.scSNV_rf).equals(0.608)); 
			
			//Test first line
			// scores  2.93008933363103E-5     0.0
			VariantRec var7 = new VariantRec("1",  1139623,  1139623, "T", "A"); 
			annotator.annotateVariant(var7);
			Assert.assertTrue(var7.getProperty(VariantRec.scSNV_ada).equals(2.93008933363103E-5)); 
			Assert.assertTrue(var7.getProperty(VariantRec.scSNV_rf).equals(0.0)); 
			
			//test first line alternative SNV
			//null
			VariantRec var8 = new VariantRec("1",  1139623,  1139623, "T", "C"); 
			annotator.annotateVariant(var8);
			Assert.assertNull(var8.getProperty(VariantRec.scSNV_ada));
			Assert.assertNull(var8.getProperty(VariantRec.scSNV_rf));
			
			
			//test before first line
			//null
			VariantRec var9 = new VariantRec("1",  1139622,  1139622, "T", "C"); 
			annotator.annotateVariant(var9);
			Assert.assertNull(var9.getProperty(VariantRec.scSNV_ada));
			Assert.assertNull(var9.getProperty(VariantRec.scSNV_rf));
			
			//Test last line
			//scores 0.999806680513166       0.94
			VariantRec var10 = new VariantRec("X", 154213081, 154213081, "G", "C"); 
			annotator.annotateVariant(var10);
			Assert.assertTrue(var10.getProperty(VariantRec.scSNV_ada).equals(0.999806680513166));
			Assert.assertTrue(var10.getProperty(VariantRec.scSNV_rf).equals(0.94));
			
			//Test last line with variant not present
			//NULL NULL
			VariantRec var11 = new VariantRec("X", 154213081, 154213081, "G", "A"); 
			annotator.annotateVariant(var11);
			Assert.assertNull(var11.getProperty(VariantRec.scSNV_ada));
			Assert.assertNull(var11.getProperty(VariantRec.scSNV_rf));
			
			//Test beyond last line not present
			//NULL NULL
			VariantRec var12 = new VariantRec("X", 154213082, 154213082, "G", "A"); 
			annotator.annotateVariant(var12);
			Assert.assertNull(var12.getProperty(VariantRec.scSNV_ada));
			Assert.assertNull(var12.getProperty(VariantRec.scSNV_rf));
			
			
			//Three variants at the same position
			//1       24785368        T       A           0.999978160008217       0.97
			//1       24785368        T       C             0.0131107838506396      0.126
			//1       24785368        T       G            0.99451484988481        0.834
			VariantRec var13 = new VariantRec("1", 24785368, 24785368, "T", "A"); 
			VariantRec var14 = new VariantRec("1", 24785368, 24785368, "T", "C"); 
			VariantRec var15 = new VariantRec("1", 24785368, 24785368, "T", "G"); 
			annotator.annotateVariant(var13);
			annotator.annotateVariant(var14);
			annotator.annotateVariant(var15);
			
			Assert.assertTrue(var13.getProperty(VariantRec.scSNV_ada).equals(0.999978160008217));
			Assert.assertTrue(var13.getProperty(VariantRec.scSNV_rf).equals(0.97));
			
			Assert.assertTrue(var14.getProperty(VariantRec.scSNV_ada).equals(0.0131107838506396));
			Assert.assertTrue(var14.getProperty(VariantRec.scSNV_rf).equals(0.126));
			
			Assert.assertTrue(var15.getProperty(VariantRec.scSNV_ada).equals(0.99451484988481));
			Assert.assertTrue(var15.getProperty(VariantRec.scSNV_rf).equals(0.834));
			
			//null
			VariantRec var16 = new VariantRec("1", 24785368, 24785368, "T", "T"); 
			annotator.annotateVariant(var16);
			Assert.assertNull(var16.getProperty(VariantRec.scSNV_ada));
			Assert.assertNull(var16.getProperty(VariantRec.scSNV_rf));
			
			
			
		} catch (Exception ex){
		
		System.err.println("Exception during testing: " + ex.getLocalizedMessage());
		ex.printStackTrace();
		Assert.assertTrue(false);
		}
		
	}

}
