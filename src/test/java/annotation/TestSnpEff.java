package annotation;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import operator.snpeff.SnpEffGeneAnnotate;

import org.junit.Assert;
import org.junit.Test;

import com.sun.jndi.ldap.pool.Pool;

import pipeline.ObjectCreationException;
import pipeline.Pipeline;
import pipeline.PipelineDocException;
import plugin.PluginLoaderException;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;
import json.JSONArray;
import json.JSONException;
import json.JSONObject;

public class TestSnpEff extends TestCase {

	public static final String SNPEFF_DIR = "snpeff.dir";

	File inputFile = new File("src/test/java/annotation/testSnpEff.xml");
	File inputFile2 = new File("src/test/java/annotation/testSnpEff2.xml");
	File inputFile3 = new File("src/test/java/annotation/testSnpEff3.xml");
    File inputFile4 = new File("src/test/java/annotation/testSnpEff4.xml");
    File inputFile5 = new File("src/test/java/annotation/testSnpEff5.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");
	File snpEffDir = null;

	private Pipeline preparePipeline(File inFile) {
		Pipeline ppl = null;
		String pipelinePropsFile = System.getProperty("pipelineProps");
		if (pipelinePropsFile != null) { // We are given a properties file.
			ppl = new Pipeline(inFile, pipelinePropsFile);
			String snpEffDirPath = (String) ppl.getProperty(SNPEFF_DIR);
			if (snpEffDirPath == null) {
				throw new IllegalArgumentException("No path to snpEff found, please specify " + SNPEFF_DIR);
			}
			if (!(new File(snpEffDirPath).exists())) {
				throw new IllegalArgumentException("No file found at snpEff path : " + SNPEFF_DIR);
			}

		} else { //User didn't provide pipeline_properties.xml file.
			File snpEffDir = new File("snpEffDirLink");
			if (!snpEffDir.exists()) {
				throw new IllegalStateException(
						"No snpEffDirLink link found. Can't run this test since you don't have SnpEff installed. You must create a link called 'snpEffDirLink' in the main Pipeline directory to the SnpEff directory to use this.");
			}
			ppl = new Pipeline(inFile, propertiesFile.getAbsolutePath());
			ppl.setProperty("snpeff.dir", snpEffDir.getAbsolutePath());
		}	
		return ppl;
	}
	
	private static JSONObject findJsonObj(JSONArray jarr, String key) {
		for(int i=0; i<jarr.length(); i++) {
			try {
				JSONObject jobj = (JSONObject) jarr.get(i);
				if (jobj.has(key)) {
					return jobj.getJSONObject(key);
				}
			} catch (JSONException e) {
				//ignored
			}
			
		}
		return null;
	}
	

	/**
	 * Regression test for cases where two symbolic alts start at the same position but have different lengths,
	 * we didn't annotate these correctly in the past
	 */
	public void testSnpEffAlmostDuplicateHandling() {
		Pipeline ppl;
        ppl = this.preparePipeline(inputFile5);
		try {
			ppl.initializePipeline();
			ppl.stopAllLogging();

			ppl.execute();
			
			SnpEffGeneAnnotate annotator = (SnpEffGeneAnnotate)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
			VariantPool vars = annotator.getVariants();
			Assert.assertTrue(vars.size() == 4);
			
			List<VariantRec> c1vars = vars.getVariantsForContig("10");
			Assert.assertTrue(c1vars.size() == 2);
			JSONArray snpeff_annos = c1vars.get(0).getjsonProperty(VariantRec.SNPEFF_ALL);
			JSONObject hit = findJsonObj(snpeff_annos, "NM_001304717.2.1");
			Assert.assertNotNull(hit);
			Assert.assertTrue(hit.has(VariantRec.VARIANT_TYPE));
			Assert.assertTrue(hit.get(VariantRec.VARIANT_TYPE).equals("conservative_inframe_deletion"));
			Assert.assertTrue(hit.get(VariantRec.GENE_NAME).equals("PTEN"));
			Assert.assertTrue(hit.get(VariantRec.CDOT).equals("c.618_620delANN"));
			
			snpeff_annos = c1vars.get(1).getjsonProperty(VariantRec.SNPEFF_ALL);
			hit = findJsonObj(snpeff_annos, "NM_001304717.2.1");
			Assert.assertNotNull(hit);
			Assert.assertTrue(hit.has(VariantRec.VARIANT_TYPE));
			Assert.assertTrue(hit.get(VariantRec.VARIANT_TYPE).equals("frameshift_variant"));
			Assert.assertTrue(hit.get(VariantRec.GENE_NAME).equals("PTEN"));
			Assert.assertTrue(hit.get(VariantRec.CDOT).equals("c.618_624delANNNNNN"));
			
			
			List<VariantRec> c2vars = vars.getVariantsForContig("13");
			Assert.assertTrue(c2vars.size() == 2);
			
			snpeff_annos = c2vars.get(0).getjsonProperty(VariantRec.SNPEFF_ALL);
			hit = findJsonObj(snpeff_annos, "NM_004119.2.1");
			Assert.assertNotNull(hit);
			Assert.assertTrue(hit.has(VariantRec.VARIANT_TYPE));
			Assert.assertTrue(hit.get(VariantRec.VARIANT_TYPE).equals("frameshift_variant"));
			Assert.assertTrue(hit.get(VariantRec.GENE_NAME).equals("FLT3"));
			Assert.assertTrue(hit.get(VariantRec.CDOT).equals("c.1826_1831dup"));
			
			snpeff_annos = c2vars.get(1).getjsonProperty(VariantRec.SNPEFF_ALL);
			hit = findJsonObj(snpeff_annos, "NM_004119.2.1");
			Assert.assertNotNull(hit);
			Assert.assertTrue(hit.has(VariantRec.VARIANT_TYPE));
			Assert.assertTrue(hit.get(VariantRec.VARIANT_TYPE).equals("duplication"));
			Assert.assertTrue(hit.get(VariantRec.GENE_NAME).equals("FLT3"));
			Assert.assertTrue(hit.get(VariantRec.CDOT).equals("c.1816_1831dup"));
			
		} catch (Exception ex) {
			ex.printStackTrace();
			Assert.assertTrue(false);
		}

	}
	
	public void testSnpEff() {

		Pipeline ppl;
                ppl = this.preparePipeline(inputFile);
                 
		try {

			ppl.initializePipeline();
			ppl.stopAllLogging();

			ppl.execute();

			//Grab the snpEff annotator - we'll take a look at the variants to make sure
			//they're ok
			SnpEffGeneAnnotate annotator = (SnpEffGeneAnnotate)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
			VariantPool vars = annotator.getVariants();
			Assert.assertTrue(vars.size() == 90);

			VariantRec var = vars.findRecord("20", 31022442, "-", "G");
			Assert.assertTrue(var != null);
			JSONArray snpeff_annos = var.getjsonProperty(VariantRec.SNPEFF_ALL);
			JSONObject hit = findJsonObj(snpeff_annos, "NM_015338.5.1");
			Assert.assertNotNull(hit);
			Assert.assertTrue(hit.has(VariantRec.VARIANT_TYPE));
			Assert.assertTrue(hit.get(VariantRec.VARIANT_TYPE).equals("frameshift_variant"));
			Assert.assertTrue(hit.get(VariantRec.GENE_NAME).equals("ASXL1"));
			Assert.assertTrue(hit.get(VariantRec.CDOT).equals("c.1934dupG"));
			
			var = vars.findRecord("13", 28602256, "C", "T");
			Assert.assertTrue(var != null);
			snpeff_annos = var.getjsonProperty(VariantRec.SNPEFF_ALL);
			hit = findJsonObj(snpeff_annos, "NM_004119.2.1");
			Assert.assertNotNull(hit);
			Assert.assertTrue(hit.get(VariantRec.VARIANT_TYPE).equals("intron_variant"));
			Assert.assertTrue(hit.get(VariantRec.GENE_NAME).equals("FLT3"));
			Assert.assertTrue(hit.get(VariantRec.CDOT).equals("c.2053+59G>A"));

		} catch (Exception ex) {
			ex.printStackTrace();
			Assert.assertTrue(false);
		} 

		ppl = this.preparePipeline(inputFile2);
		try {

			ppl.initializePipeline();
			ppl.stopAllLogging();

			ppl.execute();

			SnpEffGeneAnnotate annotator = (SnpEffGeneAnnotate)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
			VariantPool vars = annotator.getVariants();
			int x = vars.size();
			Assert.assertTrue(vars.size() == 6);

			VariantRec var = vars.findRecord("1", 24201919, "T", "C");
			Assert.assertTrue(var != null); 
			JSONArray snpeff_annos = var.getjsonProperty(VariantRec.SNPEFF_ALL);
			JSONObject hit = findJsonObj(snpeff_annos, "NM_001841.2.1");
			Assert.assertNotNull(hit);
			Assert.assertTrue(hit.get(VariantRec.VARIANT_TYPE).equals("synonymous_variant"));
			Assert.assertTrue(hit.get(VariantRec.GENE_NAME).equals("CNR2"));
			Assert.assertTrue(hit.get(VariantRec.CDOT).equals("c.189A>G"));


			var = vars.findRecord("1", 26582091, "G", "A");
			Assert.assertTrue(var != null);
			snpeff_annos = var.getjsonProperty(VariantRec.SNPEFF_ALL);
			hit = findJsonObj(snpeff_annos, "NM_022778.3.1");
			Assert.assertNotNull(hit);
			Assert.assertTrue(hit.get(VariantRec.VARIANT_TYPE).equals("missense_variant"));
			Assert.assertTrue(hit.get(VariantRec.GENE_NAME).equals("CEP85"));
			Assert.assertTrue(hit.get(VariantRec.CDOT).equals("c.638G>A"));
			Assert.assertTrue(hit.get(VariantRec.PDOT).equals("p.Ser213Asn"));

			var = vars.findRecord("1", 1900107, "-", "CTC");
			Assert.assertTrue(var != null); 
			snpeff_annos = var.getjsonProperty(VariantRec.SNPEFF_ALL);
			hit = findJsonObj(snpeff_annos, "NM_001304360.1.1");
			Assert.assertNotNull(hit);
			Assert.assertTrue(hit.get(VariantRec.VARIANT_TYPE).equals("conservative_inframe_insertion"));
			Assert.assertTrue(hit.get(VariantRec.GENE_NAME).equals("CFAP74"));
			Assert.assertTrue(hit.get(VariantRec.CDOT).equals("c.1212_1213insGAG"));
			Assert.assertTrue(hit.get(VariantRec.PDOT).equals("p.Lys404_Lys405insGlu"));
			

			var = vars.findRecord("1", 16464673, "G", "A");
			Assert.assertTrue(var != null);
			snpeff_annos = var.getjsonProperty(VariantRec.SNPEFF_ALL);
			hit = findJsonObj(snpeff_annos, "NM_004431.3.1");
			Assert.assertNotNull(hit);
			Assert.assertTrue(hit.get(VariantRec.VARIANT_TYPE).equals("synonymous_variant"));
			Assert.assertTrue(hit.get(VariantRec.GENE_NAME).equals("EPHA2"));
			Assert.assertTrue(hit.get(VariantRec.CDOT).equals("c.987C>T"));
			Assert.assertTrue(hit.get(VariantRec.PDOT).equals("p.Pro329Pro"));


			var = vars.findRecord("1", 47280747, "AT", "-");
			Assert.assertTrue(var != null);
			snpeff_annos = var.getjsonProperty(VariantRec.SNPEFF_ALL);
			hit = findJsonObj(snpeff_annos, "NM_000779.3.1");
			Assert.assertNotNull(hit);
			Assert.assertTrue(hit.get(VariantRec.VARIANT_TYPE).equals("frameshift_variant&splice_region_variant"));
			Assert.assertTrue(hit.get(VariantRec.GENE_NAME).equals("CYP4B1"));
			Assert.assertTrue(hit.get(VariantRec.CDOT).equals("c.881_882delAT"));
			Assert.assertTrue(hit.get(VariantRec.PDOT).equals("p.Asp294fs"));


		} catch (Exception ex) {
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace(System.err);
			Assert.assertFalse(true);
		}
         
		ppl = this.preparePipeline(inputFile3);
		try {
			//This tests the 'complex vars' file

			ppl.initializePipeline();
			ppl.stopAllLogging();

			ppl.execute();

			SnpEffGeneAnnotate annotator = (SnpEffGeneAnnotate)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
			VariantPool vars = annotator.getVariants();
			Assert.assertTrue(vars.size() == 10);

			VariantRec var = vars.findRecord("18", 48610383, "-", "CGCA");
			Assert.assertTrue(var != null); 
			JSONArray snpeff_annos = var.getjsonProperty(VariantRec.SNPEFF_ALL);
			JSONObject hit = findJsonObj(snpeff_annos, "NM_005359.5.1");
			Assert.assertNotNull(hit);
			Assert.assertTrue(hit.get(VariantRec.VARIANT_TYPE).equals("3_prime_UTR_variant"));
			Assert.assertTrue(hit.get(VariantRec.GENE_NAME).equals("SMAD4"));

			

			var = vars.findRecord("18", 48610383, "CACA", "-");
			Assert.assertTrue(var != null);
			snpeff_annos = var.getjsonProperty(VariantRec.SNPEFF_ALL);
			hit = findJsonObj(snpeff_annos, "NM_005359.5.1");
			Assert.assertNotNull(hit);
			Assert.assertTrue(hit.get(VariantRec.VARIANT_TYPE).equals("3_prime_UTR_variant"));
			Assert.assertTrue(hit.get(VariantRec.GENE_NAME).equals("SMAD4"));


		} catch (Exception ex) {
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace(System.err);
			Assert.assertFalse(true);
		} 

                ppl = this.preparePipeline(inputFile4);
                try {
                        // This tests the large structural variants
                        ppl.initializePipeline();
                        ppl.stopAllLogging();

                        ppl.execute();

                        SnpEffGeneAnnotate annotator = (SnpEffGeneAnnotate)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");
                        VariantPool vars = annotator.getVariants();
                        Assert.assertTrue(vars.size() == 9);
                        vars.listAll(System.err);

                        VariantRec var = vars.findRecord("9", 86588186, "TTT", "-");
                        Assert.assertTrue(var != null);
                        JSONArray snpeff_annos = var.getjsonProperty(VariantRec.SNPEFF_ALL);
                        //System.out.println(snpeff_annos);
                        JSONObject hit = findJsonObj(snpeff_annos, "NM_002140.3.1");
                        Assert.assertNotNull(hit);
                        Assert.assertTrue(hit.get(VariantRec.VARIANT_TYPE).equals("intron_variant"));
                        Assert.assertTrue(hit.get(VariantRec.GENE_NAME).equals("HNRNPK"));
                        Assert.assertTrue(hit.get(VariantRec.CDOT).equals("c.516+13_516+15delAAA"));

                        var = vars.findRecord("13", 28608224, "T", "<DUP:TANDEM>");
                        Assert.assertTrue( var != null );
                        Assert.assertTrue( var.getPropertyOrAnnotation(VariantRec.VAR_CALLER).equals("manta") );

                        var = vars.findRecord("MT", 743, "C", "<DEL>");
                        Assert.assertTrue(var != null);
                        Assert.assertTrue( var.getPropertyInt(VariantRec.SV_END) == 15784 );
                        Assert.assertTrue( var.getPropertyInt(VariantRec.INDEL_LENGTH) == 15041 );
                        snpeff_annos = var.getjsonProperty(VariantRec.SNPEFF_ALL);
                        //System.out.println(snpeff_annos);
                        hit = findJsonObj(snpeff_annos, "MT-RNR1");
                        Assert.assertNotNull(hit);
                        Assert.assertTrue(hit.get(VariantRec.CDOT).equals("n.744_15784del"));
                        Assert.assertTrue(hit.get(VariantRec.VARIANT_TYPE).equals("feature_ablation"));

                } catch (Exception ex) {
                        System.err.println("Exception during testing: " + ex.getLocalizedMessage());
                        ex.printStackTrace(System.err);
                        Assert.assertFalse(true);
                }

	}
}
