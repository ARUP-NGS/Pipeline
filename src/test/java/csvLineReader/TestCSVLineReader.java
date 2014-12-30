package csvLineReader;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import util.VCFLineParser;
import util.vcfParser.VCFParser.GTType;
import buffer.variant.CSVLineReader;
import buffer.variant.VariantRec;

import gene.Gene;
import buffer.CSVFile;

/**
 * Unit test for CSVLineReader
 * @author elainegee
 *
 */

public class TestCSVLineReader {
	

	CSVFile gatkCSV = new CSVFile(new File("src/test/java/testcsvs/gatksingle_annotated.csv"));
	CSVFile freebayesSingleCSV = new CSVFile(new File("src/test/java/testcsvs/freebayesSingle_annotated.csv"));

	CSVFile solidTumorCSV = new CSVFile(new File("src/test/java/testcsvs/solid_tumor_test1_annotated.csv"));
	CSVFile bcrablCSV = new CSVFile(new File("src/test/java/testcsvs/bcrabl_annotated.csv"));
	
	CSVFile complexCSV = new CSVFile(new File("src/test/java/testcsvs/complexVars_annotated.csv"));
	CSVFile emptyCSV = new CSVFile(new File("src/test/java/testcsvs/empty_annotated.csv"));

	// Test single-sample GATK CSV
	@Test
	public void TestSingleGATKCSV() {	
		System.err.println("Testing CSVLineReader: single sample GATK CSV ...");

		try {

			CSVLineReader reader = new CSVLineReader(gatkCSV.getFile());

			int i=0;
			//Go through file
			do {			
				i++;
				VariantRec rec = reader.toVariantRec();
				
				// Check third variant
				if (i==3) {

					//Check contig
					String contig = rec.getContig();
					Assert.assertTrue(contig.equals("1")); 
				
					// Check start
					int start = rec.getStart();
					Assert.assertTrue(start==17407);
					
					// Check end
					int end = rec.getEnd();
					Assert.assertTrue(end==17408);
					
					
					// Check ref
					String ref = rec.getRef();
					Assert.assertTrue(ref.equals("G"));
					
					// Check alt
					String alt = rec.getAlt();
					Assert.assertTrue(alt.equals("A"));
					
					// Check quality
					Double qual = rec.getQuality();
					Assert.assertTrue(qual==105.76);
					
					// Check depth
					Double depth = rec.getProperty(VariantRec.DEPTH);
					Assert.assertTrue(depth==108.0);

					// Check heterozygosity
					GTType het = rec.getZygosity();
					Assert.assertTrue(het == GTType.HET);
					
					// Check genotype quality
					Double genotypeQual = rec.getProperty(VariantRec.GENOTYPE_QUALITY);
					Assert.assertTrue(genotypeQual==99.0);
					
					// Check variant depth
					Double varDepth = rec.getProperty(VariantRec.VAR_DEPTH);
					Assert.assertTrue(varDepth==15.0);
					
					// Check gene name
					String gene = rec.getAnnotation(VariantRec.GENE_NAME);
					Assert.assertTrue(gene.equals("WASH7P"));
					
					// Check c.dot
					String cdot = rec.getAnnotation(VariantRec.CDOT);
					Assert.assertNull(cdot);
					
					// Check p.dot
					String pdot = rec.getAnnotation(VariantRec.PDOT);
					Assert.assertNull(pdot);
					
					// Check NM Number
					String nm = rec.getAnnotation(VariantRec.NM_NUMBER);
					Assert.assertNull(nm);
					
					// Check variant type
					String varType = rec.getAnnotation(VariantRec.VARIANT_TYPE);
					Assert.assertTrue(varType.equals("ncRNA_intronic"));
					
					//Check ARUP overall frequency
					Double ARUPOverallFreq = rec.getProperty(VariantRec.ARUP_OVERALL_FREQ);
					Assert.assertTrue(ARUPOverallFreq==0.008928571428571428);
					
					//Check ARUP Freq details
					String ARUPFreqDeets = rec.getAnnotation(VariantRec.ARUP_FREQ_DETAILS);
					Assert.assertTrue(ARUPFreqDeets.equals("Samples: 112 Hets: 2 Homs: 0"));
					
					// Check splicingtopnm
					String spliceTopNM = rec.getAnnotation(VariantRec.SPLICING_TOPNM);
					Assert.assertTrue(spliceTopNM.equals("-"));				
									
					// Check splicingtopnmDiff
					String spliceTopNMDiff = rec.getAnnotation(VariantRec.SPLICING_TOPNMDIFF);
					Assert.assertTrue(spliceTopNMDiff.equals("-"));	
				}				
			} while(i<4 && reader.advanceLine());	
						
			System.err.println("\tCSVLineReader tests passed on single-sample GATK CSV.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();			
		}
	}
	
	// Test single-sample FreeBayes CSV
		@Test
		public void freebayesSingleCSV() {	
			System.err.println("Testing CSVLineReader: single sample FreeBayes CSV ...");

			try {

				CSVLineReader reader = new CSVLineReader(gatkCSV.getFile());

				int i=0;
				//Go through file
				do {			
					i++;
					VariantRec rec = reader.toVariantRec();
					
					// Check last variant
					if (i==67) {

						//Check contig
						String contig = rec.getContig();
						Assert.assertTrue(contig.equals("8")); 
					
						// Check start
						int start = rec.getStart();
						Assert.assertTrue(start==133978711);
						
						// Check end
						int end = rec.getEnd();
						Assert.assertTrue(end==133978712);
						
						
						// Check ref
						String ref = rec.getRef();
						Assert.assertTrue(ref.equals("C"));
						
						// Check alt
						String alt = rec.getAlt();
						Assert.assertTrue(alt.equals("GTG,G"));
						
						// Check quality
						Double qual = rec.getQuality();
						Assert.assertTrue(qual==276.864);
						
						// Check depth
						Double depth = rec.getProperty(VariantRec.DEPTH);
						Assert.assertTrue(depth==13.0);

						// Check heterozygosity
						GTType het = rec.getZygosity();
						Assert.assertTrue(het == GTType.HET);
						
						// Check genotype quality
						Double genotypeQual = rec.getProperty(VariantRec.GENOTYPE_QUALITY);
						Assert.assertTrue(genotypeQual==-1.0);
						
						// Check variant depth
						Double varDepth = rec.getProperty(VariantRec.VAR_DEPTH);
						Assert.assertTrue(varDepth==7.0);
						
						// Check gene name
						String gene = rec.getAnnotation(VariantRec.GENE_NAME);
						Assert.assertTrue(gene.equals("TG"));
						
						// Check c.dot
						String cdot = rec.getAnnotation(VariantRec.CDOT);
						Assert.assertNull(cdot);
						
						// Check p.dot
						String pdot = rec.getAnnotation(VariantRec.PDOT);
						Assert.assertNull(pdot);
						
						// Check NM Number
						String nm = rec.getAnnotation(VariantRec.NM_NUMBER);
						Assert.assertNull(nm);
						
						// Check variant type
						String varType = rec.getAnnotation(VariantRec.VARIANT_TYPE);
						Assert.assertTrue(varType.equals("intronic"));
						
						//Check ARUP overall frequency
						Double ARUPOverallFreq = rec.getProperty(VariantRec.ARUP_OVERALL_FREQ);
						Assert.assertTrue(ARUPOverallFreq==0.5223214285714286);
						
						//Check ARUP Freq details
						String ARUPFreqDeets = rec.getAnnotation(VariantRec.ARUP_FREQ_DETAILS);
						Assert.assertTrue(ARUPFreqDeets.equals("Samples: 112 Hets: 57 Homs: 30"));
						
						// Check splicingtopnm
						String spliceTopNM = rec.getAnnotation(VariantRec.SPLICING_TOPNM);
						Assert.assertTrue(spliceTopNM.equals("-"));				
										
						// Check splicingtopnmDiff
						String spliceTopNMDiff = rec.getAnnotation(VariantRec.SPLICING_TOPNMDIFF);
						Assert.assertTrue(spliceTopNMDiff.equals("-"));	
					}				
				} while(i<4 && reader.advanceLine());	
							
				System.err.println("\tCSVLineReader tests passed on single-sample FreeBayes CSV.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Assert.fail();			
			}
		}
	
	// Test solid tumor CSV
	@Test
	public void TestSolidTumorCSV() {	
		System.err.println("Testing CSVLineReader: (Ion Torrent) Solid Tumor CSV ...");

		try {

			CSVLineReader reader = new CSVLineReader(solidTumorCSV.getFile());

			int i=0;
			//Go through file
			do {			
				i++;
				VariantRec rec = reader.toVariantRec();
				
				// Check 10th variant
				if (i==10) {

					//Check contig
					String contig = rec.getContig();
					Assert.assertTrue(contig.equals("4")); 
				
					// Check start
					int start = rec.getStart();
					Assert.assertTrue(start==1807894);
					
					// Check end
					int end = rec.getEnd();
					Assert.assertTrue(end==1807895);
					
					
					// Check ref
					String ref = rec.getRef();
					Assert.assertTrue(ref.equals("G"));
					
					// Check alt
					String alt = rec.getAlt();
					Assert.assertTrue(alt.equals("A"));
					
					// Check quality
					Double qual = rec.getQuality();
					Assert.assertTrue(qual==23829.3);
					
					// Check depth
					Double depth = rec.getProperty(VariantRec.DEPTH);
					Assert.assertTrue(depth==1635.0);

					// Check heterozygosity (hom)
					GTType het = rec.getZygosity();
					Assert.assertTrue(het == GTType.HOM);
					
					// Check genotype quality
					Double genotypeQual = rec.getProperty(VariantRec.GENOTYPE_QUALITY);
					Assert.assertTrue(genotypeQual==99.0);
					
					// Check variant depth
					Double varDepth = rec.getProperty(VariantRec.VAR_DEPTH);
					Assert.assertTrue(varDepth==1627.0);
					
					// Check gene name
					String gene = rec.getAnnotation(VariantRec.GENE_NAME);
					Assert.assertTrue(gene.equals("FGFR3"));
					
					// Check c.dot
					String cdot = rec.getAnnotation(VariantRec.CDOT);
					Assert.assertTrue(cdot.equals("c.G1953A"));
					
					// Check p.dot
					String pdot = rec.getAnnotation(VariantRec.PDOT);
					Assert.assertTrue(pdot.equals("p.T651T"));
					
					// Check NM Number
					String nm = rec.getAnnotation(VariantRec.NM_NUMBER);
					Assert.assertTrue(nm.equals("NM_000142"));
					
					// Check variant type
					String varType = rec.getAnnotation(VariantRec.VARIANT_TYPE);
					Assert.assertTrue(varType.equals("exonic;splicing"));
					
					//Check ARUP overall frequency
					Double ARUPOverallFreq = rec.getProperty(VariantRec.ARUP_OVERALL_FREQ);
					Assert.assertTrue(ARUPOverallFreq==0.9821428571428571);
					
					//Check ARUP Freq details
					String ARUPFreqDeets = rec.getAnnotation(VariantRec.ARUP_FREQ_DETAILS);
					Assert.assertTrue(ARUPFreqDeets.equals("Samples: 112 Hets: 0 Homs: 110"));
					
					// Check splicingtopnm
					String spliceTopNM = rec.getAnnotation(VariantRec.SPLICING_TOPNM);
					Assert.assertTrue(spliceTopNM.equals("-"));				
									
					// Check splicingtopnmDiff
					String spliceTopNMDiff = rec.getAnnotation(VariantRec.SPLICING_TOPNMDIFF);
					Assert.assertTrue(spliceTopNMDiff.equals("-"));	
					
					//Check OMIM disease
					String omimdis = rec.getAnnotation(Gene.OMIM_DISEASES);					
					Assert.assertTrue(omimdis.equals("Achondroplasia; Bladder cancer, somatic; CATSHL syndrome; Cervical cancer, somatic; Colorectal cancer, somatic; Crouzon syndrome with acanthosis nigricans; Hypochondroplasia; LADD syndrome; Muenke syndrome; Nevus, epidermal, somatic; Spermatocytic seminoma, somatic; Thanatophoric dysplasia, type I; Thanatophoric dysplasia, type II"));
										
				}				
			} while(i<11 && reader.advanceLine());	
						
			System.err.println("\tCSVLineReader tests passed on (Ion Torrent) Solid Tumor CSV.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();			
		}
	}
	
	// Test (IonTorrent) BCR-ABL CSV
	@Test
	public void TestBCRABLCSV() {	
		System.err.println("Testing CSVLineReader: (IonTorrent) BCR-ABL CSV ...");

		try {

			CSVLineReader reader = new CSVLineReader(bcrablCSV.getFile());

			int i=0;
			//Go through file
			do {			
				i++;
				VariantRec rec = reader.toVariantRec();
				
				// Check second variant
				if (i==2) {

					//Check contig
					String contig = rec.getContig();
					Assert.assertTrue(contig.equals("ABL1")); 
				
					// Check start
					int start = rec.getStart();
					Assert.assertTrue(start==944);
					
					// Check end
					int end = rec.getEnd();
					Assert.assertTrue(end==945);
					
					
					// Check ref
					String ref = rec.getRef();
					Assert.assertTrue(ref.equals("C"));
					
					// Check alt
					String alt = rec.getAlt();
					Assert.assertTrue(alt.equals("T"));
					
					// Check quality
					Double qual = rec.getQuality();
					Assert.assertTrue(qual==2160.3);
					
					// Check depth
					Double depth = rec.getProperty(VariantRec.DEPTH);
					Assert.assertTrue(depth==21779.0);

					// Check heterozygosity
					GTType het = rec.getZygosity();
					Assert.assertTrue(het == GTType.HET);
					
					// Check genotype quality
					Double genotypeQual = rec.getProperty(VariantRec.GENOTYPE_QUALITY);
					Assert.assertTrue(genotypeQual==99.0);
					
					// Check variant depth
					Double varDepth = rec.getProperty(VariantRec.VAR_DEPTH);
					Assert.assertTrue(varDepth==5236.0);
					
					// Check gene name
					String gene = rec.getAnnotation(VariantRec.GENE_NAME);
					Assert.assertTrue(gene.equals("NONE"));
					
					// Check c.dot
					String cdot = rec.getAnnotation(VariantRec.CDOT);
					Assert.assertNull(cdot);
					
					// Check p.dot
					String pdot = rec.getAnnotation(VariantRec.PDOT);
					Assert.assertNull(pdot);
					
					// Check NM Number
					String nm = rec.getAnnotation(VariantRec.NM_NUMBER);
					Assert.assertNull(nm);
					
					// Check variant type
					String varType = rec.getAnnotation(VariantRec.VARIANT_TYPE);
					Assert.assertTrue(varType.equals("intergenic"));
					
					//Check ARUP overall frequency
					Double ARUPOverallFreq = rec.getProperty(VariantRec.ARUP_OVERALL_FREQ);
					Assert.assertTrue(ARUPOverallFreq==0.0);
					
					//Check ARUP Freq details
					String ARUPFreqDeets = rec.getAnnotation(VariantRec.ARUP_FREQ_DETAILS);
					Assert.assertTrue(ARUPFreqDeets.equals("Total samples: 0"));
					
					// Check splicingtopnm
					String spliceTopNM = rec.getAnnotation(VariantRec.SPLICING_TOPNM);
					Assert.assertTrue(spliceTopNM.equals("-"));				
									
					// Check splicingtopnmDiff
					String spliceTopNMDiff = rec.getAnnotation(VariantRec.SPLICING_TOPNMDIFF);
					Assert.assertTrue(spliceTopNMDiff.equals("-"));	
				}				
			} while(i<3 && reader.advanceLine());	
						
			System.err.println("\tCSVLineReader tests passed on (IonTorrent) BCR-ABL CSV.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();			
		}
	}
	
	// Test empty CSV
	@Test
	public void TestEmptyCSV() {	
		System.err.println("Testing CSVLineReader: empty CSV ...");

		try {

			CSVLineReader reader = new CSVLineReader(emptyCSV.getFile());

			//Go through file
			do {
				//Check variant record
				VariantRec rec = reader.toVariantRec();
				Assert.assertNull(rec);				
													
			} while(reader.advanceLine());	
						
			System.err.println("\tCSVLineReader tests passed on empty CSV.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();			
		}
	}
	
	// Test ComplexVars CSV
	@Test
	public void ComplexVarsCSV() {	
		System.err.println("Testing CSVLineReader: Complex Variants CSV ...");

		try {

			CSVLineReader reader = new CSVLineReader(complexCSV.getFile());

			int i=0;
			//Go through file
			do {			
				i++;
				VariantRec rec = reader.toVariantRec();
				
				// Check second variant
				if (i==2) {

					//Check contig
					String contig = rec.getContig();
					Assert.assertTrue(contig.equals("19")); 
				
					// Check start
					int start = rec.getStart();
					Assert.assertTrue(start==10665691);
					
					// Check end
					int end = rec.getEnd();
					Assert.assertTrue(end==10665696);
					
					// Check ref
					String ref = rec.getRef();
					Assert.assertTrue(ref.equals("TTGAC"));
					
					// Check alt
					String alt = rec.getAlt();
					Assert.assertTrue(alt.equals("CTGAT,CTGAC"));
					
					// Check quality
					Double qual = rec.getQuality();
					Assert.assertTrue(qual==439.999);
					
					// Check depth
					Double depth = rec.getProperty(VariantRec.DEPTH);
					Assert.assertTrue(depth==18.0);

					// Check heterozygosity
					GTType het = rec.getZygosity();
					Assert.assertTrue(het == GTType.HET);
					
					// Check genotype quality
					Double genotypeQual = rec.getProperty(VariantRec.GENOTYPE_QUALITY);
					Assert.assertTrue(genotypeQual==-1.0);
					
					// Check variant depth
					Double varDepth = rec.getProperty(VariantRec.VAR_DEPTH);
					Assert.assertTrue(varDepth==9.0);
					
					// Check gene name
					String gene = rec.getAnnotation(VariantRec.GENE_NAME);
					Assert.assertTrue(gene.equals("KRI1"));
					
					// Check c.dot
					String cdot = rec.getAnnotation(VariantRec.CDOT);
					Assert.assertNull(cdot);
					
					// Check p.dot
					String pdot = rec.getAnnotation(VariantRec.PDOT);
					Assert.assertNull(pdot);
					
					// Check NM Number
					String nm = rec.getAnnotation(VariantRec.NM_NUMBER);
					Assert.assertNull(nm);
					
					// Check variant type
					String varType = rec.getAnnotation(VariantRec.VARIANT_TYPE);
					Assert.assertTrue(varType.equals("intronic"));
					
					//Check ARUP overall frequency
					Double ARUPOverallFreq = rec.getProperty(VariantRec.ARUP_OVERALL_FREQ);
					Assert.assertTrue(ARUPOverallFreq==0.6607142857142857);
					
					//Check ARUP Freq details
					String ARUPFreqDeets = rec.getAnnotation(VariantRec.ARUP_FREQ_DETAILS);
					Assert.assertTrue(ARUPFreqDeets.equals("Samples: 112 Hets: 38 Homs: 55"));
					
					// Check splicingtopnm
					String spliceTopNM = rec.getAnnotation(VariantRec.SPLICING_TOPNM);
					Assert.assertTrue(spliceTopNM.equals("-"));				
									
					// Check splicingtopnmDiff
					String spliceTopNMDiff = rec.getAnnotation(VariantRec.SPLICING_TOPNMDIFF);
					Assert.assertTrue(spliceTopNMDiff.equals("-"));	
				}		
				
				// Check fourth variant
				else if (i==4) {

					//Check contig
					String contig = rec.getContig();
					Assert.assertTrue(contig.equals("8")); 
				
					// Check start
					int start = rec.getStart();
					Assert.assertTrue(start==133978711);
					
					// Check end
					int end = rec.getEnd();
					Assert.assertTrue(end==133978712);
										
					// Check ref
					String ref = rec.getRef();
					Assert.assertTrue(ref.equals("C"));
					
					// Check alt
					String alt = rec.getAlt();
					Assert.assertTrue(alt.equals("GTG,G"));
					
					// Check quality
					Double qual = rec.getQuality();
					Assert.assertTrue(qual==276.864);
					
					// Check depth
					Double depth = rec.getProperty(VariantRec.DEPTH);
					Assert.assertTrue(depth==13.0);

					// Check heterozygosity
					GTType het = rec.getZygosity();
					Assert.assertTrue(het == GTType.HET);
					
					// Check genotype quality
					Double genotypeQual = rec.getProperty(VariantRec.GENOTYPE_QUALITY);
					Assert.assertTrue(genotypeQual==-1.0);
					
					// Check variant depth
					Double varDepth = rec.getProperty(VariantRec.VAR_DEPTH);
					Assert.assertTrue(varDepth==7.0);
					
					// Check gene name
					String gene = rec.getAnnotation(VariantRec.GENE_NAME);
					Assert.assertTrue(gene.equals("TG"));
					
					// Check c.dot
					String cdot = rec.getAnnotation(VariantRec.CDOT);
					Assert.assertNull(cdot);
					
					// Check p.dot
					String pdot = rec.getAnnotation(VariantRec.PDOT);
					Assert.assertNull(pdot);
					
					// Check NM Number
					String nm = rec.getAnnotation(VariantRec.NM_NUMBER);
					Assert.assertNull(nm);
					
					// Check variant type
					String varType = rec.getAnnotation(VariantRec.VARIANT_TYPE);
					Assert.assertTrue(varType.equals("intronic"));
					
					//Check ARUP overall frequency
					Double ARUPOverallFreq = rec.getProperty(VariantRec.ARUP_OVERALL_FREQ);
					Assert.assertTrue(ARUPOverallFreq==0.5223214285714286);
					
					//Check ARUP Freq details
					String ARUPFreqDeets = rec.getAnnotation(VariantRec.ARUP_FREQ_DETAILS);
					Assert.assertTrue(ARUPFreqDeets.equals("Samples: 112 Hets: 57 Homs: 30"));
					
					// Check splicingtopnm
					String spliceTopNM = rec.getAnnotation(VariantRec.SPLICING_TOPNM);
					Assert.assertTrue(spliceTopNM.equals("-"));				
									
					// Check splicingtopnmDiff
					String spliceTopNMDiff = rec.getAnnotation(VariantRec.SPLICING_TOPNMDIFF);
					Assert.assertTrue(spliceTopNMDiff.equals("-"));	
				}
				
				// Check third variant
				else if (i==3) {

					//Check contig
					String contig = rec.getContig();
					Assert.assertTrue(contig.equals("4")); 
				
					// Check start
					int start = rec.getStart();
					Assert.assertTrue(start==154091284);
					
					// Check end
					int end = rec.getEnd();
					Assert.assertTrue(end==154091287);
										
					// Check ref
					String ref = rec.getRef();
					Assert.assertTrue(ref.equals("GAA"));
					
					// Check alt
					String alt = rec.getAlt();
					Assert.assertTrue(alt.equals("-"));
					
					// Check quality
					Double qual = rec.getQuality();
					Assert.assertTrue(qual==88.9478);
					
					// Check depth
					Double depth = rec.getProperty(VariantRec.DEPTH);
					Assert.assertTrue(depth==3.0);

					// Check heterozygosity (hom)
					GTType het = rec.getZygosity();
					Assert.assertTrue(het == GTType.HOM);
					
					// Check genotype quality
					Double genotypeQual = rec.getProperty(VariantRec.GENOTYPE_QUALITY);
					Assert.assertTrue(genotypeQual==-1.0);
					
					// Check variant depth
					Double varDepth = rec.getProperty(VariantRec.VAR_DEPTH);
					Assert.assertTrue(varDepth==3.0);
					
					// Check gene name
					String gene = rec.getAnnotation(VariantRec.GENE_NAME);
					Assert.assertTrue(gene.equals("TRIM2"));
					
					// Check c.dot
					String cdot = rec.getAnnotation(VariantRec.CDOT);
					Assert.assertNull(cdot);
					
					// Check p.dot
					String pdot = rec.getAnnotation(VariantRec.PDOT);
					Assert.assertNull(pdot);
					
					// Check NM Number
					String nm = rec.getAnnotation(VariantRec.NM_NUMBER);
					Assert.assertNull(nm);
					
					// Check variant type
					String varType = rec.getAnnotation(VariantRec.VARIANT_TYPE);
					Assert.assertTrue(varType.equals("intronic"));
					
					//Check ARUP overall frequency
					Double ARUPOverallFreq = rec.getProperty(VariantRec.ARUP_OVERALL_FREQ);
					Assert.assertTrue(ARUPOverallFreq==0.0);
					
					//Check ARUP Freq details
					String ARUPFreqDeets = rec.getAnnotation(VariantRec.ARUP_FREQ_DETAILS);
					Assert.assertTrue(ARUPFreqDeets.equals("Total samples: 0"));
					
					// Check splicingtopnm
					String spliceTopNM = rec.getAnnotation(VariantRec.SPLICING_TOPNM);
					Assert.assertTrue(spliceTopNM.equals("-"));				
									
					// Check splicingtopnmDiff
					String spliceTopNMDiff = rec.getAnnotation(VariantRec.SPLICING_TOPNMDIFF);
					Assert.assertTrue(spliceTopNMDiff.equals("-"));	
				}
				
				// Check first variant
				else if (i==3) {

					//Check contig
					String contig = rec.getContig();
					Assert.assertTrue(contig.equals("12")); 
				
					// Check start
					int start = rec.getStart();
					Assert.assertTrue(start==57870463);
					
					// Check end
					int end = rec.getEnd();
					Assert.assertTrue(end==57870466);
										
					// Check ref
					String ref = rec.getRef();
					Assert.assertTrue(ref.equals("GT"));
					
					// Check alt
					String alt = rec.getAlt();
					Assert.assertTrue(alt.equals("T"));
					
					// Check quality
					Double qual = rec.getQuality();
					Assert.assertTrue(qual==531.427);
					
					// Check depth
					Double depth = rec.getProperty(VariantRec.DEPTH);
					Assert.assertTrue(depth==42.0);

					// Check heterozygosity
					GTType het = rec.getZygosity();
					Assert.assertTrue(het == GTType.HET);
					
					// Check genotype quality
					Double genotypeQual = rec.getProperty(VariantRec.GENOTYPE_QUALITY);
					Assert.assertTrue(genotypeQual==-1.0);
					
					// Check variant depth
					Double varDepth = rec.getProperty(VariantRec.VAR_DEPTH);
					Assert.assertTrue(varDepth==25.0);
					
					// Check gene name
					String gene = rec.getAnnotation(VariantRec.GENE_NAME);
					Assert.assertTrue(gene.equals("ARHGAP9"));
					
					// Check c.dot
					String cdot = rec.getAnnotation(VariantRec.CDOT);
					Assert.assertNull(cdot);
					
					// Check p.dot
					String pdot = rec.getAnnotation(VariantRec.PDOT);
					Assert.assertNull(pdot);
					
					// Check NM Number
					String nm = rec.getAnnotation(VariantRec.NM_NUMBER);
					Assert.assertNull(nm);
					
					// Check variant type
					String varType = rec.getAnnotation(VariantRec.VARIANT_TYPE);
					Assert.assertTrue(varType.equals("intronic"));
					
					//Check ARUP overall frequency
					Double ARUPOverallFreq = rec.getProperty(VariantRec.ARUP_OVERALL_FREQ);
					Assert.assertTrue(ARUPOverallFreq==0.6383928571428571);
					
					//Check ARUP Freq details
					String ARUPFreqDeets = rec.getAnnotation(VariantRec.ARUP_FREQ_DETAILS);
					Assert.assertTrue(ARUPFreqDeets.equals("Samples: 112 Hets: 43 Homs: 50"));
					
					// Check splicingtopnm
					String spliceTopNM = rec.getAnnotation(VariantRec.SPLICING_TOPNM);
					Assert.assertTrue(spliceTopNM.equals("-"));				
									
					// Check splicingtopnmDiff
					String spliceTopNMDiff = rec.getAnnotation(VariantRec.SPLICING_TOPNMDIFF);
					Assert.assertTrue(spliceTopNMDiff.equals("-"));	
				}
			} while(reader.advanceLine());	
						
			System.err.println("\tCSVLineReader tests passed on Complex Variants CSV.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail();			
		}
	}
		
		
		
		
}