package vcfLineParser;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import util.vcfParser.VCFParser;
import util.vcfParser.VCFParser.EntryType;
import util.vcfParser.VCFParser.HeaderEntry;
import buffer.variant.VariantRec;

public class TestVCFParser {

	File gatkVCF = new File("src/test/java/testvcfs/gatksingle.vcf");
	File freebayesVCF = new File("src/test/java/testvcfs/freebayes.single.vcf");

	File solidTumorVCF = new File("src/test/java/testvcfs/solid_tumor_test1.vcf");
	File bcrablVCF = new File("src/test/java/testvcfs/bcrabl.vcf");
	
	File complexVCF = new File("src/test/java/testvcfs/complexVars.vcf");
	File emptyVCF = new File("src/test/java/testvcfs/empty.vcf");
	
	File panelVCF = new File("examples/test_panel.vcf");
	
	@Test
	public void TestReadHeader() {
		
		try {
			VCFParser parser = new VCFParser(freebayesVCF);
			
			Assert.assertNotNull(parser.getCreator());
			Assert.assertTrue(parser.getCreator().startsWith("freeBayes"));
			
			Set<String> samples = parser.getSamples();
			Assert.assertTrue(samples.size() ==1);
			Assert.assertTrue(samples.iterator().next().equals("sample"));
			
			HeaderEntry entry = parser.getHeaderEntry("NS");
			Assert.assertNotNull(entry);
			Assert.assertTrue(entry.entryType == EntryType.INFO);
			Assert.assertTrue(entry.id.equals("NS"));
			Assert.assertTrue(entry.type.equals("Integer"));
			Assert.assertTrue(entry.number.equals("1"));
			
			entry = parser.getHeaderEntry("SAP");
			Assert.assertNotNull(entry);
			Assert.assertTrue(entry.entryType == EntryType.INFO);
			Assert.assertTrue(entry.id.equals("SAP"));
			Assert.assertTrue(entry.type.equals("Float"));
			
			entry = parser.getHeaderEntry("QR");
			Assert.assertNotNull(entry);
			Assert.assertTrue(entry.entryType == EntryType.FORMAT);
			Assert.assertTrue(entry.id.equals("QR"));
			Assert.assertTrue(entry.type.equals("Integer"));
			
			System.err.println("\tVCFParser tests on parsing header passed on single-sample FreeBayes VCF.");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
		
		try {
			VCFParser parser = new VCFParser(gatkVCF);
			
			Assert.assertNotNull(parser.getCreator());
			Assert.assertTrue(parser.getCreator().startsWith("GATK"));
			
			Set<String> samples = parser.getSamples();
			Assert.assertTrue(samples.size() ==1);
			Assert.assertTrue(samples.iterator().next().equals("sample"));
			
			HeaderEntry entry = parser.getHeaderEntry("MQ");
			Assert.assertNotNull(entry);
			Assert.assertTrue(entry.entryType == EntryType.INFO);
			Assert.assertTrue(entry.id.equals("MQ"));
			Assert.assertTrue(entry.type.equals("Float"));
			
			entry = parser.getHeaderEntry("AD");
			Assert.assertNotNull(entry);
			Assert.assertTrue(entry.entryType == EntryType.FORMAT);
			Assert.assertTrue(entry.id.equals("AD"));
			Assert.assertTrue(entry.type.equals("Integer"));
			
			entry = parser.getHeaderEntry("GT");
			Assert.assertNotNull(entry);
			Assert.assertTrue(entry.entryType == EntryType.FORMAT);
			Assert.assertTrue(entry.id.equals("GT"));
			Assert.assertTrue(entry.type.equals("String"));
			Assert.assertTrue(entry.number.equals("1"));
							
			System.err.println("\tVCFParser tests on parsing header passed on single-sample GATK VCF.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
	}
	
	@Test
	public void TestParseVariants() {
		// Don't strip matching bases
		try {
			VCFParser parser = new VCFParser(complexVCF);
			
			Assert.assertTrue(parser.isStripInitialMatchingBases()); //check default		
		
			parser.setStripInitialMatchingBases(false);
			Assert.assertFalse(parser.isStripInitialMatchingBases());
			
			
			int i=0;

			//Go through file
			while(parser.advanceLine() && i<5) {	
				VariantRec var = parser.toVariantRec();
				System.out.println(var.toSimpleString());		

				// Check second variant, first alt 
				if (i == 2) { //first variant has 2 alts
					Integer pos = parser.getPos();
					Assert.assertTrue(pos == 133978709);
					
					String ref = parser.getRef();
					Assert.assertTrue(ref.equals("CTC"));
					
					String alt = parser.getAlt();
					Assert.assertTrue(alt.equals("CTGTG"));					
					
					Boolean hetero = parser.isHetero();
					Assert.assertTrue(hetero);
							
					Boolean homo = parser.isHomo();
					Assert.assertFalse(homo);
							
					Boolean phase = parser.isPhased();
					Assert.assertFalse(phase);
												
					Integer depth = parser.getDepth();
					Assert.assertTrue(depth==13);

					Integer varDepth = parser.getVariantDepth();
					Assert.assertTrue(varDepth==7);
							
					Double genotypeQual = parser.getGenotypeQuality();
					Assert.assertTrue(genotypeQual.equals(-1.0));
							
					Double vqsr = parser.getVQSR();
					Assert.assertTrue(vqsr.equals(-1.0));
							
					Double fs = parser.getStrandBiasScore();
					Assert.assertTrue(fs.equals(-1.0));
							
					Double rp = parser.getRPScore();
					Assert.assertTrue(rp.equals(-1.0));
					
				}
				
				// Check first variant, second alt
				if (i == 3) {
					Integer pos = parser.getPos();
					Assert.assertTrue(pos == 133978709);
					
					String ref = parser.getRef();
					Assert.assertTrue(ref.equals("CTC"));
					
					String alt = parser.getAlt();
					Assert.assertTrue(alt.equals("CTG"));					
					
					Boolean hetero = parser.isHetero();
					Assert.assertTrue(hetero);
							
					Boolean homo = parser.isHomo();
					Assert.assertFalse(homo);
							
					Boolean phase = parser.isPhased();
					Assert.assertFalse(phase);
												
					Integer depth = parser.getDepth();
					Assert.assertTrue(depth==13);

					Integer varDepth = parser.getVariantDepth();
					Assert.assertTrue(varDepth==5);
				}
				
				i++;
										
			}
			
			System.err.println("\tVCFParser tests for parsing variants passed on a complex VCF (don't strip initial matching bases).");
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
		//Strip matching bases
		try {
			VCFParser parserStrip = new VCFParser(complexVCF);
										
			Assert.assertTrue(parserStrip.isStripInitialMatchingBases()); //check default

			parserStrip.setStripInitialMatchingBases(true);
			Assert.assertTrue(parserStrip.isStripInitialMatchingBases());
					
			int i=0;

			//Go through file
			while(parserStrip.advanceLine() && i<2) {	
				VariantRec var = parserStrip.toVariantRec();
				System.out.println(var.toSimpleString());		

				// Check second variant, first alt
				if (i == 2) { //first variant has 2 alts
					Boolean isVar = parserStrip.isVariant();
					Assert.assertTrue(isVar);
					
					Integer pos = parserStrip.getPos();
					Assert.assertTrue(pos == 133978711);
					
					String ref = parserStrip.getRef();
					Assert.assertTrue(ref.equals("C"));
					
					String alt = parserStrip.getAlt();
					Assert.assertTrue(alt.equals("GTG"));					
					
					Boolean hetero = parserStrip.isHetero();
					Assert.assertTrue(hetero);
							
					Boolean homo = parserStrip.isHomo();
					Assert.assertFalse(homo);
							
					Boolean phase = parserStrip.isPhased();
					Assert.assertFalse(phase);
												
					Integer depth = parserStrip.getDepth();
					Assert.assertTrue(depth==13);

					Integer varDepth = parserStrip.getVariantDepth();
					Assert.assertTrue(varDepth==7);
							
					Double genotypeQual = parserStrip.getGenotypeQuality();
					Assert.assertTrue(genotypeQual.equals(-1.0));
							
					Double vqsr = parserStrip.getVQSR();
					Assert.assertTrue(vqsr.equals(-1.0));
							
					Double fs = parserStrip.getStrandBiasScore();
					Assert.assertTrue(fs.equals(-1.0));
							
					Double rp = parserStrip.getRPScore();
					Assert.assertTrue(rp.equals(-1.0));
							
				}
						
				// Check first variant, second alt
				if (i == 3) {
					Integer pos = parserStrip.getPos();
					Assert.assertTrue(pos == 133978711);
					
					String ref = parserStrip.getRef();
					Assert.assertTrue(ref.equals("C"));
					
					String alt = parserStrip.getAlt();
					Assert.assertTrue(alt.equals("G"));					
					
					Boolean hetero = parserStrip.isHetero();
					Assert.assertTrue(hetero);
							
					Boolean homo = parserStrip.isHomo();
					Assert.assertFalse(homo);
							
					Boolean phase = parserStrip.isPhased();
					Assert.assertFalse(phase);
												
					Integer depth = parserStrip.getDepth();
					Assert.assertTrue(depth==13);

					Integer varDepth = parserStrip.getVariantDepth();
					Assert.assertTrue(varDepth==5);
				}
						
				i++;
												
			}
					
			System.err.println("\tVCFParser tests for parsing variants passed on a complex VCF (strip initial matching bases).");
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
				
		
		
		//Check fields for single-sample GATK
		try {
			VCFParser parserGATK = new VCFParser(gatkVCF);
			//Go through file
			int i=0;
			while(parserGATK.advanceLine() && i<1) {	
				VariantRec var = parserGATK.toVariantRec();
				
				// Check first variant, first alt
				if (i == 0) {
					Boolean hetero = parserGATK.isHetero();
					Assert.assertTrue(hetero);
					
					Boolean homo = parserGATK.isHomo();
					Assert.assertFalse(homo);
					
					Boolean phase = parserGATK.isPhased();
					Assert.assertFalse(phase);
					
					Integer depth = parserGATK.getDepth();
					Assert.assertTrue(depth==41);

					Integer varDepth = parserGATK.getVariantDepth();
					Assert.assertTrue(varDepth==15);
					
					Double genotypeQual = parserGATK.getGenotypeQuality();
					Assert.assertTrue(genotypeQual.equals(58.54));
					
					Double vqsr = parserGATK.getVQSR();
					Assert.assertTrue(vqsr.equals(-1.0));
					
					Double fs = parserGATK.getStrandBiasScore();
					Assert.assertTrue(fs.equals(0.0));
					
					Double rp = parserGATK.getRPScore();
					Assert.assertTrue(rp.equals(2.205));
				}
				i++;		

			}
			System.err.println("\tVCFParser tests for parsing variants passed on a single-sample GATK VCF.");
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
		//Check fields for single-sample FreeBayes
		try {
			VCFParser parserFB = new VCFParser(freebayesVCF);
			//Go through file
			int i=0;
	
			while(parserFB.advanceLine() && i<3) {	
				VariantRec var = parserFB.toVariantRec();

				// Check third variant
				if (i == 2) {
					Boolean hetero = parserFB.isHetero();
					Assert.assertTrue(hetero);

					Boolean homo = parserFB.isHomo();
					Assert.assertFalse(homo);
					
					Boolean phase = parserFB.isPhased();
					Assert.assertFalse(phase);
					
					Integer depth = parserFB.getDepth();
					Assert.assertTrue(depth==7);

					Integer varDepth = parserFB.getVariantDepth();
					Assert.assertTrue(varDepth==2);
							
					Double genotypeQual = parserFB.getGenotypeQuality();
					Assert.assertTrue(genotypeQual.equals(-1.0));
							
					Double vqsr = parserFB.getVQSR();
					Assert.assertTrue(vqsr.equals(-1.0));
					
					Double fs = parserFB.getStrandBiasScore();
					Assert.assertTrue(fs.equals(-1.0));
							
					Double rp = parserFB.getRPScore();
					Assert.assertTrue(rp.equals(-1.0));
				}
				i++;		

			}
			System.err.println("\tVCFParser tests for parsing variants passed on a single-sample FreeBayes VCF.");
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
		//Check fields for solid tumor VCF (Ion Torrent), don't strip trailing matching bases
		try {
			VCFParser parserTorr = new VCFParser(solidTumorVCF);
			
			Assert.assertTrue(parserTorr.isStripTrailingMatchingBases()); //check default		
		
			parserTorr.setStripTrailingMatchingBases(false);
			Assert.assertFalse(parserTorr.isStripTrailingMatchingBases());
			
			//Go through file
			int i=0;
			while(parserTorr.advanceLine() && i<3) {	
				VariantRec var = parserTorr.toVariantRec();

				// Check third variant
				if (i == 2) {
					Boolean hetero = parserTorr.isHetero();
					Assert.assertFalse(hetero);
							
					Boolean homo = parserTorr.isHomo();
					Assert.assertTrue(homo);
							
					Boolean phase = parserTorr.isPhased();
					Assert.assertFalse(phase);
							
					Integer depth = parserTorr.getDepth();
					Assert.assertTrue(depth==1119);

					Integer varDepth = parserTorr.getVariantDepth();
					Assert.assertTrue(varDepth==1119);
							
					Double genotypeQual = parserTorr.getGenotypeQuality();
					Assert.assertTrue(genotypeQual.equals(99.0));
						
					Double vqsr = parserTorr.getVQSR();
					Assert.assertTrue(vqsr.equals(-1.0));
							
					Double fs = parserTorr.getStrandBiasScore();
					Assert.assertTrue(fs.equals(0.5));
					
					Double rp = parserTorr.getRPScore();
					Assert.assertTrue(rp.equals(-1.0));
				}
				
				// Check last variant
				if (i == 20) {					
					String chrom = parserTorr.getContig();
					Assert.assertTrue(chrom.equals("11"));
					
					Integer pos = parserTorr.getPos();
					Assert.assertTrue(pos == 108225624);
						
					String ref = parserTorr.getRef();
					Assert.assertTrue(ref.equals("TATTTTTTTTC"));
						
					String alt = parserTorr.getAlt();
					Assert.assertTrue(alt.equals("ATTTTTTTC"));				
					
					Boolean hetero = parserTorr.isHetero();
					Assert.assertTrue(hetero);
							
					Boolean homo = parserTorr.isHomo();
					Assert.assertFalse(homo);
							
					Boolean phase = parserTorr.isPhased();
					Assert.assertFalse(phase);
							
					Integer depth = parserTorr.getDepth();
					Assert.assertTrue(depth==234);

					Integer varDepth = parserTorr.getVariantDepth();
					Assert.assertTrue(varDepth==220);
							
					Double genotypeQual = parserTorr.getGenotypeQuality();
					Assert.assertTrue(genotypeQual.equals(17.0));
						
					Double vqsr = parserTorr.getVQSR();
					Assert.assertTrue(vqsr.equals(-1.0));
							
					Double fs = parserTorr.getStrandBiasScore();
					Assert.assertTrue(fs.equals(0.5));
					
					Double rp = parserTorr.getRPScore();
					Assert.assertTrue(rp.equals(-1.0));
				}
				
				
				i++;		

			}
			System.err.println("\tVCFParser tests for parsing variants passed on a solid tumor VCF (Ion Torrent) (don't strip trailing matching bases).");
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
		//Check fields for solid tumor VCF (Ion Torrent), do strip trailing matching bases
		try {
			VCFParser parserTorrStrip = new VCFParser(solidTumorVCF);
			
			Assert.assertTrue(parserTorrStrip.isStripTrailingMatchingBases()); //check default		
			
			//Go through file
			int i=0;
			while(parserTorrStrip.advanceLine() && i<3) {	
				VariantRec var = parserTorrStrip.toVariantRec();

				// Check third variant
				if (i == 2) {
					Boolean hetero = parserTorrStrip.isHetero();
					Assert.assertFalse(hetero);
							
					Boolean homo = parserTorrStrip.isHomo();
					Assert.assertTrue(homo);
							
					Boolean phase = parserTorrStrip.isPhased();
					Assert.assertFalse(phase);
							
					Integer depth = parserTorrStrip.getDepth();
					Assert.assertTrue(depth==1119);

					Integer varDepth = parserTorrStrip.getVariantDepth();
					Assert.assertTrue(varDepth==1119);
							
					Double genotypeQual = parserTorrStrip.getGenotypeQuality();
					Assert.assertTrue(genotypeQual.equals(99.0));
						
					Double vqsr = parserTorrStrip.getVQSR();
					Assert.assertTrue(vqsr.equals(-1.0));
							
					Double fs = parserTorrStrip.getStrandBiasScore();
					Assert.assertTrue(fs.equals(0.5));
					
					Double rp = parserTorrStrip.getRPScore();
					Assert.assertTrue(rp.equals(-1.0));
				}
				
				// Check last variant
				if (i == 20) {		
					String chrom = parserTorrStrip.getContig();
					Assert.assertTrue(chrom.equals("11"));
					
					Integer pos = parserTorrStrip.getPos();
					Assert.assertTrue(pos == 108225624);
						
					String ref = parserTorrStrip.getRef();
					Assert.assertTrue(ref.equals("TAT"));
						
					String alt = parserTorrStrip.getAlt();
					Assert.assertTrue(alt.equals("A"));				
					
					Boolean hetero = parserTorrStrip.isHetero();
					Assert.assertTrue(hetero);
							
					Boolean homo = parserTorrStrip.isHomo();
					Assert.assertFalse(homo);
							
					Boolean phase = parserTorrStrip.isPhased();
					Assert.assertFalse(phase);
							
					Integer depth = parserTorrStrip.getDepth();
					Assert.assertTrue(depth==234);

					Integer varDepth = parserTorrStrip.getVariantDepth();
					Assert.assertTrue(varDepth==220);
							
					Double genotypeQual = parserTorrStrip.getGenotypeQuality();
					Assert.assertTrue(genotypeQual.equals(17.0));
						
					Double vqsr = parserTorrStrip.getVQSR();
					Assert.assertTrue(vqsr.equals(-1.0));
							
					Double fs = parserTorrStrip.getStrandBiasScore();
					Assert.assertTrue(fs.equals(0.5));
					
					Double rp = parserTorrStrip.getRPScore();
					Assert.assertTrue(rp.equals(-1.0));
				}					
							
				i++;		

			}
			System.err.println("\tVCFParser tests for parsing variants passed on a solid tumor VCF (Ion Torrent) (strip trailing matching bases).");
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
		//Check fields for bcr-abl VCF (Ion Torrent)
		try {
			VCFParser parserBCR = new VCFParser(bcrablVCF);
			//Go through file
			int i=0;
			while(parserBCR.advanceLine() && i<2) {	
				VariantRec var = parserBCR.toVariantRec();

				// Check second variant
				if (i == 1) {
					Boolean hetero = parserBCR.isHetero();
					Assert.assertTrue(hetero);
									
					Boolean homo = parserBCR.isHomo();
					Assert.assertFalse(homo);
									
					Boolean phase = parserBCR.isPhased();
					Assert.assertFalse(phase);
									
					Integer depth = parserBCR.getDepth();
					Assert.assertTrue(depth==1983);

					Integer varDepth = parserBCR.getVariantDepth();
					Assert.assertTrue(varDepth==444);
									
					Double genotypeQual = parserBCR.getGenotypeQuality();
					Assert.assertTrue(genotypeQual.equals(99.0));
								
					Double vqsr = parserBCR.getVQSR();
					Assert.assertTrue(vqsr.equals(-1.0));
									
					Double fs = parserBCR.getStrandBiasScore();
					Assert.assertTrue(fs.equals(0.518097));
							
					Double rp = parserBCR.getRPScore();
					Assert.assertTrue(rp.equals(-1.0));
				}
				i++;		

			}
			System.err.println("\tVCFParser tests for parsing variants passed on a bcr-abl VCF (Ion Torrent).");
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
				
		//Check fields for empty VCF 
		try {
			VCFParser parserEmpty = new VCFParser(emptyVCF);
			
			//Go through file			
			while(parserEmpty.advanceLine()) {	
				VariantRec var = parserEmpty.toVariantRec();
							
				Boolean hetero = parserEmpty.isHetero();
				Assert.assertFalse(hetero);
								
				Boolean homo = parserEmpty.isHomo();
				Assert.assertFalse(homo);
								
				Boolean phase = parserEmpty.isPhased();
				Assert.assertFalse(phase);
									
				Integer depth = parserEmpty.getDepth();
				Assert.assertTrue(depth==-1);

				Integer varDepth = parserEmpty.getVariantDepth();
				Assert.assertTrue(varDepth==-1);
								
				Double genotypeQual = parserEmpty.getGenotypeQuality();
				Assert.assertTrue(genotypeQual.equals(-1.0));
									
				Double vqsr = parserEmpty.getVQSR();
				Assert.assertTrue(vqsr.equals(-1.0));
									
				Double fs = parserEmpty.getStrandBiasScore();
				Assert.assertTrue(fs.equals(-1.0));
							
				Double rp = parserEmpty.getRPScore();
				Assert.assertTrue(rp.equals(-1.0));
			}
		
			System.err.println("\tVCFParser tests for parsing variants passed on an empty VCF.");
			} catch (IOException e) {
				e.printStackTrace();
				Assert.assertTrue(false);
			}
		}
	
	//test Hom calls 
	@Test
	public void TestHom() {				
		try {
			VCFParser parserPanel = new VCFParser(panelVCF);

			//Go through file
			int i=0;
			while(parserPanel.advanceLine() && i<17) {	
				VariantRec var = parserPanel.toVariantRec();

				// Check the first variant
				if (i == 0) {
					String chrom = parserPanel.getContig();
					Assert.assertTrue(chrom.equals("2"));
			
					Integer pos = parserPanel.getPos();
					Assert.assertTrue(pos == 227872182);
				
					String ref = parserPanel.getRef();
					Assert.assertTrue(ref.equals("G"));
				
					String alt = parserPanel.getAlt();
					Assert.assertTrue(alt.equals("A"));	
			
					Boolean hetero = parserPanel.isHetero();
					Assert.assertTrue(hetero);
							
					Boolean homo = parserPanel.isHomo();
					Assert.assertFalse(homo);
				}
				else if (i == 14) {
				// Check the fifteenth variant (hom)
						String chrom = parserPanel.getContig();
						Assert.assertTrue(chrom.equals("2"));
				
						Integer pos = parserPanel.getPos();
						Assert.assertTrue(pos == 228128540);
					
						String ref = parserPanel.getRef();
						Assert.assertTrue(ref.equals("C"));
					
						String alt = parserPanel.getAlt();
						Assert.assertTrue(alt.equals("T"));	
				
						Boolean hetero = parserPanel.isHetero();
						Assert.assertFalse(hetero);
								
						Boolean homo = parserPanel.isHomo();
						Assert.assertTrue(homo);
					}			
				else if (i == 15) {
					// Check the 16th variant (hom)
							Integer pos = parserPanel.getPos();
							Assert.assertTrue(pos == 228163453);
									
							Boolean hetero = parserPanel.isHetero();
							Assert.assertFalse(hetero);
									
							Boolean homo = parserPanel.isHomo();
							Assert.assertTrue(homo);
						}			
				else if (i == 16) {
					// Check the 17th variant (hom)
							Integer pos = parserPanel.getPos();
							Assert.assertTrue(pos == 228168748);
									
							Boolean hetero = parserPanel.isHetero();
							Assert.assertFalse(hetero);
									
							Boolean homo = parserPanel.isHomo();
							Assert.assertTrue(homo);
						}		
					i++;							
			}
										
			System.err.println("\tVCFParser tests on parsing homozygotes/heterozytgotes passed on panel VCF.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
	}
}
	
