package annotation;

import java.io.File;

import junit.framework.TestCase;
import operator.variant.HGMDVarAnnotator;

import gene.Gene;

import org.junit.Assert;

import pipeline.Pipeline;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;


public class TestHGMDVarAnnotator extends TestCase{
	File inputFile = new File("src/test/java/annotation/HGMD.xml");
    File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");
    
    Gene gene;
   

    public void testHGMDVarAnnotator() {
    	
    	Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
    	String hgmdHit = null;
    	String hgmdExactHit = null;

        try {
            ppl.setProperty("hgmd.path", "src/test/java/testcsvs/HGMD_Advanced_Substitutions-200.csv");
            ppl.setProperty("hgmd.indel.path", "src/test/java/testcsvs/HGMD_Advanced_Micro_Lesions-200.csv");
            
            ppl.initializePipeline();
            ppl.stopAllLogging();
            ppl.execute(); 
            
        }catch (Exception ex) {
            System.err.println("Exception during initialization: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    		
            HGMDVarAnnotator annotator = (HGMDVarAnnotator) ppl.getObjectHandler().getObjectForLabel("HGMDVar");
            VariantPool vars = annotator.getVariants(); //this is null atm!!!     
	        
            //chr16: 70289732 G-A - DM? Charcot-Marie-Tooth ---test for exact negative strand
	        VariantRec snv1 = vars.findRecord("16", 70289732, "G" , "A");
			Assert.assertTrue(snv1 != null);
			hgmdHit = getSizeHGMD(snv1.getAnnotation(VariantRec.HGMD_HIT));	
			Assert.assertTrue(hgmdHit.contains("true"));
			hgmdExactHit = getSizeHGMD(snv1.getAnnotation(VariantRec.HGMD_HIT_EXACT));
			Assert.assertTrue(hgmdExactHit.contains("true"));
			Assert.assertTrue(snv1.getAnnotation(VariantRec.HGMD_CLASS).equals("DM?"));
			
	        //chr16: 70289732 C-T - DM? Charcot-Marie-Tooth ---test for inexact negative strand
	        VariantRec snv2 = vars.findRecord("16", 70289732, "C" , "T");
			Assert.assertTrue(snv2 != null);
			hgmdHit = getSizeHGMD(snv2.getAnnotation(VariantRec.HGMD_HIT));	
			Assert.assertTrue(hgmdHit.contains("true"));
			hgmdExactHit = getSizeHGMD(snv2.getAnnotation(VariantRec.HGMD_HIT_EXACT));
			Assert.assertTrue(hgmdExactHit.contains("-"));
			Assert.assertTrue(snv2.getAnnotation(VariantRec.HGMD_CLASS).equals("DM?"));
			
			//chr17:35311130 A-G - DM Pancreatic Cancer---test for exact positive strand
			VariantRec snv3 = vars.findRecord("17", 35311130, "A" , "G"); //Switch to reverse complement?
			Assert.assertTrue(snv3 != null);
			hgmdHit = getSizeHGMD(snv3.getAnnotation(VariantRec.HGMD_HIT));	
			Assert.assertTrue(hgmdHit.equals("true"));
			hgmdExactHit = getSizeHGMD(snv3.getAnnotation(VariantRec.HGMD_HIT_EXACT));
			Assert.assertTrue(hgmdExactHit.equals("true"));
			Assert.assertTrue(snv3.getAnnotation(VariantRec.HGMD_CLASS).equals("DM"));
			
			//chr9:107562804 C-T - FP Altered HDL cholesterol levels ---test for exact negative strand
			//in vcf: 9       107562804       .       C       T
			VariantRec snv4 = vars.findRecord("9", 107562804, "C" , "T");
			Assert.assertTrue(snv4 != null);			
			hgmdHit = getSizeHGMD(snv4.getAnnotation(VariantRec.HGMD_HIT));	
			Assert.assertTrue(hgmdHit.equals("true"));
			hgmdExactHit = getSizeHGMD(snv4.getAnnotation(VariantRec.HGMD_HIT_EXACT));
			Assert.assertTrue(hgmdExactHit.equals("true"));
			Assert.assertTrue(snv4.getAnnotation(VariantRec.HGMD_CLASS).equals("FP"));
			
			//chr12:9004550 G-A - DM Noonan Syndrome ---test for inexact positive strand
			VariantRec snv5 = vars.findRecord("12", 9004550, "G" , "A");
			Assert.assertTrue(snv5 != null);
			hgmdHit = getSizeHGMD(snv5.getAnnotation(VariantRec.HGMD_HIT));	
			Assert.assertTrue(hgmdHit.equals("true"));
			hgmdExactHit = getSizeHGMD(snv5.getAnnotation(VariantRec.HGMD_HIT_EXACT));
			Assert.assertTrue(hgmdExactHit.equals("-"));
			Assert.assertTrue(snv5.getAnnotation(VariantRec.HGMD_CLASS).equals("DM"));
			
    	
    }
    //This method is mocked up to mimic behavior of VariantWriter, which simply returns "true" if HGMD_HIT variable has a value
    //For example, HGMD_HIT would normally contain a string like (created by toString() method in HGMDB class): 
    //	"Charcot-Marie-Tooth disease, DM? (NM_001605.2:c.2185C-T,  NULL Lupo vol. JMD: 18 (225))"
    public static String getSizeHGMD(String hgmdAnnoStr){
    	if ( hgmdAnnoStr != null && !hgmdAnnoStr.isEmpty() )
    		return "true";
    	else
    		return "-";
    		
    }
}
        

