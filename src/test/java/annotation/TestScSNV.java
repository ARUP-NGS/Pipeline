package annotation;

import java.io.File;

import junit.framework.TestCase;

import org.junit.Assert;

import operator.variant.ScSncAnnotate;
import pipeline.Pipeline;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;



import static org.junit.Assert.*;

import org.junit.Test;

public class TestScSnc extends TestCase {

	
	File inputFile = new File("src/test/java/annotation/testScSNV.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");

	
	
	
	public void testTestScSnc() {
		
		try {
			Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
			
			ppl.setProperty("dbScSNV.path", "/home/ksimmon/reference/dbscSNV_cat_1-22XY.tab.bgz"); //TODO movethis to a pipeline directory and push to git
			ppl.initializePipeline();
			ppl.stopAllLogging();
			ppl.execute();
			
			//Check to see if UK10KAnnotator is adding the correct AF value annotations
			ScSncAnnotate annotator = (ScSncAnnotate)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");

			VariantPool vars = annotator.getVariants();	// I dont need this
			
			
			System.out.println(vars.toString());
			
			//1       860327  A       C       n       y       upstream        SAMD11  .       .       UTR5    ENSG00000187634 .       .       0.00430955476585136     0.04
			//Create variants to test variants within the DBscSNC file
			VariantRec var = new VariantRec("1", 860327, 860327, "A", "C");
			annotator.annotateVariant(var);
			
			System.out.println(var.toString());
			System.out.println(var.getProperty(VariantRec.scSNV_ada));
			
			
			System.out.println(var.toString());
			
			
			
			
			
			
			
		} catch (Exception ex){
		
		System.err.println("Exception during testing: " + ex.getLocalizedMessage());
		ex.printStackTrace();
		Assert.assertTrue(false);
		}
		
	}

}
