package annotation;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import buffer.variant.VariantPool;
import buffer.variant.VariantRec;
import junit.framework.TestCase;
import operator.variant.ExAC63KExomesAnnotator;
import pipeline.Pipeline;
import util.vcfParser.VCFParser;

// Test the ExAC 63k exomes annotator, using the first 1000 lines of the ExAC 63k VCF.

public class TestExAC63KExomesAnnotator extends TestCase {

	File inputVCFTemplate = new File("src/test/java/annotation/testExAC63k_VCF.xml");
	File inputCSVTemplate = new File("src/test/java/annotation/testExAC63k_CSV.xml");
	String propertiesFile = "src/test/java/core/inputFiles/testProperties.xml";
	
	Pipeline pplVCF;
	
	ExAC63KExomesAnnotator annotatorVCF;
	ExAC63KExomesAnnotator annotatorCSV;
	
	boolean thrown;
	
	public void setUp() {
		try {
			//Override default propertiesFile path if we have one specified on command line.
			Pipeline pplVCF = new Pipeline(inputVCFTemplate, propertiesFile);
			if (System.getProperty("pipelineProps") != null) { // This can be specified either in maven or in eclipse as a JVM argument in the run configuration: -DpipelineProps="/path/to/pipeline_properties.xml"
				System.out.println("Using Pipeline props file:" + System.getProperty("pipelineProps"));
				
				propertiesFile = System.getProperty("pipelineProps");
				pplVCF = new Pipeline(inputVCFTemplate, propertiesFile);
			} else {
				System.out.println("Using default toy test resources.");
				
				pplVCF = new Pipeline(inputVCFTemplate, propertiesFile);
				pplVCF.setProperty("63k.db.path", "src/test/java/testvcfs/ExAC.3.test.vcf.gz");
			}
			
			pplVCF.initializePipeline();
			pplVCF.stopAllLogging();
			pplVCF.execute();
			annotatorVCF = (ExAC63KExomesAnnotator) pplVCF.getObjectHandler().getObjectForLabel("ExACAnnotator");
			
			
		} catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testSizeOfVariantPoolFunctions() {
		//Test the size of the variant pool.
		VariantPool vars = annotatorVCF.getVariants();
		System.out.println(annotatorVCF.EXAC_63K_PATH);
		Assert.assertEquals(889, vars.size());
	}
	
	@Test
	public void testSingleAltVariants() {
		// Test VCF
		try {
			//1       17407   .       G       A       137942.94       PASS    AC=438;AC_AFR=30;AC_AMR=15;AC_Adj=398;AC_EAS=27;AC_FIN=3;AC_Het=398;AC_Hom=0;AC_NFE=266;AC_OTH=4;AC_SAS=53;AF=0.034;AN=13052;AN_AFR=990;AN_AMR=278;AN_Adj=5202;AN_EAS=402;AN_FIN=110;AN_NFE=2974;AN_OTH=56;AN_SAS=392;BaseQRankSum=2.38;ClippingRankSum=0.209;DP=197149;FS=0.000;GQ_MEAN=41.41;GQ_STDDEV=77.70;Het_AFR=30;Het_AMR=15;Het_EAS=27;Het_FIN=3;Het_NFE=266;Het_OTH=4;Het_SAS=53;Hom_AFR=0;Hom_AMR=0;Hom_EAS=0;Hom_FIN=0;Hom_NFE=0;Hom_OTH=0;Hom_SAS=0
			VariantRec var = new VariantRec("1", 17407, 17407, "G", "A");
			var = VCFParser.normalizeVariant(var);
			annotatorVCF.annotateVariant(var);
			
			//overall
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_COUNT).equals(Double.valueOf(398)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_NUMBER).equals(Double.valueOf(5202)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_OVERALL_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_FREQ), 0.07651, 0.001);

			//South Asian
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_COUNT).equals(Double.valueOf(53)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_NUMBER).equals(Double.valueOf(392)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_SOUTHASIAN_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_FREQ), 0.1352, 0.001);
			
			//Non-Finnish Europeans
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_COUNT).equals(Double.valueOf(266)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_NUMBER).equals(Double.valueOf(2974)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_FREQ), 0.08944, 0.001);
			
			//Other populations
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_COUNT).equals(Double.valueOf(4)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_NUMBER).equals(Double.valueOf(56)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_OTHER_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_FREQ), 0.07143, 0.001);

			//East Asian
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_COUNT).equals(Double.valueOf(27)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_NUMBER).equals(Double.valueOf(402)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EASTASIAN_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_FREQ), 0.06716, 0.001);

			//Latino/American
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_COUNT).equals(Double.valueOf(15)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_NUMBER).equals(Double.valueOf(278)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_LATINO_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_FREQ), 0.05396, 0.001);
			
			//African
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_COUNT).equals(Double.valueOf(30)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_NUMBER).equals(Double.valueOf(990)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_AFRICAN_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_FREQ), 0.0303, 0.001);
			
			//Finnish
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_COUNT).equals(Double.valueOf(3)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_NUMBER).equals(Double.valueOf(110)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EUR_FINNISH_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_FREQ), 0.02727, 0.001);
			
		} catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
		Assert.assertFalse(thrown);
	}

	@Test
	public void testMultiAltQuery() {
		try {						
			//1	1636400	rs144690660	G	A,C	1190692.03	PASS	AC=4484,262;AC_AFR=1254,52;AC_AMR=157,34;AC_Adj=3051,251;AC_EAS=372,11;AC_FIN=54,0;AC_Het=1506,184,53;AC_Hom=746,7;AC_NFE=442,32;AC_OTH=12,0;AC_SAS=760,122;AF=0.037,2.172e-03;
			//AN=120606;AN_AFR=8544;AN_AMR=11082;AN_Adj=115186;AN_EAS=8044;AN_FIN=6360;AN_NFE=65406;AN_OTH=872;AN_SAS=14878;BaseQRankSum=0.257;ClippingRankSum=-4.420e-01;DB;DP=2479243;FS=60.859;GQ_MEAN=75.48;GQ_STDDEV=36.33;Het_AFR=517,37,15;Het_AMR=122,31,1;Het_EAS=181,8,3;Het_FIN=38,0,0;Het_NFE=331,29,3;Het_OTH=6,0,0;Het_SAS=311,79,31;Hom_AFR=361,0;Hom_AMR=17,1;Hom_EAS=94,0;Hom_FIN=8,0;Hom_NFE=54,0;Hom_OTH=3,0;Hom_SAS=209,6
			VariantRec var2A1 = new VariantRec("1", 1636400, 1636400, "G", "A");
			var2A1 = VCFParser.normalizeVariant(var2A1);
			annotatorVCF.annotateVariant(var2A1);
			
			//overall
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_COUNT).equals(Double.valueOf(3051)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_NUMBER).equals(Double.valueOf(115186)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_OVERALL_HOM_COUNT).equals(Double.valueOf(746)));
			Assert.assertEquals(var2A1.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_FREQ), 0.02649, 0.001);

			//South Asian
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_COUNT).equals(Double.valueOf(760)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_NUMBER).equals(Double.valueOf(14878)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_SOUTHASIAN_HOM_COUNT).equals(Double.valueOf(209)));
			Assert.assertEquals(var2A1.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_FREQ), 0.05108, 0.001);

			//Non-Finnish Europeans 442 	65406 	54 	0.006758
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_COUNT).equals(Double.valueOf(442)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_NUMBER).equals(Double.valueOf(65406)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_HOM_COUNT).equals(Double.valueOf(54)));
			Assert.assertEquals(var2A1.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_FREQ), 0.006758, 0.001);

			//Other populations 12 	872 	3 	0.01376
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_COUNT).equals(Double.valueOf(12)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_NUMBER).equals(Double.valueOf(872)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_OTHER_HOM_COUNT).equals(Double.valueOf(3)));
			Assert.assertEquals(var2A1.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_FREQ), 0.01376, 0.001);

			//East Asian 372 	8044 	94 	0.04625
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_COUNT).equals(Double.valueOf(372)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_NUMBER).equals(Double.valueOf(8044)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_EASTASIAN_HOM_COUNT).equals(Double.valueOf(94)));
			Assert.assertEquals(var2A1.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_FREQ), 0.04625, 0.001);

			//Latino/American  	157 	11082 	17 	0.01417
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_COUNT).equals(Double.valueOf(157)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_NUMBER).equals(Double.valueOf(11082)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_LATINO_HOM_COUNT).equals(Double.valueOf(17)));
			Assert.assertEquals(var2A1.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_FREQ), 0.01417, 0.001);

			//African
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_COUNT).equals(Double.valueOf(1254)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_NUMBER).equals(Double.valueOf(8544)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_AFRICAN_HOM_COUNT).equals(Double.valueOf(361)));
			Assert.assertEquals(var2A1.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_FREQ), 0.1468, 0.001);

			//Finnish 54 	6360 	8 	0.008491
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_COUNT).equals(Double.valueOf(54)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_NUMBER).equals(Double.valueOf(6360)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXAC63K_EUR_FINNISH_HOM_COUNT).equals(Double.valueOf(8)));
			Assert.assertEquals(var2A1.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_FREQ), 0.008491, 0.001);
			
			
			//------------------ var2.A2 -----------------------------------------
			VariantRec var2A2 = new VariantRec("1", 1636400, 1636400, "G", "C");
			var2A2 = VCFParser.normalizeVariant(var2A2);
			annotatorVCF.annotateVariant(var2A2);

			//overall  	251 	115186 	7 	0.002179 
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_COUNT).equals(Double.valueOf(251)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_NUMBER).equals(Double.valueOf(115186)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_OVERALL_HOM_COUNT).equals(Double.valueOf(7)));
			Assert.assertEquals(var2A2.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_FREQ), 0.002179, 0.001);

			//South Asian 122 	14878 	6 	0.0082
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_COUNT).equals(Double.valueOf(122)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_NUMBER).equals(Double.valueOf(14878)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_SOUTHASIAN_HOM_COUNT).equals(Double.valueOf(6)));
			Assert.assertEquals(var2A2.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_FREQ), 0.0082, 0.001);

			//Non-Finnish Europeans 32 	65406 	0 	0.0004893
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_COUNT).equals(Double.valueOf(32)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_NUMBER).equals(Double.valueOf(65406)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var2A2.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_FREQ), 0.0004893, 0.001);

			//Other populations  	0 	872 	0 	0
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_NUMBER).equals(Double.valueOf(872)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_OTHER_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var2A2.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_FREQ), 0, 0.001);

			//East Asian  	11 	8044 	0 	0.001367
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_COUNT).equals(Double.valueOf(11)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_NUMBER).equals(Double.valueOf(8044)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_EASTASIAN_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var2A2.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_FREQ), 0.001367, 0.001);

			//Latino/American  	 	34 	11082 	1 	0.003068
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_COUNT).equals(Double.valueOf(34)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_NUMBER).equals(Double.valueOf(11082)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_LATINO_HOM_COUNT).equals(Double.valueOf(1)));
			Assert.assertEquals(var2A2.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_FREQ), 0.003068, 0.001);

			//African 52 	8544 	0 	0.006086
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_COUNT).equals(Double.valueOf(52)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_NUMBER).equals(Double.valueOf(8544)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_AFRICAN_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var2A2.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_FREQ), 0.006086, 0.001);

			//Finnish 0 	6360 	0 	0
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_NUMBER).equals(Double.valueOf(6360)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXAC63K_EUR_FINNISH_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var2A2.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_FREQ), 0, 0.001);
			
		} catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
		Assert.assertFalse(thrown);
	}
	
	@Test
	public void testHemi() {
		try {
			//X       100276147       rs56176072      C       T       10110649.85     PASS    AC=5744;AC_AFR=78;AC_AMR=144;AC_Adj=4174;AC_EAS=0;AC_FIN=440;AC_Hemi=1520;AC_Het=2466;AC_Hom=94;AC_NFE=3380;AC_OTH=26;AC_SAS=106;AF=0.047;AN=121410;AN_AFR=8509;AN_AMR=9307;AN_Adj=87627;AN_EAS=6623;AN_FIN=4521;AN_NFE=47933;AN_OTH=632;AN_SAS=10102;BaseQRankSum=-7.130e-01;ClippingRankSum=-2.840e-01;DB;DP=1949974;FS=0.518;GQ_MEAN=125.43;GQ_STDDEV=301.48;Hemi_AFR=13;Hemi_AMR=33;Hemi_EAS=0;Hemi_FIN=188;Hemi_NFE=1207;Hemi_OTH=7;Hemi_SAS=72;Het_AFR=65;Het_AMR=105;Het_EAS=0;Het_FIN=230;Het_NFE=2015;Het_OTH=19;Het_SAS=32;Hom_AFR=0;Hom_AMR=3;Hom_EAS=0;Hom_FIN=11;Hom_NFE=79;Hom_OTH=0;Hom_SAS=1;
			VariantRec var = new VariantRec("X", 100276147, 100276147, "C", "T");
			var = VCFParser.normalizeVariant(var);
			annotatorVCF.annotateVariant(var);
			
			//overall  	 	4174 	87627 	94 	1520 	0.04763
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_COUNT).equals(Double.valueOf(4174)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_NUMBER).equals(Double.valueOf(87627)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_OVERALL_HOM_COUNT).equals(Double.valueOf(94)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_FREQ), 0.04763, 0.001);

			//South Asian 106 	10102 	1 	72 	0.01049
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_COUNT).equals(Double.valueOf(106)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_NUMBER).equals(Double.valueOf(10102)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_SOUTHASIAN_HOM_COUNT).equals(Double.valueOf(1)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_FREQ), 0.01049, 0.001);

			//Non-Finnish Europeans 3380 	47933 	79 	1207 	0.07052
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_COUNT).equals(Double.valueOf(3380)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_NUMBER).equals(Double.valueOf(47933)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_HOM_COUNT).equals(Double.valueOf(79)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_FREQ), 0.07052, 0.001);

			//Other populations  	26 	632 	0 	7 	0.04114
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_COUNT).equals(Double.valueOf(26)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_NUMBER).equals(Double.valueOf(632)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_OTHER_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_FREQ), 0.04114, 0.001);

			//East Asian  	0 	6623 	0 	0 	0
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_NUMBER).equals(Double.valueOf(6623)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EASTASIAN_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_FREQ), 0, 0.001);

			//Latino/American  	 	144 	9307 	3 	33 	0.01547
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_COUNT).equals(Double.valueOf(144)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_NUMBER).equals(Double.valueOf(9307)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_LATINO_HOM_COUNT).equals(Double.valueOf(3)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_FREQ), 0.01547, 0.001);

			//African  	78 	8509 	0 	13 	0.009167
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_COUNT).equals(Double.valueOf(78)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_NUMBER).equals(Double.valueOf(8509)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_AFRICAN_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_FREQ), 0.009167, 0.001);

			//Finnish 440 	4521 	11 	188 	0.09732
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_COUNT).equals(Double.valueOf(440)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_NUMBER).equals(Double.valueOf(4521)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EUR_FINNISH_HOM_COUNT).equals(Double.valueOf(11)));
			Assert.assertEquals(var.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_FREQ), 0.09732, 0.001);

			//X specific checks
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_OVERALL_HEMI_COUNT).equals(Double.valueOf(1520)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_SOUTHASIAN_HEMI_COUNT).equals(Double.valueOf(72)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_HEMI_COUNT).equals(Double.valueOf(1207)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_OTHER_HEMI_COUNT).equals(Double.valueOf(7)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EASTASIAN_HEMI_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_LATINO_HEMI_COUNT).equals(Double.valueOf(33)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_AFRICAN_HEMI_COUNT).equals(Double.valueOf(13)));
			Assert.assertTrue(var.getProperty(VariantRec.EXAC63K_EUR_FINNISH_HEMI_COUNT).equals(Double.valueOf(188)));
			
		}  catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
		Assert.assertFalse(thrown);
	}
	
	
	@Test
	public void testMultipleAltHemi() {
		try {
			//X       100083015       rs141539257     C       T,A     163996.46       PASS    AC=138,7;AC_AFR=114,0;AC_AMR=3,0;AC_Adj=117,5;AC_EAS=0,0;AC_FIN=0,0;AC_Hemi=15,2;AC_Het=98,3,0;AC_Hom=2,0;AC_NFE=0,5;AC_OTH=0,0;AC_SAS=0,0;AF=1.137e-03,5.766e-05;AN=121404;AN_AFR=8328;AN_AMR=9028;AN_Adj=83644;AN_EAS=6523;AN_FIN=4328;AN_NFE=46005;AN_OTH=595;AN_SAS=8837;BaseQRankSum=-1.580e-01;ClippingRankSum=-1.560e-01;DB;DP=1593387;FS=1.982;GQ_MEAN=63.15;GQ_STDDEV=48.54;Hemi_AFR=14,0;Hemi_AMR=1,0;Hemi_EAS=0,0;Hemi_FIN=0,0;Hemi_NFE=0,2;Hemi_OTH=0,0;Hemi_SAS=0,0;Het_AFR=96,0,0;Het_AMR=2,0,0;Het_EAS=0,0,0;Het_FIN=0,0,0;Het_NFE=0,3,0;Het_OTH=0,0,0;Het_SAS=0,0,0;Hom_AFR=2,0;Hom_AMR=0,0;Hom_EAS=0,0;Hom_FIN=0,0;Hom_NFE=0,0;Hom_OTH=0,0;Hom_SAS=0,0;InbreedingCoeff=0.2339;
			VariantRec varA1 = new VariantRec("X", 100083015, 100276147, "C", "T");
			varA1 = VCFParser.normalizeVariant(varA1);
			annotatorVCF.annotateVariant(varA1);
			
			//overall  	 	117 	83644 	2 	15 	0.001399 
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_COUNT).equals(Double.valueOf(117)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_NUMBER).equals(Double.valueOf(83644)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_OVERALL_HOM_COUNT).equals(Double.valueOf(2)));
			Assert.assertEquals(varA1.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_FREQ), 0.001399, 0.001);

			//South Asian 0 	8837 	0 	0 	0
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_NUMBER).equals(Double.valueOf(8837)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_SOUTHASIAN_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(varA1.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_FREQ), 0, 0.001);

			//Non-Finnish Europeans 0 	46005 	0 	0 	0
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_NUMBER).equals(Double.valueOf(46005)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(varA1.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_FREQ), 0, 0.001);

			//Other populations  	0 	595 	0 	0 	0
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_NUMBER).equals(Double.valueOf(595)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_OTHER_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(varA1.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_FREQ), 0, 0.001);

			//East Asian  	 	0 	6523 	0 	0 	0
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_NUMBER).equals(Double.valueOf(6523)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_EASTASIAN_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(varA1.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_FREQ), 0, 0.001);

			//Latino/American  	 	3 	9028 	0 	1 	0.0003323
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_COUNT).equals(Double.valueOf(3)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_NUMBER).equals(Double.valueOf(9028)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_LATINO_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(varA1.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_FREQ), 0.0003323, 0.001);

			//African  	114 	8328 	2 	14 	0.01369
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_COUNT).equals(Double.valueOf(114)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_NUMBER).equals(Double.valueOf(8328)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_AFRICAN_HOM_COUNT).equals(Double.valueOf(2)));
			Assert.assertEquals(varA1.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_FREQ), 0.01369, 0.001);

			//Finnish 0 	4328 	0 	0 	0
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_NUMBER).equals(Double.valueOf(4328)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_EUR_FINNISH_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(varA1.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_FREQ), 0, 0.001);

			//X specific checks
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_OVERALL_HEMI_COUNT).equals(Double.valueOf(15)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_SOUTHASIAN_HEMI_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_HEMI_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_OTHER_HEMI_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_EASTASIAN_HEMI_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_LATINO_HEMI_COUNT).equals(Double.valueOf(1)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_AFRICAN_HEMI_COUNT).equals(Double.valueOf(14)));
			Assert.assertTrue(varA1.getProperty(VariantRec.EXAC63K_EUR_FINNISH_HEMI_COUNT).equals(Double.valueOf(0)));

			
			//---------- varA2 ---------------------------------------------------
			VariantRec varA2 = new VariantRec("X", 100083015, 100276147, "C", "A");
			varA2 = VCFParser.normalizeVariant(varA2);
			annotatorVCF.annotateVariant(varA2);
			
			//overall  	 	5 	83644 	0 	2 	0.00005978 
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_COUNT).equals(Double.valueOf(5)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_NUMBER).equals(Double.valueOf(83644)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_OVERALL_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(varA2.getProperty(VariantRec.EXAC63K_OVERALL_ALLELE_FREQ), 0.00005978, 0.001);

			//South Asian 0 	8837 	0 	0 	0
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_NUMBER).equals(Double.valueOf(8837)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_SOUTHASIAN_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(varA2.getProperty(VariantRec.EXAC63K_SOUTHASIAN_ALLELE_FREQ), 0, 0.001);

			//Non-Finnish Europeans 5 	46005 	0 	2 	0.0001087
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_COUNT).equals(Double.valueOf(5)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_NUMBER).equals(Double.valueOf(46005)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(varA2.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_FREQ), 0.0001087, 0.001);

			//Other populations  	 	0 	595 	0 	0 	0
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_NUMBER).equals(Double.valueOf(595)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_OTHER_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(varA2.getProperty(VariantRec.EXAC63K_OTHER_ALLELE_FREQ), 0, 0.001);

			//East Asian  	 	0 	6523 	0 	0 	0
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_NUMBER).equals(Double.valueOf(6523)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_EASTASIAN_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(varA2.getProperty(VariantRec.EXAC63K_EASTASIAN_ALLELE_FREQ), 0, 0.001);

			//Latino/American  	 	 	0 	9028 	0 	0 	0
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_NUMBER).equals(Double.valueOf(9028)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_LATINO_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(varA2.getProperty(VariantRec.EXAC63K_LATINO_ALLELE_FREQ), 0.0003323, 0.001);

			//African  	0 	8328 	0 	0 	0
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_NUMBER).equals(Double.valueOf(8328)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_AFRICAN_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(varA2.getProperty(VariantRec.EXAC63K_AFRICAN_ALLELE_FREQ), 0, 0.001);

			//Finnish 0 	4328 	0 	0 	0
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_NUMBER).equals(Double.valueOf(4328)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_EUR_FINNISH_HOM_COUNT).equals(Double.valueOf(0)));
			Assert.assertEquals(varA2.getProperty(VariantRec.EXAC63K_EUR_FINNISH_ALLELE_FREQ), 0, 0.001);

			//X specific checks
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_OVERALL_HEMI_COUNT).equals(Double.valueOf(2)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_SOUTHASIAN_HEMI_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_EUR_NONFINNISH_HEMI_COUNT).equals(Double.valueOf(2)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_OTHER_HEMI_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_EASTASIAN_HEMI_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_LATINO_HEMI_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_AFRICAN_HEMI_COUNT).equals(Double.valueOf(0)));
			Assert.assertTrue(varA2.getProperty(VariantRec.EXAC63K_EUR_FINNISH_HEMI_COUNT).equals(Double.valueOf(0)));

			
		}  catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
		Assert.assertFalse(thrown);
	}
	
}