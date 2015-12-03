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
				pplVCF.setProperty("63k.db.path", "src/test/java/testvcfs/ExAC.r0.2.sites.vep_1000lines.vcf.gz");

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
			
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_FREQ).equals(0.034));
			
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_HOM_FREQ).equals(0.0));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_HOM_COUNT).equals(0.0));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_HET_FREQ).equals(398.0/5202.0/2.0));
			
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_AFR_FREQ).equals((double) 30/(double) 1000));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_AMR_FREQ).equals((double) 16/(double) 280));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_EAS_FREQ).equals((double) 29/(double) 412));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_FIN_FREQ).equals((double) 3/(double) 112));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_NFE_FREQ).equals((double) 269/(double) 2990));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_SAS_FREQ).equals((double) 54/(double) 394));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_OTH_FREQ).equals((double) 4/(double) 56));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_HET_FREQ).equals((double) 405 /(double) 13316));
			
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_AFR_HET).equals((double) 30/(double) 1000));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_AMR_HET).equals((double) 16/(double) 280));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_EAS_HET).equals((double) 29/(double) 412));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_FIN_HET).equals((double) 3/(double) 112));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_NFE_HET).equals((double) 269/(double) 2990));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_SAS_HET).equals((double) 54/(double) 394));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_OTH_HET).equals((double) 4/(double) 56));
			
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_AFR_HOM).equals(0.0));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_AMR_HOM).equals(0.0));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_EAS_HOM).equals(0.0));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_FIN_HOM).equals(0.0));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_NFE_HOM).equals(0.0));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_SAS_HOM).equals(0.0));
			Assert.assertTrue(var.getProperty(VariantRec.EXOMES_63K_OTH_HOM).equals(0.0));
			
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
			//multi-alt SNP
			// 1	877645	.	T	G,C	11199.11	PASS	AC=71,1; AC_Het=24,1,0; AC_Hom=0,0; ; AC_AFR=0,1; AC_AMR=1,0; AC_Adj=24,1; AC_EAS=0,0; AC_FIN=1,0; AC_NFE=21,0; AC_OTH=0,0; AC_SAS=1,0; ; AF=6.486e-04,9.135e-06; AN=109472; ; AN_AFR=678; AN_AMR=1440; AN_Adj=9890; AN_EAS=974; AN_FIN=266; AN_NFE=5784; AN_OTH=50; AN_SAS=698; ; Het_AFR=0,1,0; Het_AMR=1,0,0; Het_EAS=0,0,0; Het_FIN=1,0,0; Het_NFE=21,0,0; Het_OTH=0,0,0; Het_SAS=1,0,0; ; Hom_AFR=0,0; Hom_AMR=0,0; Hom_EAS=0,0; Hom_FIN=0,0; Hom_NFE=0,0; Hom_OTH=0,0; Hom_SAS=0,0
			VariantRec var1 = new VariantRec("1", 877645, 877645, "T", "G");
			var1 = VCFParser.normalizeVariant(var1);
			System.out.println(var1);
			annotatorVCF.annotateVariant(var1);
			
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_FREQ).equals(Double.parseDouble("6.486e-04")));
			
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_HOM_FREQ).equals(0.0));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_HOM_COUNT).equals(0.0));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_HET_FREQ).equals(24.0/9890.0));
			
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_AFR_FREQ).equals(0.0));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_AMR_FREQ).equals(1.0/1440.0));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_EAS_FREQ).equals(0.0));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_FIN_FREQ).equals(1.0/266.0));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_NFE_FREQ).equals(21.0/5784.0));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_OTH_FREQ).equals(0.0/50.0));
			
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_AFR_HET).equals(0.0));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_AMR_HET).equals((double) 1/(double) 1440));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_EAS_HET).equals(0.0));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_FIN_HET).equals((double) 1/(double) 266));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_NFE_HET).equals((double) 21/(double) 5784));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_SAS_HET).equals((double) 1/(double) 698));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_OTH_HET).equals(0.0));
			
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_AFR_HOM).equals(0.0));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_AMR_HOM).equals(0.0));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_EAS_HOM).equals(0.0));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_FIN_HOM).equals(0.0));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_NFE_HOM).equals(0.0));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_SAS_HOM).equals(0.0));
			Assert.assertTrue(var1.getProperty(VariantRec.EXOMES_63K_OTH_HOM).equals(0.0));
			
			//Begin Testing the Version 3 ExAC database
			
			// Somewhat common multi-alt
			//1	1636400	rs144690660	G	A,C	1190692.03	PASS	AC=4484,262;AC_AFR=1254,52;AC_AMR=157,34;AC_Adj=3051,251;AC_EAS=372,11;AC_FIN=54,0;AC_Het=1506,184,53;AC_Hom=746,7;AC_NFE=442,32;AC_OTH=12,0;AC_SAS=760,122;AF=0.037,2.172e-03;
			//AN=120606;AN_AFR=8544;AN_AMR=11082;AN_Adj=115186;AN_EAS=8044;AN_FIN=6360;AN_NFE=65406;AN_OTH=872;AN_SAS=14878;BaseQRankSum=0.257;ClippingRankSum=-4.420e-01;DB;DP=2479243;FS=60.859;GQ_MEAN=75.48;GQ_STDDEV=36.33;Het_AFR=517,37,15;Het_AMR=122,31,1;Het_EAS=181,8,3;Het_FIN=38,0,0;Het_NFE=331,29,3;Het_OTH=6,0,0;Het_SAS=311,79,31;Hom_AFR=361,0;Hom_AMR=17,1;Hom_EAS=94,0;Hom_FIN=8,0;Hom_NFE=54,0;Hom_OTH=3,0;Hom_SAS=209,6
			VariantRec var2A1 = new VariantRec("1", 1636400, 1636400, "G", "A");
			var2A1 = VCFParser.normalizeVariant(var2A1);
			annotatorVCF.annotateVariant(var2A1);
			
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_FREQ).equals(0.037));
			
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_HOM_FREQ).equals( (double) 746/(115186/2)));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_HOM_COUNT).equals(746.0));
			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_HET_FREQ).equals((double) 1506/(115186)));
//			
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_AFR_FREQ).equals(0.0));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_AMR_FREQ).equals(1.0/1440.0));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_EAS_FREQ).equals(0.0));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_FIN_FREQ).equals(1.0/266.0));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_NFE_FREQ).equals(21.0/5784.0));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_OTH_FREQ).equals(0.0/50.0));
//			
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_AFR_HET).equals(0.0));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_AMR_HET).equals((double) 1/(double) 1440));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_EAS_HET).equals(0.0));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_FIN_HET).equals((double) 1/(double) 266));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_NFE_HET).equals((double) 21/(double) 5784));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_SAS_HET).equals((double) 1/(double) 698));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_OTH_HET).equals(0.0));
//			
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_AFR_HOM).equals(0.0));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_AMR_HOM).equals(0.0));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_EAS_HOM).equals(0.0));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_FIN_HOM).equals(0.0));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_NFE_HOM).equals(0.0));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_SAS_HOM).equals(0.0));
//			Assert.assertTrue(var2A1.getProperty(VariantRec.EXOMES_63K_OTH_HOM).equals(0.0));
			
			
			//var2.A2
			VariantRec var2A2 = new VariantRec("1", 1636400, 1636400, "G", "C");
			var2A2 = VCFParser.normalizeVariant(var2A2);
			annotatorVCF.annotateVariant(var2A2);
			
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_FREQ).equals(Double.parseDouble("2.172e-03")));
			
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_HOM_FREQ).equals((double) 7/(120606/2)));
			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_HOM_COUNT).equals(7.0));
			//Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_HET_FREQ).equals((double) 184/(120606/2)));
//			
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_AFR_FREQ).equals(0.0));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_AMR_FREQ).equals(1.0/1440.0));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_EAS_FREQ).equals(0.0));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_FIN_FREQ).equals(1.0/266.0));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_NFE_FREQ).equals(21.0/5784.0));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_OTH_FREQ).equals(0.0/50.0));
//			
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_AFR_HET).equals(0.0));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_AMR_HET).equals((double) 1/(double) 1440));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_EAS_HET).equals(0.0));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_FIN_HET).equals((double) 1/(double) 266));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_NFE_HET).equals((double) 21/(double) 5784));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_SAS_HET).equals((double) 1/(double) 698));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_OTH_HET).equals(0.0));
//			
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_AFR_HOM).equals(0.0));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_AMR_HOM).equals(0.0));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_EAS_HOM).equals(0.0));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_FIN_HOM).equals(0.0));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_NFE_HOM).equals(0.0));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_SAS_HOM).equals(0.0));
//			Assert.assertTrue(var2A2.getProperty(VariantRec.EXOMES_63K_OTH_HOM).equals(0.0));
			
		} catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
		Assert.assertFalse(thrown);
	}
	
	@Test
	public void testHemi() {
		//X       100276147       rs56176072      C       T       10110649.85     PASS    AC=5744;AC_AFR=78;AC_AMR=144;AC_Adj=4174;AC_EAS=0;AC_FIN=440;AC_Hemi=1520;AC_Het=2466;AC_Hom=94;AC_NFE=3380;AC_OTH=26;AC_SAS=106;AF=0.047;AN=121410;AN_AFR=8509;AN_AMR=9307;AN_Adj=87627;AN_EAS=6623;AN_FIN=4521;AN_NFE=47933;AN_OTH=632;AN_SAS=10102;BaseQRankSum=-7.130e-01;ClippingRankSum=-2.840e-01;DB;DP=1949974;FS=0.518;GQ_MEAN=125.43;GQ_STDDEV=301.48;Hemi_AFR=13;Hemi_AMR=33;Hemi_EAS=0;Hemi_FIN=188;Hemi_NFE=1207;Hemi_OTH=7;Hemi_SAS=72;Het_AFR=65;Het_AMR=105;Het_EAS=0;Het_FIN=230;Het_NFE=2015;Het_OTH=19;Het_SAS=32;Hom_AFR=0;Hom_AMR=3;Hom_EAS=0;Hom_FIN=11;Hom_NFE=79;Hom_OTH=0;Hom_SAS=1;
	}
	
	
	@Test
	public void testMultipleAltHemi() {
		//X       100083015       rs141539257     C       T,A     163996.46       PASS    AC=138,7;AC_AFR=114,0;AC_AMR=3,0;AC_Adj=117,5;AC_EAS=0,0;AC_FIN=0,0;AC_Hemi=15,2;AC_Het=98,3,0;AC_Hom=2,0;AC_NFE=0,5;AC_OTH=0,0;AC_SAS=0,0;AF=1.137e-03,5.766e-05;AN=121404;AN_AFR=8328;AN_AMR=9028;AN_Adj=83644;AN_EAS=6523;AN_FIN=4328;AN_NFE=46005;AN_OTH=595;AN_SAS=8837;BaseQRankSum=-1.580e-01;ClippingRankSum=-1.560e-01;DB;DP=1593387;FS=1.982;GQ_MEAN=63.15;GQ_STDDEV=48.54;Hemi_AFR=14,0;Hemi_AMR=1,0;Hemi_EAS=0,0;Hemi_FIN=0,0;Hemi_NFE=0,2;Hemi_OTH=0,0;Hemi_SAS=0,0;Het_AFR=96,0,0;Het_AMR=2,0,0;Het_EAS=0,0,0;Het_FIN=0,0,0;Het_NFE=0,3,0;Het_OTH=0,0,0;Het_SAS=0,0,0;Hom_AFR=2,0;Hom_AMR=0,0;Hom_EAS=0,0;Hom_FIN=0,0;Hom_NFE=0,0;Hom_OTH=0,0;Hom_SAS=0,0;InbreedingCoeff=0.2339;
	}
	
}

