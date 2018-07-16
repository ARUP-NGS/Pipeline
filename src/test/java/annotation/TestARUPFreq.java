package annotation;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import pipeline.Pipeline;
import util.vcfParser.VCFParser;
import buffer.variant.VariantRec;
import operator.variant.ARUPDBAnnotate;



public class TestARUPFreq extends TestCase {
	File inputFile = new File("src/test/java/annotation/testARUPDB.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml"); //do I need?
	ARUPDBAnnotate annotator;
	boolean thrown;

	public void setUp() {
		try {
			Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
			ppl.setProperty("arup.db.path", "src/test/java/testvcfs/testARUPDB.csv.gz");
			ppl.initializePipeline();
			ppl.stopAllLogging();
			ppl.execute();
			annotator = (ARUPDBAnnotate) ppl.getObjectHandler().getObjectForLabel("ARUPFreqAnno");

		} catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	@Test
	public void testARUPDBSnp() {
		try{
			// Two variants at the same position:
			// First var			
			VariantRec var1 = new VariantRec("1", 220986516, 220986516, "C", "G");
			var1 = VCFParser.normalizeVariant(var1);
			annotator.annotateVariant(var1);
			Assert.assertEquals(.3390, var1.getProperty(VariantRec.ARUP_OVERALL_FREQ), 0.0001);
			Assert.assertEquals(288, var1.getProperty(VariantRec.ARUP_HET_COUNT), 0.0001);
			Assert.assertEquals(75, var1.getProperty(VariantRec.ARUP_HOM_COUNT), 0.0001);
			Assert.assertEquals(646, var1.getProperty(VariantRec.ARUP_SAMPLE_COUNT), 0.0001);
			Assert.assertEquals("Samples: 646 Hets: 288 Homs: 75", var1.getAnnotation(VariantRec.ARUP_FREQ_DETAILS));
			// Second var			
			VariantRec var2 = new VariantRec("1", 220986516, 220986516, "C", "T");
			var2 = VCFParser.normalizeVariant(var2);
			annotator.annotateVariant(var2);
			Assert.assertEquals(.27399, var2.getProperty(VariantRec.ARUP_OVERALL_FREQ), 0.0001);
			Assert.assertEquals(250, var2.getProperty(VariantRec.ARUP_HET_COUNT), 0.0001);
			Assert.assertEquals(52, var2.getProperty(VariantRec.ARUP_HOM_COUNT), 0.0001);
			Assert.assertEquals(646, var2.getProperty(VariantRec.ARUP_SAMPLE_COUNT), 0.0001);
			Assert.assertEquals("Samples: 646 Hets: 250 Homs: 52", var2.getAnnotation(VariantRec.ARUP_FREQ_DETAILS));
		}  catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
	}
	@Test
	public void testARUPDBNewVar() {
		try {
			// Variant that isn't in database
			VariantRec var3 = new VariantRec("9", 117165114, 117165114, "G", "T");
			var3 = VCFParser.normalizeVariant(var3);
			annotator.annotateVariant(var3);
			Assert.assertEquals(0.0, var3.getProperty(VariantRec.ARUP_OVERALL_FREQ), 0.0001);
			Assert.assertEquals(0.0, var3.getProperty(VariantRec.ARUP_HET_COUNT), 0.0001);
			Assert.assertEquals(0.0, var3.getProperty(VariantRec.ARUP_HOM_COUNT), 0.0001);
			Assert.assertEquals(0.0, var3.getProperty(VariantRec.ARUP_SAMPLE_COUNT), 0.0001);
			Assert.assertEquals("Total samples: 0", var3.getAnnotation(VariantRec.ARUP_FREQ_DETAILS));
		}	catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
	}
	@Test
	public void testARUPDBDeletion() {
		try {
			// Deletion
			VariantRec var4 = new VariantRec("13", 20763685, 20763685, "GC", "G");
			var4 = VCFParser.normalizeVariant(var4);
			// After normalization, the position for this deletion shifts to position 20763686
			annotator.annotateVariant(var4);
			Assert.assertEquals(.01404, var4.getProperty(VariantRec.ARUP_OVERALL_FREQ), 0.0001);
			Assert.assertEquals(23, var4.getProperty(VariantRec.ARUP_HET_COUNT), 0.0001);
			Assert.assertEquals(2, var4.getProperty(VariantRec.ARUP_HOM_COUNT), 0.0001);
			Assert.assertEquals(961, var4.getProperty(VariantRec.ARUP_SAMPLE_COUNT), 0.0001);
			Assert.assertEquals("Samples: 961 Hets: 23 Homs: 2", var4.getAnnotation(VariantRec.ARUP_FREQ_DETAILS));
			VariantRec var4b = new VariantRec("13", 20763686, 20763686, "CC", "C");
			var4b = VCFParser.normalizeVariant(var4b);
			// After normalization, the position for this deletion will not shift
			annotator.annotateVariant(var4b);
			Assert.assertEquals(.01404, var4b.getProperty(VariantRec.ARUP_OVERALL_FREQ), 0.0001);
			Assert.assertEquals(23, var4b.getProperty(VariantRec.ARUP_HET_COUNT), 0.0001);
			Assert.assertEquals(2, var4b.getProperty(VariantRec.ARUP_HOM_COUNT), 0.0001);
			Assert.assertEquals(961, var4b.getProperty(VariantRec.ARUP_SAMPLE_COUNT), 0.0001);
			Assert.assertEquals("Samples: 961 Hets: 23 Homs: 2", var4b.getAnnotation(VariantRec.ARUP_FREQ_DETAILS));
		}	catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
	}
	
	@Test
	public void testARUPDBInsertion() {
		try {
			// Insertion
			VariantRec var5 = new VariantRec("16", 90161004, 90161004, "G", "GGG");
			var5 = VCFParser.normalizeVariant(var5);
			annotator.annotateVariant(var5);
			Assert.assertEquals(.018637, var5.getProperty(VariantRec.ARUP_OVERALL_FREQ), 0.0001);
			Assert.assertEquals(23, var5.getProperty(VariantRec.ARUP_HET_COUNT), 0.0001);
			Assert.assertEquals(3, var5.getProperty(VariantRec.ARUP_HOM_COUNT), 0.0001);
			Assert.assertEquals(778, var5.getProperty(VariantRec.ARUP_SAMPLE_COUNT), 0.0001);
			Assert.assertEquals("Samples: 778 Hets: 23 Homs: 3", var5.getAnnotation(VariantRec.ARUP_FREQ_DETAILS));		
		}	catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
	}
	@Test
	public void testARUPDBIndel() {
		try {
			// Indel
			VariantRec var5b = new VariantRec("1", 220986516, 220986516, "CTAT", "TTAC");
			var5b = VCFParser.normalizeVariant(var5b);
			annotator.annotateVariant(var5b);
			Assert.assertEquals(.00077, var5b.getProperty(VariantRec.ARUP_OVERALL_FREQ), 0.0001);
			Assert.assertEquals(1, var5b.getProperty(VariantRec.ARUP_HET_COUNT), 0.0001);
			Assert.assertEquals(0, var5b.getProperty(VariantRec.ARUP_HOM_COUNT), 0.0001);
			Assert.assertEquals(646, var5b.getProperty(VariantRec.ARUP_SAMPLE_COUNT), 0.0001);
			Assert.assertEquals("Samples: 646 Hets: 1 Homs: 0", var5b.getAnnotation(VariantRec.ARUP_FREQ_DETAILS));
		}	catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
	}
	@Test
	public void testARUPDBChromXvar() {
		try {
			// X Chrom variant
			VariantRec var6 = new VariantRec("X", 103042882, 103042882, "T", "C");
			var6 = VCFParser.normalizeVariant(var6);
			annotator.annotateVariant(var6);
			Assert.assertEquals(.28771, var6.getProperty(VariantRec.ARUP_OVERALL_FREQ), 0.0001);
			Assert.assertEquals(143, var6.getProperty(VariantRec.ARUP_HET_COUNT), 0.0001);
			Assert.assertEquals(165, var6.getProperty(VariantRec.ARUP_HOM_COUNT), 0.0001);
			Assert.assertEquals(822, var6.getProperty(VariantRec.ARUP_SAMPLE_COUNT), 0.0001);
			Assert.assertEquals("Samples: 822 Hets: 143 Homs: 165", var6.getAnnotation(VariantRec.ARUP_FREQ_DETAILS));
		}	catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
	}
}
