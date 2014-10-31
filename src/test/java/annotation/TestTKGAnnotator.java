package annotation;

import java.io.File;

import junit.framework.TestCase;
import operator.variant.TGPTabixAnnotator;

import org.junit.Assert;

import pipeline.Pipeline;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class TestTKGAnnotator extends TestCase {

	File inputFile = new File("src/test/java/annotation/testTKG.xml");
	File testTKGFile = new File("src/test/java/annotation/testTKG.vcf.gz");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");
	
	public void testTKG() {
		try {
			Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
			ppl.setProperty("tgp.sites.path", testTKGFile.getAbsolutePath());
			ppl.initializePipeline();
			ppl.stopAllLogging();
			
			ppl.execute();
			
			//Grab the annotator - we'll take a look at the variants to make sure
			//they're ok
			TGPTabixAnnotator annotator = (TGPTabixAnnotator)ppl.getObjectHandler().getObjectForLabel("Annotate");
			VariantPool vars = annotator.getVariants();
			
			VariantRec var = vars.findRecord("1", 774374, "G", "A");
			Assert.assertTrue(var != null);
			Assert.assertTrue(var.getProperty(VariantRec.POP_FREQUENCY) == 0.0027);
			
			
			var = vars.findRecord("1", 774391, "G", "T");
			Assert.assertTrue(var != null);
			Assert.assertTrue(var.getProperty(VariantRec.POP_FREQUENCY) == 0.01);
			
			//This one doesn't exist, should be null
			var = vars.findRecord("1", 774391, "G", "C");
			Assert.assertTrue(var == null);
			
			var = vars.findRecord("1", 774547, "GAGA", "-");
			Assert.assertTrue(var != null);
			Assert.assertTrue(var.getProperty(VariantRec.POP_FREQUENCY) == 0.02);
			
			
			var = vars.findRecord("1", 774582, "-", "A");
			Assert.assertTrue(var != null);
			Assert.assertTrue(var.getProperty(VariantRec.POP_FREQUENCY) == 0.01);
			
			
		} catch (Exception ex) {
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
}
