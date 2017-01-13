package annotation;

import java.io.File;

import operator.variant.MITOMAP_rRNAtRNA;
import operator.variant.MITOMAPcoding;
import operator.variant.MitoMapFrequency;

import org.junit.Assert;
import junit.framework.TestCase;
import pipeline.Pipeline;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;




public class TestMitoMapFrequency extends TestCase {
	
	/**
	 * This TEST CASE currently checks accuracy of BOTH the MITOMAP_rRNAtRNA and MITOMAPcoding annotators
	 * 
	 * author jdurtschi (modified from chrisk)
	 */
	
	
	File inputFile = new File("src/test/java/annotation/testMitoMapFrequency.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");
	
	public void testMitoMapFrequency() {
		
		try {
			Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());

			ppl.setProperty("mitomap.freq.db.path", "src/test/java/testvcfs/mitomap_polymorphisms.vcf.gz");
			ppl.initializePipeline();
			ppl.stopAllLogging();
			ppl.execute();

			MitoMapFrequency annotator = (MitoMapFrequency)ppl.getObjectHandler().getObjectForLabel("MitoMapFrequency");
			
			VariantPool vars = annotator.getVariants();	
			
			VariantRec var = vars.findRecord("MT",146,"T","C");
			System.out.println(var.getContig() + " " + var.getStart() + " " + var.getRef() + " " + var.getAlt() + " " + var.getProperty(VariantRec.MITOMAP_FREQ) + " " + var.getAnnotation(VariantRec.MITOMAP_ALLELE_ID));
			Assert.assertTrue(var != null);
			Assert.assertTrue(var.getProperty(VariantRec.MITOMAP_FREQ).equals(0.196949));
			Assert.assertTrue(var.getAnnotation(VariantRec.MITOMAP_ALLELE_ID).equals("T146C"));
			
			var = vars.findRecord("MT",240,"AAC","-");
			Assert.assertTrue(var != null);
			System.out.println(var.getContig() + " " + var.getStart() + " " + var.getRef() + " " + var.getAlt() + " " + var.getProperty(VariantRec.MITOMAP_FREQ) + " " + var.getAnnotation(VariantRec.MITOMAP_ALLELE_ID));
			Assert.assertTrue(var.getProperty(VariantRec.MITOMAP_FREQ).equals(0.0));
			Assert.assertTrue(var.getAnnotation(VariantRec.MITOMAP_ALLELE_ID).equals("TAAC239T"));

			var = vars.findRecord("MT",309,"-","CCT");
			System.out.println(var.getContig() + " " + var.getStart() + " " + var.getRef() + " " + var.getAlt() + " " + var.getProperty(VariantRec.MITOMAP_FREQ) + " " + var.getAnnotation(VariantRec.MITOMAP_ALLELE_ID));
			Assert.assertTrue(var != null);
			Assert.assertTrue(var.getProperty(VariantRec.MITOMAP_FREQ).equals(0.000218));
			Assert.assertTrue(var.getAnnotation(VariantRec.MITOMAP_ALLELE_ID).equals("C308CCCT"));
			
			var = vars.findRecord("MT",3107,"N","-");
			Assert.assertTrue(var != null);
			System.out.println(var.getContig() + " " + var.getStart() + " " + var.getRef() + " " + var.getAlt()) ;
			Assert.assertTrue(var.getProperty(VariantRec.MITOMAP_FREQ) == null);
			Assert.assertTrue(var.getAnnotation(VariantRec.MITOMAP_ALLELE_ID) == null);
			
			
		} catch (Exception ex) {
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
		
	}
}
