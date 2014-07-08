package annotation;

import java.io.File;

import junit.framework.TestCase;
import operator.snpeff.SnpEffGeneAnnotate;

import org.junit.Assert;

import pipeline.Pipeline;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class TestSnpEff extends TestCase {
	
	
	File inputFile = new File("src/test/java/annotation/testSnpEff.xml");
	File inputFile2 = new File("src/test/java/annotation/testSnpEff2.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");
	
	public void testSnpEff() {
		
	
			try {
				Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
				ppl.setProperty("snpeff.dir", "/home/brendan/snpEff_3_6/");
				ppl.initializePipeline();
				ppl.stopAllLogging();
				
				ppl.execute();
				
				//Grab the snpEff annotator - we'll take a look at the variants to make sure
				//they're ok
				SnpEffGeneAnnotate annotator = (SnpEffGeneAnnotate)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
				VariantPool vars = annotator.getVariants();
				Assert.assertTrue(vars.size() == 88);
				
				VariantRec var = vars.findRecord("20", 31022442, "-", "G");
				Assert.assertTrue(var != null);
				Assert.assertTrue(var.getAnnotation(VariantRec.VARIANT_TYPE).equalsIgnoreCase("FRAME_SHIFT"));
				Assert.assertTrue(var.getAnnotation(VariantRec.NM_NUMBER).contains("NM_015338"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).contains("ASXL1"));
				
				var = vars.findRecord("13", 28602256, "C", "T");
				Assert.assertTrue(var != null);
				Assert.assertTrue(var.getAnnotation(VariantRec.VARIANT_TYPE).equalsIgnoreCase("INTRON"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).equalsIgnoreCase("FLT3"));
				Assert.assertTrue(var.getAnnotation(VariantRec.NM_NUMBER).contains("NM_004119"));
				
				
			} catch (Exception ex) {
				ex.printStackTrace();
				Assert.assertTrue(false);
			}
			
			
			try {
				Pipeline ppl = new Pipeline(inputFile2, propertiesFile.getAbsolutePath());
				ppl.setProperty("snpeff.dir", "/home/brendan/snpEff_3_6/");
				ppl.initializePipeline();
				ppl.stopAllLogging();
				
				ppl.execute();
				
				SnpEffGeneAnnotate annotator = (SnpEffGeneAnnotate)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
				VariantPool vars = annotator.getVariants();
				Assert.assertTrue(vars.size() == 6);
				
				VariantRec var = vars.findRecord("1", 24201919, "T", "C");
				Assert.assertTrue(var != null); 
				Assert.assertTrue(var.getAnnotation(VariantRec.VARIANT_TYPE).equalsIgnoreCase("SYNONYMOUS"));
				Assert.assertTrue(var.getAnnotation(VariantRec.CDOT).contains("c.189A>G"));
				Assert.assertTrue(var.getAnnotation(VariantRec.NM_NUMBER).contains("NM_001841"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).contains("CNR2"));
				
				var = vars.findRecord("1", 26582091, "G", "A");
				Assert.assertTrue(var != null); 
				Assert.assertTrue(var.getAnnotation(VariantRec.VARIANT_TYPE).equalsIgnoreCase("NON_SYNONYMOUS"));
				Assert.assertTrue(var.getAnnotation(VariantRec.PDOT).contains("p.Ser162Asn"));
				Assert.assertTrue(var.getAnnotation(VariantRec.CDOT).contains("c.485G>A"));
				Assert.assertTrue(var.getAnnotation(VariantRec.NM_NUMBER).contains("NM_001281517"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).contains("CEP85"));
				
				
				var = vars.findRecord("1", 1900107, "-", "CTC");
				Assert.assertTrue(var != null); 
				Assert.assertTrue(var.getAnnotation(VariantRec.VARIANT_TYPE).equalsIgnoreCase("CODON_INSERTION"));
				Assert.assertTrue(var.getAnnotation(VariantRec.PDOT).contains("p.403Lys_404LysinsLysGlu"));
				Assert.assertTrue(var.getAnnotation(VariantRec.CDOT).contains("c.1212_1213insGAG"));
				Assert.assertTrue(var.getAnnotation(VariantRec.NM_NUMBER).contains("NM_001080484"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).contains("KIAA1751"));
				
				var = vars.findRecord("1", 16464673, "G", "A");
				Assert.assertTrue(var != null); 
				Assert.assertTrue(var.getAnnotation(VariantRec.VARIANT_TYPE).equalsIgnoreCase("SYNONYMOUS"));
				Assert.assertTrue(var.getAnnotation(VariantRec.PDOT).contains("p.Pro329"));
				Assert.assertTrue(var.getAnnotation(VariantRec.CDOT).contains("c.987C>T"));
				Assert.assertTrue(var.getAnnotation(VariantRec.NM_NUMBER).contains("NM_004431"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).contains("EPHA2"));
				
				
				var = vars.findRecord("1", 47280747, "AT", "-");
				Assert.assertTrue(var != null); 
				Assert.assertTrue(var.getAnnotation(VariantRec.VARIANT_TYPE).equalsIgnoreCase("FRAME_SHIFT"));
				Assert.assertTrue(var.getAnnotation(VariantRec.PDOT).contains("p.293Asp_294Glufs"));
				Assert.assertTrue(var.getAnnotation(VariantRec.CDOT).contains("c.881_882delAT"));
				Assert.assertTrue(var.getAnnotation(VariantRec.NM_NUMBER).contains("NM_000779"));
				Assert.assertTrue(var.getAnnotation(VariantRec.GENE_NAME).contains("CYP4B1"));
				
				
			} catch (Exception ex) {
				
			}
	
		
	}
}