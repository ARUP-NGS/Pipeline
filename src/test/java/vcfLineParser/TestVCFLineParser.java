package vcfLineParser;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import util.VCFLineParser;
import util.vcfParser.VCFParser.GTType;
import buffer.VCFFile;
import buffer.variant.VariantRec;

/**
 * Unit test for VCFLine parser
 * @author elainegee
 *
 */

public class TestVCFLineParser {
	

	File gatkVCF = new File("src/test/java/testvcfs/gatksingle.vcf");
	File freebayesVCF = new File("src/test/java/testvcfs/freebayes.single.vcf");

	File solidTumorVCF = new File("src/test/java/testvcfs/solid_tumor_test1.vcf");
	File bcrablVCF = new File("src/test/java/testvcfs/bcrabl.vcf");
	
	File complexVCF = new File("src/test/java/testvcfs/complexVars.vcf");
	File emptyVCF = new File("src/test/java/testvcfs/empty.vcf");

	// Test single-sample GATK VCF
	@Test
	public void TestSingleGATKVCF() {	
		System.err.println("Testing VCFLineParser: single sample GATK VCF ...");

		try {
			VCFLineParser reader = new VCFLineParser(new VCFFile(gatkVCF));

			int i=0;
			//Go through file
			do {			
				i++;
				
				// Check third variant
				if (i==3) {

					//Chech contig
					String contig = reader.getContig();
					Assert.assertTrue(contig.equals("1")); 
				
					// Check pos
					int pos = reader.getPosition();
					Assert.assertTrue(pos==17407);
					
					// Check ref
					String ref = reader.getRef();
					Assert.assertTrue(ref.equals("G"));
					
					// Check alt
					String alt = reader.getAlt();
					Assert.assertTrue(alt.equals("A"));
					
					// Check quality
					Double qual = reader.getQuality();
					Assert.assertTrue(qual==105.76);
					
					// Check heterozygosity
					util.vcfParser.VCFParser.GTType het = reader.isHetero();
					Assert.assertTrue(het == GTType.HET);
					
					// Check genotype quality
					Double genotypeQual = reader.getGenotypeQuality();
					Assert.assertTrue(genotypeQual==99);
					
					// Check depth
					Integer depth = reader.getDepth();
					Assert.assertTrue(depth==108);
					
					// Check variant depth
					Integer vardepth = reader.getVariantDepth();
					Assert.assertTrue(vardepth==15);				
									
					// Check phase
					boolean phase = reader.isPhased();
					Assert.assertFalse(phase);
				}				
			} while(i<4 && reader.advanceLine());	
						
			System.err.println("\tVCFLineParser tests passed on single-sample GATK VCF.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();			
		}
	}

	// Test single-sample FreeBayes VCF
	@Test
	public void TestSingleFreeBayesVCF() {	
		System.err.println("Testing VCFLineParser: single sample FreeBayes VCF ...");

		try {
			VCFLineParser reader = new VCFLineParser(new VCFFile(freebayesVCF));

			int i=0;
			//Go through file
			do {			
				i++;
				
				// Check last variant
				if (i==67) {

					//Chech contig
					String contig = reader.getContig();
					Assert.assertTrue(contig.equals("8")); 
				
					// Check pos
					int pos = reader.getPosition();
					Assert.assertTrue(pos==133978709);
					
					// Check ref
					String ref = reader.getRef();
					Assert.assertTrue(ref.equals("CTC"));
					
					// Check alt
					String alt = reader.getAlt();
					Assert.assertTrue(alt.equals("CTGTG,CTG"));
					
					// Check quality
					Double qual = reader.getQuality();
					Assert.assertTrue(qual==276.864);
					
					// Check heterozygosity
					GTType het = reader.isHetero();
					Assert.assertTrue(het == GTType.HET);
					
					// Check genotype quality
					Double genotypeQual = reader.getGenotypeQuality();
					Assert.assertTrue(genotypeQual.equals(-1.0));
					
					// Check depth
					Integer depth = reader.getDepth();
					Assert.assertTrue(depth==13);
					
					// Check variant depth
					Integer vardepth1 = reader.getVariantDepth(0);
					Assert.assertTrue(vardepth1==7);			
					
					Integer vardepth2= reader.getVariantDepth(1);
					Assert.assertTrue(vardepth2==5);		
									
					// Check phase
					boolean phase = reader.isPhased();
					Assert.assertFalse(phase);
				}				
			} while(i<68 && reader.advanceLine());	
						
			System.err.println("\tVCFLineParser tests passed on single-sample FreeBayes VCF.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();			
		}
	}

	// Test solid tumor (IonTorrent) VCF
		@Test
		public void TestSolidTumorVCF() {	
			System.err.println("Testing VCFLineParser: (Ion Torrent) Solid Tumor VCF ...");

			try {
				VCFLineParser reader = new VCFLineParser(new VCFFile(solidTumorVCF));

				int i=0;
				//Go through file
				do {			
					i++;
					
					// Check second variant
					if (i==2) {

						//Chech contig
						String contig = reader.getContig();
						Assert.assertTrue(contig.equals("chr4")); 
					
						// Check pos
						int pos = reader.getPosition();
						Assert.assertTrue(pos==1807894);
						
						// Check ref
						String ref = reader.getRef();
						Assert.assertTrue(ref.equals("G"));
						
						// Check alt
						String alt = reader.getAlt();
						Assert.assertTrue(alt.equals("A"));
						
						// Check quality
						Double qual = reader.getQuality();
						Assert.assertTrue(qual==23829.3);
						
						// Check heterozygosity (hom)
						GTType het = reader.isHetero();
						Assert.assertTrue(het == GTType.HOM);
						
						// Check genotype quality
						Double genotypeQual = reader.getGenotypeQuality();
						Assert.assertTrue(genotypeQual==99.0);
						
						// Check depth
						Integer depth = reader.getDepth();
						Assert.assertTrue(depth==1635);
						
						// Check variant depth
						Integer vardepth = reader.getVariantDepth();
						Assert.assertTrue(vardepth==1627);				
										
						// Check phase
						boolean phase = reader.isPhased();
						Assert.assertFalse(phase);
					}				
				} while(i<5 && reader.advanceLine());	
							
				System.err.println("\tVCFLineParser tests passed on (IonTorrent) solid tumor VCF.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Assert.fail();			
			}
		}
	
		// Test bcrabl (IonTorrent) VCF
		@Test
		public void TestBcrAblVCF() {	
			System.err.println("Testing VCFLineParser: (IonTorrent) BCR-ABL VCF ...");

			try {
				VCFLineParser reader = new VCFLineParser(new VCFFile(bcrablVCF));

				int i=0;
				//Go through file
				do {			
					i++;
					
					// Check last variant
					if (i==2) {

						//Chech contig
						String contig = reader.getContig();
						Assert.assertTrue(contig.equals("ABL1")); 
					
						// Check pos
						int pos = reader.getPosition();
						Assert.assertTrue(pos==944);
						
						// Check ref
						String ref = reader.getRef();
						Assert.assertTrue(ref.equals("C"));
						
						// Check alt
						String alt = reader.getAlt();
						Assert.assertTrue(alt.equals("T"));
						
						// Check quality
						Double qual = reader.getQuality();
						Assert.assertTrue(qual==2160.3);
						
						// Check heterozygosity (het)
						GTType het = reader.isHetero();
						Assert.assertTrue(het == GTType.HET);
						
						// Check genotype quality
						Double genotypeQual = reader.getGenotypeQuality();
						Assert.assertTrue(genotypeQual==99);
						
						// Check depth
						Integer depth = reader.getDepth();
						Assert.assertTrue(depth==21779);
						
						// Check variant depth
						Integer vardepth = reader.getVariantDepth();
						Assert.assertTrue(vardepth==5236);				
										
						// Check phase
						boolean phase = reader.isPhased();
						Assert.assertFalse(phase);
					}				
				} while(i<3 && reader.advanceLine());	
							
				System.err.println("\tVCFLineParser tests passed on (IonTorrent) BCR-ABL VCF.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Assert.fail();			
			}
		}
		
		
	//Test Empty VCF
	@Test
	public void TestEmptyVCF() {	
		System.err.println("Testing VCFLineParser: empty VCF ...");

		try {
			VCFLineParser reader = new VCFLineParser(new VCFFile(emptyVCF));

			//Go through file
			do {			
					//Chech contig
					String contig = reader.getContig();
					Assert.assertNull(contig); 
				
					// Check pos
					int pos = reader.getPosition();
					Assert.assertTrue(pos==-1);
					
					// Check ref
					String ref = reader.getRef();
					Assert.assertTrue(ref.equals("?"));

					// Check alt
					String alt = reader.getAlt();
					Assert.assertTrue(alt.equals("?"));
					
					// Check quality
					Double qual = reader.getQuality();
					Assert.assertTrue(qual.equals(-1.0));
					
					//Check heterozygosity (Unknown)
					GTType het = reader.isHetero();
					Assert.assertTrue(het == GTType.UNKNOWN);

					
					// Check genotype quality
					Double genotypeQual = reader.getGenotypeQuality();
					Assert.assertTrue(genotypeQual.equals(-1.0));
					
					// Check depth
					Integer depth = reader.getDepth();
					Assert.assertTrue(depth==-1);
					
					// Check variant depth
					Integer vardepth = reader.getVariantDepth();
					Assert.assertTrue(vardepth==-1);			
									
					// Check phase
					boolean phase = reader.isPhased();
					Assert.assertFalse(phase);
				
			} while(reader.advanceLine());	
						
			System.err.println("\tVCFLineParser tests passed on empty VCF.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();
			
		}
	}
	
	// Test Complex Vars VCF
	@Test
	public void TestComplexVarsVCF() {	
		System.err.println("Testing VCFLineParser: Complex Variants VCF ...");

		try {
			VCFLineParser reader = new VCFLineParser(new VCFFile(complexVCF));

			int i=0;
			//Go through file
			do {			
				i++;
				
				// Check first variant (multiple alleles)
				if (i==1) {

					//Chech contig
					String contig = reader.getContig();
					Assert.assertTrue(contig.equals("19")); 
				
					// Check pos
					int pos = reader.getPosition();
					Assert.assertTrue(pos==10665691);
					
					// Check ref
					String ref = reader.getRef();
					Assert.assertTrue(ref.equals("TTGAC"));
					
					// Check alt
					String alt = reader.getAlt();
					Assert.assertTrue(alt.equals("CTGAT,CTGAC"));
					
					// Check quality
					Double qual = reader.getQuality();
					Assert.assertTrue(qual==439.999);
					
					// Check heterozygosity (het)
					GTType het = reader.isHetero();
					Assert.assertTrue(het == GTType.HET);
					
					// Check genotype quality
					Double genotypeQual = reader.getGenotypeQuality();
					Assert.assertTrue(genotypeQual==-1.0);
					
					// Check depth
					Integer depth = reader.getDepth();
					Assert.assertTrue(depth==18);
					
					// Check variant depth
					Integer vardepth1 = reader.getVariantDepth(0);
					Assert.assertTrue(vardepth1==9);	
					
					Integer vardepth2 = reader.getVariantDepth(1);
					Assert.assertTrue(vardepth2==19);	
									
					// Check phase
					boolean phase = reader.isPhased();
					Assert.assertFalse(phase);
				}
				// Check second variant (multiple alternate alleles)
				else if (i==2) {
					VariantRec rec = reader.toVariantRec();
					// Check alt (via VariantRec)
					String RecAlt = rec.getAlt();
					Assert.assertTrue(RecAlt.equals("GTG,G"));					
					
					//Chech contig
					String contig = reader.getContig();
					Assert.assertTrue(contig.equals("8")); 
				
					// Check pos
					int pos = reader.getPosition();
					Assert.assertTrue(pos==133978709);
					
					// Check ref
					String ref = reader.getRef();
					Assert.assertTrue(ref.equals("CTC"));
					
					// Check alt
					String alt = reader.getAlt();
					Assert.assertTrue(alt.equals("CTGTG,CTG"));
					
					//Check genotype
					String genotype=reader.getGenotype();
					Assert.assertTrue(genotype.equals("1/2"));
					
					// Check quality
					Double qual = reader.getQuality();
					Assert.assertTrue(qual==276.864);
					
					// Check heterozygosity (het)
					GTType het = reader.isHetero();
					Assert.assertTrue(het == GTType.HET);
					
					// Check genotype quality
					Double genotypeQual = reader.getGenotypeQuality();
					Assert.assertTrue(genotypeQual==-1.0);
					
					// Check depth
					Integer depth = reader.getDepth();
					Assert.assertTrue(depth==13);
					
					// Check variant depth
					Integer vardepth1 = reader.getVariantDepth(0);
					Assert.assertTrue(vardepth1==7);	
					
					Integer vardepth2 = reader.getVariantDepth(1);
					Assert.assertTrue(vardepth2==5);	
									
					// Check phase
					boolean phase = reader.isPhased();
					Assert.assertFalse(phase);
				}	
				// Check third variant (deletion)
				else if (i==3) {

					//Chech contig
					String contig = reader.getContig();
					Assert.assertTrue(contig.equals("4")); 
				
					// Check pos
					int pos = reader.getPosition();
					Assert.assertTrue(pos==154091281);
					
					// Check ref
					String ref = reader.getRef();
					Assert.assertTrue(ref.equals("GGAGAA"));
					
					// Check alt
					String alt = reader.getAlt();
					Assert.assertTrue(alt.equals("GGA"));
					
					// Check quality
					Double qual = reader.getQuality();
					Assert.assertTrue(qual==88.9478);
					
					// Check heterozygosity (hom)
					GTType het = reader.isHetero();
					Assert.assertTrue(het == GTType.HOM);
					
					// Check genotype quality
					Double genotypeQual = reader.getGenotypeQuality();
					Assert.assertTrue(genotypeQual==-1.0);
					
					// Check depth
					Integer depth = reader.getDepth();
					Assert.assertTrue(depth==3);
					
					// Check variant depth
					Integer vardepth = reader.getVariantDepth();
					Assert.assertTrue(vardepth==3);	
									
					// Check phase
					boolean phase = reader.isPhased();
					Assert.assertFalse(phase);
				}
				// Check fourth variant (deletion)
				else if (i==4) {

					//Chech contig
					String contig = reader.getContig();
					Assert.assertTrue(contig.equals("12")); 
				
					// Check pos
					int pos = reader.getPosition();
					Assert.assertTrue(pos==57870463);
					
					// Check ref
					String ref = reader.getRef();
					Assert.assertTrue(ref.equals("AGT"));
					
					// Check alt
					String alt = reader.getAlt();
					Assert.assertTrue(alt.equals("AT"));
					
					// Check quality
					Double qual = reader.getQuality();
					Assert.assertTrue(qual==531.427);
					
					// Check heterozygosity (het)
					GTType het = reader.isHetero();
					Assert.assertTrue(het == GTType.HET);
					
					// Check genotype quality
					Double genotypeQual = reader.getGenotypeQuality();
					Assert.assertTrue(genotypeQual==-1.0);
					
					// Check depth
					Integer depth = reader.getDepth();
					Assert.assertTrue(depth==42);
					
					// Check variant depth
					Integer vardepth = reader.getVariantDepth();
					Assert.assertTrue(vardepth==25);	
									
					// Check phase
					boolean phase = reader.isPhased();
					Assert.assertFalse(phase);
				}
			} while(reader.advanceLine());	
						
			System.err.println("\tVCFLineParser tests passed on Complex Variants VCF.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();			
		}
	}
}