package annotation;

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;
import operator.variant.COSMICAnnotatorTabix;
import pipeline.Pipeline;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;




public class TestCOSMIC extends TestCase{
		
		/**
		 * Testing the COSMICAnnotatorTabix. Currently (11/10/2014) using the myeloid.vcf and the first 500 lines of
		 * the CosmicCodingMuts VCF plus several random lines from the gzipped files.
		 * 
		 * 
		 * NOTE: In cases where you are checking indels, you will need to increment the pos +1
		 * 	and strip off the first base in the ref and alt. If only a single base, put a dash.
		 */
		
		
		File inputFile = new File("src/test/java/annotation/testCOSMIC.xml");
		File propertiesFile = new File("src/test/java/core/inputFiles/testProperties.xml");

		
		public void testCOSMIC() {
			

			try {
				Pipeline ppl = new Pipeline(inputFile, propertiesFile.getAbsolutePath());
				
				ppl.setProperty("cosmic.db.path", "src/test/java/testvcfs/Cosmic_1000lines.vcf.gz");

				ppl.initializePipeline();
				ppl.stopAllLogging();

				ppl.execute();

				COSMICAnnotatorTabix annotator = (operator.variant.COSMICAnnotatorTabix)ppl.getObjectHandler().getObjectForLabel("GeneAnnotate");

				VariantPool vars = annotator.getVariants();
				
				//17	7579472	COSM250061	G	C	.	.	GENE=TP53;STRAND=-;CDS=c.215C>G;AA=p.P72R;CNT=5
				VariantRec var2 = vars.findRecord("17", 7579472, "G" , "C");
				Assert.assertTrue(var2 != null);
				Assert.assertTrue(var2.getAnnotation(VariantRec.COSMIC_ID).contains("COSM250061"));
				Assert.assertTrue(var2.getProperty(VariantRec.COSMIC_COUNT).equals(5.0));
				
				//3	128204951	COSM445531	C	T
				VariantRec var = vars.findRecord("3", 128204951, "C" , "T");
				Assert.assertTrue(var != null);
				Assert.assertTrue(var.getProperty(VariantRec.COSMIC_COUNT).equals(3.0));
				Assert.assertTrue(var.getAnnotation(VariantRec.COSMIC_ID).contains("COSM445531"));

				//20	31022441	COSM1411076	A	AG	.	.	GENE=ASXL1;STRAND=+;CDS=c.1926_1927insG;AA=p.G646fs*12;CNT=1
				VariantRec var3 = vars.findRecord("20", 31022442, "-" , "G");
				Assert.assertTrue(var3 != null);
				Assert.assertTrue(var3.getAnnotation(VariantRec.COSMIC_ID).contains("COSM1411076"));
				Assert.assertTrue(var3.getProperty(VariantRec.COSMIC_COUNT).equals(1.0));

				//7	148543693	COSM1735881	TA	T	.	.	GENE=EZH2_ENST00000350995;STRAND=-;CDS=c.118-4delt;AA=p.?;CNT=1
				VariantRec var4 = vars.findRecord("7", 148543694, "A" , "-");
				Assert.assertTrue(var4 != null);
				Assert.assertTrue(var4.getProperty(VariantRec.COSMIC_COUNT).equals(1.0));
				Assert.assertTrue(var4.getAnnotation(VariantRec.COSMIC_ID).contains("COSM1735881"));
			
				//1	115258747	COSM564	C	T	.	.	GENE=NRAS;STRAND=-;CDS=c.35G>A;AA=p.G12D;CNT=462
				VariantRec var5 = vars.findRecord("1", 115258747, "C" , "T");
				Assert.assertTrue(var5 != null);
				Assert.assertTrue(var5.getProperty(VariantRec.COSMIC_COUNT).equals(462.0));
				Assert.assertTrue(var5.getAnnotation(VariantRec.COSMIC_ID).contains("COSM564"));
				
				//4	106196951	COSM3760322	A	G	.	.	GENE=TET2;STRAND=+;CDS=c.5284A>G;AA=p.I1762V;CNT=3
				VariantRec var6 = vars.findRecord("4", 106196951, "A" , "G");
				Assert.assertTrue(var6 != null);
				Assert.assertTrue(var6.getProperty(VariantRec.COSMIC_COUNT).equals(3.0));
				Assert.assertTrue(var6.getAnnotation(VariantRec.COSMIC_ID).contains("COSM3760322"));
				
			} catch (Exception ex) {
				System.err.println("Exception during testing: " + ex.getLocalizedMessage());
				ex.printStackTrace();
				Assert.assertTrue(false);
			}
			
		}
}

