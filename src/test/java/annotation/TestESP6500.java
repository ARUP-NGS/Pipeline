package annotation;

import java.io.File;

import junit.framework.TestCase;
import operator.OperationFailedException;
import operator.variant.ESP6500Annotator;

import org.broad.tribble.readers.TabixReader;

import org.junit.Assert;
import org.junit.Test;

import pipeline.Pipeline;
import util.vcfParser.VCFParser;
import buffer.variant.VariantRec;

public class TestESP6500 extends TestCase {
	File inputFile = new File("src/test/java/annotation/testESP6500.xml");
	File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml"); //do I need?
	ESP6500Annotator annotator;
        TabixReader reader;
	boolean thrown;

	public void setUp() {
		try {
			Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
			ppl.setProperty("esp.path", "src/test/java/testvcfs/ESP6500.test.vcf.gz");
			ppl.initializePipeline();
			ppl.stopAllLogging();
			ppl.execute();
			annotator = (ESP6500Annotator) ppl.getObjectHandler().getObjectForLabel("ESPAnnotator");
                        reader = new TabixReader((String)ppl.getProperty("esp.path"));

		} catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}

	@Test
	public void testMultiAltAnnotation() {
		try {

			/*
			 * 15	43678542	rs4608311	CGTATATATAT	CAT,CATAT,CATATAT,CATATATAT,C	.	PASS
			 * DBSNP=dbSNP_111;EA_AC=2122,81,79,106,12,5430;AA_AC=2669,141,49,146,30,537;
			 * TAC=4791,222,128,252,42,5967;MAF=30.6513,25.28,47.6671;
			 * GTS=A1A1,A1A2,A1A3,A1A4,A1A5,A1R,A2A2,A2A3,A2A4,A2A5,A2R,A3A3,A3A4,A3A5,A3R,A4A4,A4A5,A4R,A5A5,A5R,RR;
			 * EA_GTC=316,0,0,0,0,1490,17,0,0,0,47,21,1,0,36,20,0,65,1,10,1891;
			 * AA_GTC=1140,0,0,4,3,382,63,2,3,0,10,20,2,0,5,60,0,17,13,1,61;
			 * GTC=1456,0,0,4,3,1872,80,2,3,0,57,41,3,0,41,80,0,82,14,11,1952;DP=41;
			 * GL=TUBGCP4;CP=0.0;CG=-2.0;AA=.;CA=.;EXOME_CHIP=no;GWAS_PUBMED=.;
			 * FG=NM_014444.2:intron,NM_014444.2:intron,NM_014444.2:intron,NM_014444.2:intron,NM_014444.2:intron;
			 * HGVS_CDNA_VAR=NM_014444.2:c.1014+15_1014+24del10,NM_014444.2:c.1014+15_1014+16del11,NM_014444.2:c.1014+15_1014+18del11,NM_014444.2:c.1014+15_1014+20del11,NM_014444.2:c.1014+15_1014+22del11;
			 * HGVS_PROTEIN_VAR=.,.,.,.,.;CDS_SIZES=NM_014444.2:2001,NM_014444.2:2001,NM_014444.2:2001,NM_014444.2:2001,NM_014444.2:2001;
			 * GS=.,.,.,.,.;PH=.,.,.,.,.;EA_AGE=.;AA_AGE=.
			 */

			//A1
			VariantRec var1 = new VariantRec("15", 43678542, 43678542, "CGTATATATAT", "CAT");

			var1 = VCFParser.normalizeVariant(var1);
			annotator.annotateVariant(var1, reader);

			Assert.assertEquals(.306513, var1.getProperty(VariantRec.EXOMES_FREQ_EA), 0.0001);
			Assert.assertEquals(.2528, var1.getProperty(VariantRec.EXOMES_FREQ_AA), 0.0001);
			Assert.assertEquals(.476671, var1.getProperty(VariantRec.EXOMES_FREQ), 0.0001);

			Assert.assertEquals((1891.0/3915.0), var1.getProperty(VariantRec.EXOMES_EA_HOMREF), 0.0001);
			Assert.assertEquals((1490.0/3915.0), var1.getProperty(VariantRec.EXOMES_EA_HET), 0.0001);
			Assert.assertEquals((316.0/3915.0), var1.getProperty(VariantRec.EXOMES_EA_HOMALT), 0.0001);

			Assert.assertEquals((61.0/1786.0), var1.getProperty(VariantRec.EXOMES_AA_HOMREF), 0.0001);
			Assert.assertEquals((382.0/1786.0), var1.getProperty(VariantRec.EXOMES_AA_HET), 0.0001);
			Assert.assertEquals((1140.0/1786.0), var1.getProperty(VariantRec.EXOMES_AA_HOMALT), 0.0001);

			//A3
			VariantRec var2 = new VariantRec("15", 43678542, 43678542, "CGTATATATAT", "CATATAT"); //CATAT CATATAT

			var2 = VCFParser.normalizeVariant(var2);
			annotator.annotateVariant(var2, reader);

			Assert.assertEquals(.306513, var2.getProperty(VariantRec.EXOMES_FREQ_EA), 0.0001);
			Assert.assertEquals(.2528, var2.getProperty(VariantRec.EXOMES_FREQ_AA), 0.0001);
			Assert.assertEquals(.476671, var2.getProperty(VariantRec.EXOMES_FREQ), 0.0001);

			Assert.assertEquals((1891.0/3915.0), var2.getProperty(VariantRec.EXOMES_EA_HOMREF), 0.0001);
			Assert.assertEquals((36.0/3915.0), var2.getProperty(VariantRec.EXOMES_EA_HET), 0.0001);
			Assert.assertEquals((21.0/3915.0), var2.getProperty(VariantRec.EXOMES_EA_HOMALT), 0.0001);

			Assert.assertEquals((61.0/1786.0), var2.getProperty(VariantRec.EXOMES_AA_HOMREF), 0.0001);
			Assert.assertEquals((5.0/1786.0), var2.getProperty(VariantRec.EXOMES_AA_HET), 0.0001);
			Assert.assertEquals((20.0/1786.0), var2.getProperty(VariantRec.EXOMES_AA_HOMALT), 0.0001);
			Assert.assertEquals(((21 + 20.0)/(3915.0+1786.0)), var2.getProperty(VariantRec.EXOMES_HOM_FREQ), 0.0001);

			//A4
			VariantRec var3 = new VariantRec("15", 43678542, 43678542, "CGTATATATAT", "CATATATAT");

			var3 = VCFParser.normalizeVariant(var3);
			annotator.annotateVariant(var3, reader);

			Assert.assertEquals(.306513, var3.getProperty(VariantRec.EXOMES_FREQ_EA), 0.0001);
			Assert.assertEquals(.2528, var3.getProperty(VariantRec.EXOMES_FREQ_AA), 0.0001);
			Assert.assertEquals(.476671, var3.getProperty(VariantRec.EXOMES_FREQ), 0.0001);

			Assert.assertEquals((1891.0/3915.0), var3.getProperty(VariantRec.EXOMES_EA_HOMREF), 0.0001);
			Assert.assertEquals((65.0/3915.0), var3.getProperty(VariantRec.EXOMES_EA_HET), 0.0001);
			Assert.assertEquals((20.0/3915.0), var3.getProperty(VariantRec.EXOMES_EA_HOMALT), 0.0001);

			Assert.assertEquals((61.0/1786.0), var3.getProperty(VariantRec.EXOMES_AA_HOMREF), 0.0001);
			Assert.assertEquals((17.0/1786.0), var3.getProperty(VariantRec.EXOMES_AA_HET), 0.0001);
			Assert.assertEquals((60.0/1786.0), var3.getProperty(VariantRec.EXOMES_AA_HOMALT), 0.0001);
			Assert.assertEquals((80/(3915.0+1786.0)), var3.getProperty(VariantRec.EXOMES_HOM_FREQ), 0.0001);

		} catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
		Assert.assertFalse(thrown);
	}

	
	@Test
	public void testXChrSNPs() {
		/* X	154158158	rs371159191	T	C	.	PASS	DBSNP=dbSNP_138;EA_AC=1,6726;AA_AC=0,3835;TAC=1,10561;
		MAF=0.0149,0.0,0.0095;
		GTS=CC,CT,C,TT,T;
		EA_GTC=0,0,1,2428,1870;
		AA_GTC=0,0,0,1632,571;
		GTC=0,0,1,4060,2441;
		*/
		try {
			VariantRec var1 = new VariantRec("X", 154158158, 154158158, "T", "C");

			var1 = VCFParser.normalizeVariant(var1);
			annotator.annotateVariant(var1, reader);

			Assert.assertEquals(0.0149/100, var1.getProperty(VariantRec.EXOMES_FREQ_EA), 0.0000001);
			Assert.assertEquals(0.0, var1.getProperty(VariantRec.EXOMES_FREQ_AA), 0.0000001);
			Assert.assertEquals(0.0095/100, var1.getProperty(VariantRec.EXOMES_FREQ), 0.0000001);

			Assert.assertEquals((4298.0/(4299.0)), var1.getProperty(VariantRec.EXOMES_EA_HOMREF), 0.0001);
			Assert.assertEquals(0.0, var1.getProperty(VariantRec.EXOMES_EA_HET), 0.0001);
			Assert.assertEquals((1.0/(4299.0)), var1.getProperty(VariantRec.EXOMES_EA_HOMALT), 0.0001);

			Assert.assertEquals((2203.0/(2203.0)), var1.getProperty(VariantRec.EXOMES_AA_HOMREF), 0.0001);
			Assert.assertEquals(0.0, var1.getProperty(VariantRec.EXOMES_AA_HET), 0.0001);
			Assert.assertEquals(0.0, var1.getProperty(VariantRec.EXOMES_AA_HOMALT), 0.0001);
			Assert.assertEquals((1.0/(4299.0)), var1.getProperty(VariantRec.EXOMES_HOM_FREQ), 0.0001);

		} catch (Exception ex) {
			thrown = true;
			System.err.println("Exception during testing: " + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
		Assert.assertFalse(thrown);
	}

	@Test
	public void testSingleAltAnnotation() {
		//15      43678512        rs183013671     G       A       .       PASS    
		//DBSNP=dbSNP_135;EA_AC=2,8254;AA_AC=0,3834;TAC=2,12088;
		//MAF=0.0242,0.0,0.0165;
		//GTS=AA,AG,GG;   EA_GTC=0,2,4126;   AA_GTC=0,0,1917;
		//GTC=0,2,6043;DP=86;GL=TUBGCP4;CP=0.3;CG=5.7;AA=G;CA=.;EXOME_CHIP=yes;GWAS_PUBMED=.;FG=NM_014444.2:missense;
		//HGVS_CDNA_VAR=NM_014444.2:c.998G>A;HGVS_PROTEIN_VAR=NM_014444.2:p.(R333H);CDS_SIZES=NM_014444.2:2001;GS=29;PH=probably-damaging:0.996;EA_AGE=.;AA_AGE=.

		VariantRec var1 = new VariantRec("15", 43678512, 43678512, "G", "A");

		var1 = VCFParser.normalizeVariant(var1);
		try {
			annotator.annotateVariant(var1, reader);
		} catch (OperationFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Assert.assertEquals(0.0242/100, var1.getProperty(VariantRec.EXOMES_FREQ_EA), 0.0001);
		Assert.assertEquals(0.0, var1.getProperty(VariantRec.EXOMES_FREQ_AA), 0.0001);
		Assert.assertEquals(0.0165/100, var1.getProperty(VariantRec.EXOMES_FREQ), 0.0001);

		Assert.assertEquals((4126.0/4128.0), var1.getProperty(VariantRec.EXOMES_EA_HOMREF), 0.0001);
		Assert.assertEquals((2.0/4128.0), var1.getProperty(VariantRec.EXOMES_EA_HET), 0.0001);
		Assert.assertEquals((0.0/4128.0), var1.getProperty(VariantRec.EXOMES_EA_HOMALT), 0.0001);

		Assert.assertEquals((1917.0/1917.0), var1.getProperty(VariantRec.EXOMES_AA_HOMREF), 0.0001);
		Assert.assertEquals((0.0/1917.0), var1.getProperty(VariantRec.EXOMES_AA_HET), 0.0001);
		Assert.assertEquals((0.0/1917.0), var1.getProperty(VariantRec.EXOMES_AA_HOMALT), 0.0001);
	}
}


