package annotation;

import java.io.File;

import junit.framework.TestCase;
import operator.annovar.GeneAnnotator;

import org.junit.Assert;

import pipeline.Pipeline;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class TestAnnovar extends TestCase {

	File inputFile = new File("src/test/java/annotation/testAnnovar1.xml");
	File inputFile2 = new File("src/test/java/annotation/testAnnovar2.xml");
	File inputFile3 = new File("src/test/java/annotation/testAnnovar3.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");
	File annovarDir = new File("annovarDirLink");
	
	public void testAnnovar() {
		
			if (! annovarDir.exists()) {
				throw new IllegalStateException("Can't run this test since you don't have annovar installed. You must create a link called 'annovarDirLink' in the main Pipeline directory to the Annovar directory to use this.");
			}
		
			try {
				Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
				ppl.setProperty("annovar.path", annovarDir.getAbsolutePath() + "/");
				ppl.initializePipeline();
				ppl.stopAllLogging();
				
				ppl.execute();
				
				//Grab the annotator - we'll take a look at the variants to make sure
				//they're ok
				GeneAnnotator annotator = (GeneAnnotator)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
				VariantPool vars = annotator.getVariants();
				Assert.assertTrue(vars.size() == 90);
				
				VariantRec var = vars.findRecord("20", 31022442, "-", "G");
				Assert.assertTrue(var != null);
				Assert.assertTrue(var.getAnnotation(VariantRec.EXON_FUNCTION).equalsIgnoreCase("frameshift insertion"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VARIANT_TYPE).equalsIgnoreCase("exonic"));
				Assert.assertTrue(var.getAnnotation(VariantRec.NM_NUMBER).contains("NM_015338"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).contains("ASXL1"));
				
				var = vars.findRecord("13", 28602256, "C", "T");
				Assert.assertTrue(var != null);
				Assert.assertTrue(var.getAnnotation(VariantRec.VARIANT_TYPE).equalsIgnoreCase("intronic"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).equalsIgnoreCase("FLT3"));
				
				
				
			} catch (Exception ex) {
				ex.printStackTrace();
				Assert.assertTrue(false);
			}
			
			
			try {
				Pipeline ppl = new Pipeline(inputFile2, propertiesFile.getAbsolutePath());
				ppl.setProperty("annovar.path", annovarDir.getAbsolutePath() + "/");
				ppl.initializePipeline();
				ppl.stopAllLogging();
				
				ppl.execute();
				
				//Grab the annotator - we'll take a look at the variants to make sure
				//they're ok
				GeneAnnotator annotator = (GeneAnnotator)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
				VariantPool vars = annotator.getVariants();
				Assert.assertTrue(vars.size() == 6);
				
				VariantRec var = vars.findRecord("1", 24201919, "T", "C");
				Assert.assertTrue(var != null); 
				Assert.assertTrue(var.getAnnotation(VariantRec.EXON_FUNCTION).equalsIgnoreCase("synonymous SNV"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VARIANT_TYPE).equalsIgnoreCase("exonic"));
				Assert.assertTrue(var.getAnnotation(VariantRec.CDOT).contains("c.A189G"));
				Assert.assertTrue(var.getAnnotation(VariantRec.NM_NUMBER).contains("NM_001841"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).contains("CNR2"));
				
				var = vars.findRecord("1", 26582091, "G", "A");
				Assert.assertTrue(var != null); 
				Assert.assertTrue(var.getAnnotation(VariantRec.EXON_FUNCTION).equalsIgnoreCase("NONSYNONYMOUS SNV"));
				Assert.assertTrue(var.getAnnotation(VariantRec.PDOT).contains("p.S213N"));
				Assert.assertTrue(var.getAnnotation(VariantRec.CDOT).contains("c.G638A"));
				Assert.assertTrue(var.getAnnotation(VariantRec.NM_NUMBER).contains("NM_022778"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).contains("CEP85"));
				
				
				var = vars.findRecord("1", 1900107, "-", "CTC");
				Assert.assertTrue(var != null); 
				Assert.assertTrue(var.getAnnotation(VariantRec.EXON_FUNCTION).equalsIgnoreCase("nonframeshift insertion"));
				Assert.assertTrue(var.getAnnotation(VariantRec.PDOT).contains("p.K404delinsKR"));
				Assert.assertTrue(var.getAnnotation(VariantRec.CDOT).contains("c.1212_1213insGAG"));
				Assert.assertTrue(var.getAnnotation(VariantRec.NM_NUMBER).contains("NM_001080484"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).contains("KIAA1751"));
				
				var = vars.findRecord("1", 16464673, "G", "A");
				Assert.assertTrue(var != null); 
				Assert.assertTrue(var.getAnnotation(VariantRec.EXON_FUNCTION).equalsIgnoreCase("SYNONYMOUS SNV"));
				Assert.assertTrue(var.getAnnotation(VariantRec.PDOT).contains("p.P329P"));
				Assert.assertTrue(var.getAnnotation(VariantRec.CDOT).contains("c.C987T"));
				Assert.assertTrue(var.getAnnotation(VariantRec.NM_NUMBER).contains("NM_004431"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).contains("EPHA2"));
				
				
				var = vars.findRecord("1", 47280747, "AT", "-");
				Assert.assertTrue(var != null); 
				Assert.assertTrue(var.getAnnotation(VariantRec.EXON_FUNCTION).equalsIgnoreCase("frameshift deletion"));
				Assert.assertTrue(var.getAnnotation(VariantRec.PDOT).contains("p.295_295del"));
				Assert.assertTrue(var.getAnnotation(VariantRec.CDOT).contains("c.884_885del"));
				Assert.assertTrue(var.getAnnotation(VariantRec.NM_NUMBER).contains("NM_001099772"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).contains("CYP4B1"));
				
				
				
			} catch (Exception ex) {
				System.err.println("Exception during testing: " + ex.getLocalizedMessage());
				ex.printStackTrace(System.err);
				Assert.assertFalse(true);
			}
			
			
			try {
				Pipeline ppl = new Pipeline(inputFile3, propertiesFile.getAbsolutePath());
				ppl.setProperty("annovar.path", annovarDir.getAbsolutePath() + "/");
				ppl.initializePipeline();
				ppl.stopAllLogging();
				
				ppl.execute();
				
				//Grab the annotator - we'll take a look at the variants to make sure
				//they're ok
				GeneAnnotator annotator = (GeneAnnotator)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
				VariantPool vars = annotator.getVariants();
				Assert.assertTrue(vars.size() == 10);
				
				VariantRec var = vars.findRecord("13", 139870, "AT", "-");
				Assert.assertTrue(var != null); 
				Assert.assertTrue(var.getAnnotation(VariantRec.VARIANT_TYPE).equalsIgnoreCase("Intergenic"));
				
				var = vars.findRecord("13", 139872, "-", "G");
				Assert.assertTrue(var != null); 
				Assert.assertTrue(var.getAnnotation(VariantRec.VARIANT_TYPE).equalsIgnoreCase("Intergenic"));
				
				
				var = vars.findRecord("18", 48610384, "-", "GCAC");
				Assert.assertTrue(var != null); 
				Assert.assertTrue(var.getAnnotation(VariantRec.VARIANT_TYPE).equalsIgnoreCase("UTR3"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).contains("SMAD4"));
			
				var = vars.findRecord("18", 48610383, "CACA", "-");
				Assert.assertTrue(var != null);
				Assert.assertTrue(var.getAnnotation(VariantRec.VARIANT_TYPE).equalsIgnoreCase("UTR3"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).contains("SMAD4"));
				
			} catch (Exception ex) {
				System.err.println("Exception during testing: " + ex.getLocalizedMessage());
				ex.printStackTrace(System.err);
				Assert.assertFalse(true);
			}
	}
}
