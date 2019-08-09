package vcfLineParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import buffer.variant.VariantRec;
import util.vcfParser.VCFParser;
import util.vcfParser.VCFParser.EntryType;
import util.vcfParser.VCFParser.GTType;
import util.vcfParser.VCFParser.HeaderEntry;

public class TestVCFParser {

	File gatkVCF = new File("src/test/java/testvcfs/gatksingle.vcf");
	File freebayesVCF = new File("src/test/java/testvcfs/freebayes.single.vcf");

	File solidTumorVCF = new File("src/test/java/testvcfs/solid_tumor_test1.vcf");
	File bcrablVCF = new File("src/test/java/testvcfs/bcrabl.vcf");
	
	File complexVCF = new File("src/test/java/testvcfs/complexVars.vcf");
	File emptyVCF = new File("src/test/java/testvcfs/empty.vcf");
	
	File panelVCF = new File("examples/test_panel.vcf");
	
	File noVariantCallerHeaderVCF = new File("src/test/java/testvcfs/noVariantHeader.vcf");
	
	File completeGenomicsVCF = new File("src/test/java/testvcfs/completeGenomics-GTtest.vcf");
	
	File LoFreqScalpelMantaVCF = new File("src/test/java/testvcfs/lofreq_scalpel_manta.vcf");
	
	File germlineMNPSVCF = new File("src/test/java/testvcfs/mnps3.vcf");

        File lithiumVCF = new File("src/test/java/testvcfs/lithium_filter.vcf");
	
	@Test (expected = IOException.class)
	public void TestEmptyHeader() throws IOException {
		System.out.println("VCFParser tests on parsing header:");
		
		VCFParser parserNoCreator = new VCFParser(noVariantCallerHeaderVCF);
		parserNoCreator.getCreator();
	}
	
	@Test
	public void TestFreeBayesHeader() throws IOException {
		VCFParser parser = new VCFParser(freebayesVCF);
		
		Assert.assertNotNull(parser.getCreator());
		Assert.assertTrue(parser.getCreator().startsWith("freeBayes"));
		
		Set<String> samples = parser.getSamples();
		Assert.assertTrue(samples.size() ==1);
		Assert.assertTrue(samples.iterator().next().equals("sample"));
		
		HeaderEntry entry = parser.getHeaderEntry("INFO_NS");
		Assert.assertNotNull(entry);
		Assert.assertTrue(entry.entryType == EntryType.INFO);
		Assert.assertTrue(entry.id.equals("INFO_NS"));
		Assert.assertTrue(entry.type.equals("Integer"));
		Assert.assertTrue(entry.number.equals("1"));
		
		entry = parser.getHeaderEntry("INFO_SAP");
		Assert.assertNotNull(entry);
		Assert.assertTrue(entry.entryType == EntryType.INFO);
		Assert.assertTrue(entry.id.equals("INFO_SAP"));
		Assert.assertTrue(entry.type.equals("Float"));
		
		entry = parser.getHeaderEntry("FORMAT_QR");
		Assert.assertNotNull(entry);
		Assert.assertTrue(entry.entryType == EntryType.FORMAT);
		Assert.assertTrue(entry.id.equals("FORMAT_QR"));
		Assert.assertTrue(entry.type.equals("Integer"));
		
		System.out.println("\tVCFParser tests on parsing header passed for single-sample FreeBayes VCF.");
	
	}	
		
	@Test
	public void TestGATKHeader() throws IOException {
		VCFParser parser = new VCFParser(gatkVCF);
		
		Assert.assertNotNull(parser.getCreator());
		Assert.assertTrue(parser.getCreator().startsWith("GATK"));
		
		Set<String> samples = parser.getSamples();
		Assert.assertTrue(samples.size() ==1);
		Assert.assertTrue(samples.iterator().next().equals("sample"));
		
		HeaderEntry entry = parser.getHeaderEntry("INFO_MQ");
		Assert.assertNotNull(entry);
		Assert.assertTrue(entry.entryType == EntryType.INFO);
		Assert.assertTrue(entry.id.equals("INFO_MQ"));
		Assert.assertTrue(entry.type.equals("Float"));
		
		entry = parser.getHeaderEntry("FORMAT_AD");
		Assert.assertNotNull(entry);
		Assert.assertTrue(entry.entryType == EntryType.FORMAT);
		Assert.assertTrue(entry.id.equals("FORMAT_AD"));
		Assert.assertTrue(entry.type.equals("Integer"));
		
		entry = parser.getHeaderEntry("FORMAT_GT");
		Assert.assertNotNull(entry);
		Assert.assertTrue(entry.entryType == EntryType.FORMAT);
		Assert.assertTrue(entry.id.equals("FORMAT_GT"));
		Assert.assertTrue(entry.type.equals("String"));
		Assert.assertTrue(entry.number.equals("1"));
						
		System.out.println("\tVCFParser tests on parsing header passed for single-sample GATK VCF.");
			
	}
	
	@Test
	public void TestLoFreqScalpelHeader() throws IOException {		
		VCFParser lsmParser = new VCFParser(LoFreqScalpelMantaVCF);
		
		Assert.assertNotNull(lsmParser.getCreator());
		Assert.assertTrue(lsmParser.getCreator().startsWith("lofreq_scalpel_manta"));
		
		Set<String> lsmSamples = lsmParser.getSamples();
		Assert.assertTrue(lsmSamples.size() ==1);
		Assert.assertTrue(lsmSamples.iterator().next().equals("test_sample"));
		
		HeaderEntry insEntry = lsmParser.getHeaderEntry("ALT_INS");
		Assert.assertNotNull(insEntry);
		Assert.assertTrue(insEntry.entryType == EntryType.ALT);
		Assert.assertTrue(insEntry.id.equals("ALT_INS"));
		Assert.assertTrue(insEntry.description.equals("Insertion"));
		
		HeaderEntry dp4Entry = lsmParser.getHeaderEntry("INFO_DP4");
		Assert.assertNotNull(dp4Entry);
		Assert.assertTrue(dp4Entry.entryType == EntryType.INFO);
		Assert.assertTrue(dp4Entry.id.equals("INFO_DP4"));
		Assert.assertTrue(dp4Entry.type.equals("Integer"));

		HeaderEntry afEntry = lsmParser.getHeaderEntry("FORMAT_AF");
		Assert.assertNotNull(afEntry);
		Assert.assertTrue(afEntry.entryType == EntryType.FORMAT);
		Assert.assertTrue(afEntry.id.equals("FORMAT_AF"));
		Assert.assertTrue(afEntry.type.equals("Float"));
		Assert.assertTrue(afEntry.number.equals("1"));
		Assert.assertTrue(afEntry.description.equals("Allele Frequency"));

		System.out.println("\tVCFParser tests on parsing header passed for merged Lofreq-Scalpel-Manta VCF.");		
	}
	
	
	
	@Test
	public void TestSettingStripFlags() throws IOException {
		VCFParser parser = new VCFParser(complexVCF);		

		//Test setting strip flag
		Assert.assertTrue(parser.isStripInitialMatchingBases()); //check default				
		parser.setStripInitialMatchingBases(false);
		Assert.assertFalse(parser.isStripInitialMatchingBases());
		parser.setStripInitialMatchingBases(true);
		Assert.assertTrue(parser.isStripInitialMatchingBases());
		System.out.println("\tTest on flipping strip matching bases flag passed.");		
	}
	
	@Test
	public void TestParseComplexVariants() throws IOException {
		VCFParser parser = new VCFParser(complexVCF);	
		parser.setStripInitialMatchingBases(false);

		System.out.println("Complex VCF test, variants not normalized");
		int i=0;

		//Go through file
		while(parser.advanceLine() && i<5) {	
			VariantRec var = parser.toVariantRec();
			System.out.println(var.toSimpleString());		

			// Check second variant, first alt 
			if (i == 2) { //first variant has 2 alts
				Assert.assertTrue(var.getStart() == 133978709);
				
				String ref = parser.getRef();
				Assert.assertTrue(var.getRef().equals(ref));
				Assert.assertTrue(ref.equals("CTC"));
				
				String alt = parser.getAlt();
				Assert.assertTrue(var.getAlt().equals(alt));
				Assert.assertTrue(alt.equals("CTGTG"));					
				
				String gt = parser.getGT();
				Assert.assertTrue(gt.equals("CTGTG/CTG"));
				Assert.assertTrue(var.getGenotype().equals("CTGTG/CTG"));
				
				GTType hetero = parser.isHetero();
				Assert.assertTrue(var.getZygosity() == GTType.HET);
				Assert.assertTrue(hetero == GTType.HET);
						
				GTType homo = parser.isHomo();
				Assert.assertTrue(homo == GTType.HET);
						
				Boolean phase = parser.isPhased();
				Assert.assertFalse(phase);

				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(133978709)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("CTC"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("CTGTG"));
											
				Integer depth = parser.getDepth();
				Assert.assertTrue(var.getProperty(VariantRec.DEPTH).equals( new Double(depth)));
				Assert.assertTrue(depth==13);

				Integer varDepth = parser.getVariantDepth();
				Assert.assertTrue(varDepth==7);
						
				Assert.assertTrue(var.getProperty(VariantRec.GENOTYPE_QUALITY) == null);
						
				Double vqsr = parser.getVQSR();
				Assert.assertTrue(vqsr.equals(-1.0));
						
				Double fs = parser.getStrandBiasScore();
				Assert.assertTrue(fs.equals(-1.0));
						
				Double rp = parser.getRPScore();
				Assert.assertTrue(rp.equals(-1.0));
				
			}
			
			// Check second variant, second alt
			if (i == 3) {
				Integer pos = parser.getPos();
				Assert.assertTrue(pos == 133978709);
				
				String ref = parser.getRef();
				Assert.assertTrue(ref.equals("CTC"));
				
				String alt = parser.getAlt();
				Assert.assertTrue(alt.equals("CTG"));		
				
				String genotype = parser.getGT();
				Assert.assertTrue(genotype.equals("CTGTG/CTG")); //need to trim this
				
				GTType hetero = parser.isHetero();
				Assert.assertTrue(hetero == GTType.HET);
						
				GTType homo = parser.isHomo();
				Assert.assertTrue(homo == GTType.HET);
						
				Boolean phase = parser.isPhased();
				Assert.assertFalse(phase);
											
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(133978709)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("CTC"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("CTG"));
				
				Integer depth = parser.getDepth();
				Assert.assertTrue(depth==13);

				Integer varDepth = parser.getVariantDepth();
				Assert.assertTrue(varDepth==5);
			}
			
			i++;
									
		}
		
		System.out.println("\tVCFParser tests for parsing variants (not normalized) passed on a complex VCF (don't strip initial matching bases).");
	}
	
	@Test
	public void TestNormComplexVariants() throws IOException{
		
		//Normalize variant (front & back)

		System.out.println("Complex VCF test, normalized variants");
		VCFParser parserStrip = new VCFParser(complexVCF);
			
		int i=0;

		//Go through file
		while(parserStrip.advanceLine() && i<4) {	
			VariantRec var = parserStrip.toVariantRec();
			var = VCFParser.normalizeVariant(var);
			System.out.println(i);
			System.out.println(var.toSimpleString());		

			// Check second variant, first alt
			if (i == 2) { //skip first variant with 2 alts				
				Integer pos = var.getStart();
				Assert.assertTrue(pos == 133978711);
				
				String ref = parserStrip.getRef();
				Assert.assertTrue(ref.equals("CTC"));
				
				String normRef = var.getRef();
				Assert.assertTrue(normRef.equals("C"));

				String alt = var.getAlt();
				Assert.assertTrue(alt.equals("GTG"));					
				
				String gt = parserStrip.getGT();
				Assert.assertTrue(gt.equals("CTGTG/CTG"));
				String genotype = var.getGenotype();
				Assert.assertTrue(genotype.equals("CTGTG/CTG")); //genotype is alt-alt
				
				GTType hetero = var.getZygosity();
				Assert.assertTrue(hetero == GTType.HET);
						
				Boolean phase = parserStrip.isPhased();
				Assert.assertFalse(phase);
				
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(133978709)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("CTC"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("CTGTG"));
												
				Integer depth = parserStrip.getDepth();
				Assert.assertTrue(var.getProperty(VariantRec.DEPTH).equals(new Double(depth)));

				Integer varDepth = parserStrip.getVariantDepth();
				Assert.assertTrue(var.getProperty(VariantRec.VAR_DEPTH).equals(new Double(7)));
				
				Assert.assertTrue(var.getProperty(VariantRec.GENOTYPE_QUALITY)== null);
						
				Double vqsr = parserStrip.getVQSR();
				Assert.assertTrue(vqsr.equals(-1.0));
						
				Double fs = parserStrip.getStrandBiasScore();
				Assert.assertTrue(fs.equals(-1.0));
						
				Double rp = parserStrip.getRPScore();
				Assert.assertTrue(rp.equals(-1.0));
						
			}
					
			// Check second variant, second alt
			if (i == 3) {
				Assert.assertTrue(var.getStart() == 133978711);
				
				String ref = var.getRef();
				Assert.assertTrue(ref.equals("C"));
				
				String alt = var.getAlt();
				Assert.assertTrue(alt.equals("G"));	
				
				String genotype = var.getGenotype();
				Assert.assertTrue(genotype.equals("CTGTG/CTG")); //need to trim this
									
				GTType hetero = var.getZygosity();
				Assert.assertTrue(hetero == GTType.HET);
						
				GTType homo = parserStrip.isHomo();
				Assert.assertTrue(homo == GTType.HET);
						
				Boolean phase = parserStrip.isPhased();
				Assert.assertFalse(phase);
				
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(133978709)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("CTC"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("CTG"));
											
				Integer depth = parserStrip.getDepth();
				Assert.assertTrue(depth==13);

				Integer varDepth = parserStrip.getVariantDepth();
				Assert.assertTrue(varDepth==5);
			}
					
			i++;
											
		}
				
		System.out.println("\tVCFParser tests for parsing normalized variants passed on a complex VCF.");
	}
	
	
	@Test
	public void TestNormGATKVars() throws IOException {
		System.out.println("GATK VCF test, normalize variant:");
		VCFParser parserGATK = new VCFParser(gatkVCF);
		//Go through file
		int i=0;
		while(parserGATK.advanceLine() && i<1) {	
			VariantRec var = parserGATK.toVariantRec();
			var = VCFParser.normalizeVariant(var);
			
			// Check first variant, first alt
			if (i == 0) {
				String genotype = parserGATK.getGT();
				Assert.assertTrue(genotype.equals("G/C"));
				
				GTType hetero = parserGATK.isHetero();
				Assert.assertTrue(hetero == GTType.HET);
				
				GTType homo = parserGATK.isHomo();
				Assert.assertTrue(homo == GTType.HET);
				
				Boolean phase = parserGATK.isPhased();
				Assert.assertFalse(phase);
				
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(14673)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("G"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("C"));
				
				Integer depth = parserGATK.getDepth();
				Assert.assertTrue(depth==41);

				Integer varDepth = parserGATK.getVariantDepth();
				Assert.assertTrue(varDepth==15);
				
				String genotypeQual = parserGATK.getGenotypeQuality();
				Assert.assertTrue(genotypeQual.equals("58.54"));
				
				Double vqsr = parserGATK.getVQSR();
				Assert.assertTrue(vqsr.equals(-1.0));
				
				Double fs = parserGATK.getStrandBiasScore();
				Assert.assertTrue(fs.equals(0.0));
				
				Double rp = parserGATK.getRPScore();
				Assert.assertTrue(rp.equals(2.205));
			}
			i++;		

		}
		System.out.println("\tVCFParser tests for parsing normalized variants passed on a single-sample GATK VCF.");
		
	}
	
	@Test
	public void TestNormFreeBayesVars() throws IOException {
		//Check fields for single-sample FreeBayes
		System.out.println("FreeBayes VCF test, normalilzed:");
		VCFParser parserFB = new VCFParser(freebayesVCF);
		//Go through file
		int i=0;

		while(parserFB.advanceLine() && i<3) {	
			VariantRec var = parserFB.toVariantRec();
			var = VCFParser.normalizeVariant(var);
			// Check third variant
			if (i == 2) {
				String genotype = parserFB.getGT();
				Assert.assertTrue(genotype.equals("C/A"));
				
				GTType hetero = parserFB.isHetero();
				Assert.assertTrue(hetero == GTType.HET);

				GTType homo = parserFB.isHomo();
				Assert.assertTrue(homo == GTType.HET);
				
				Boolean phase = parserFB.isPhased();
				Assert.assertFalse(phase);
				
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(11863)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("C"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("A"));
				
				Integer depth = parserFB.getDepth();
				Assert.assertTrue(depth==7);

				Integer varDepth = parserFB.getVariantDepth();
				Assert.assertTrue(varDepth==2);
						
				String genotypeQual = parserFB.getGenotypeQuality();
				Assert.assertTrue(genotypeQual == null);
						
				Double vqsr = parserFB.getVQSR();
				Assert.assertTrue(vqsr.equals(-1.0));
				
				Double fs = parserFB.getStrandBiasScore();
				Assert.assertTrue(fs.equals(-1.0));
						
				Double rp = parserFB.getRPScore();
				Assert.assertTrue(rp.equals(-1.0));
			}
			i++;		

		}
		System.out.println("\tVCFParser tests for parsing normalized variants passed on a single-sample FreeBayes VCF.");
		
	}
	
	@Test
	public void TestParseLoFreqScalpelMantaVariants() throws IOException {
		VCFParser LSMparser = new VCFParser(LoFreqScalpelMantaVCF);		
		LSMparser.setStripInitialMatchingBases(false);
		
		System.out.println("Lofreq-Scalpel-Manta VCF test, parse variants without normalization");
		int i=0;

		//Go through file
		while(LSMparser.advanceLine() && i<4) {		
			VariantRec var = LSMparser.toVariantRec();	
			System.out.println(var.toSimpleString());		

			// Check 1st variant, from LoFreq
			if (i == 0) { 
				Integer pos = LSMparser.getPos();
				Assert.assertTrue(var.getStart() == pos);
				Assert.assertTrue(pos == 43814864);
				
				String ref = LSMparser.getRef();
				Assert.assertTrue(var.getRef().equals(ref));
				Assert.assertTrue(ref.equals("T"));
				
				String alt = LSMparser.getAlt();
				Assert.assertTrue(var.getAlt().equals(alt));
				Assert.assertTrue(alt.equals("C"));					
				
				String gt = LSMparser.getGT();
				Assert.assertTrue(gt.equals("T/C"));
				Assert.assertTrue(var.getGenotype().equals("T/C"));
				
				GTType hetero = LSMparser.isHetero();
				Assert.assertTrue(hetero == GTType.HET);
				Assert.assertTrue(var.getZygosity() == GTType.HET);
						
				GTType homo = LSMparser.isHomo();
				Assert.assertTrue(homo == GTType.HET);
						
				Boolean phase = LSMparser.isPhased();
				Assert.assertFalse(phase);

				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(43814864)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("T"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("C"));
											
				Integer depth = LSMparser.getDepth();
				Assert.assertTrue(var.getProperty(VariantRec.DEPTH).equals( new Double(depth)));
				int DepthSum = 485 + 306 + 476 + 273;
				Assert.assertTrue(depth==DepthSum);

				Integer varDepth = LSMparser.getVariantDepth();
				int altDepthSum = 476 + 273;
				Assert.assertTrue(varDepth==altDepthSum);
						
				Assert.assertTrue(var.getAnnotation(VariantRec.GENOTYPE_QUALITY) == null);			
						
				Double strandbias = LSMparser.getStrandBiasScore();
				Assert.assertTrue(strandbias.equals(4.0));
						
				Double rp = LSMparser.getRPScore();
				Assert.assertTrue(rp.equals(-1.0));
				
			}
			
			// Check second variant
			if (i == 1) {
				Integer pos = LSMparser.getPos();
				Assert.assertTrue(pos == 190068427);
				
				String ref = LSMparser.getRef();
				Assert.assertTrue(ref.equals("TACACAC"));
				
				String alt = LSMparser.getAlt();
				Assert.assertTrue(alt.equals("T"));		
				
				String genotype = LSMparser.getGT();
				Assert.assertTrue(genotype.equals("TACACAC/T")); //need to trim this
				
				GTType hetero = LSMparser.isHetero();
				Assert.assertTrue(hetero == GTType.HET);
						
				GTType homo = LSMparser.isHomo();
				Assert.assertTrue(homo == GTType.HET);
						
				Boolean phase = LSMparser.isPhased();
				Assert.assertFalse(phase);
											
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(190068427)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("TACACAC"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("T"));
				
				Integer depth = LSMparser.getDepth();
				Assert.assertTrue(depth==13);

				Integer varDepth = LSMparser.getVariantDepth();
				Assert.assertTrue(varDepth==6);
			}
			
			// manta, DEL
			if (i == 2) { 
				String contig = LSMparser.getContig();
				Assert.assertTrue(var.getContig().equals("2"));
				
				Integer pos = LSMparser.getPos();
				Assert.assertTrue(var.getStart() == pos);
				Assert.assertTrue(pos == 25475131);
				
				String ref = LSMparser.getRef();
				Assert.assertTrue(var.getRef().equals(ref));
				Assert.assertTrue(ref.equals("G"));
				
				String alt = LSMparser.getAlt();
				Assert.assertTrue(var.getAlt().equals(alt));
				Assert.assertTrue(alt.equals("<DEL>"));					

				Assert.assertTrue(var.getGenotype().equals(".")); //no GT field
				
				GTType hetero = LSMparser.isHetero();
				Assert.assertTrue(hetero == GTType.UNKNOWN); //no GT field
				Assert.assertTrue(var.getZygosity() == GTType.UNKNOWN);
						
				GTType homo = LSMparser.isHomo();
				Assert.assertTrue(homo == GTType.UNKNOWN);
						
				Boolean phase = LSMparser.isPhased();
				Assert.assertFalse(phase);

				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(25475131)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("G"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("<DEL>"));
											
				Integer depth = LSMparser.getDepth();
				Assert.assertTrue(var.getProperty(VariantRec.DEPTH).equals( new Double(depth)));
				int DepthSum = 44 + 22;
				Assert.assertTrue(depth==DepthSum);

				Integer varDepth = LSMparser.getVariantDepth();
				Assert.assertTrue(varDepth==22);
						
				//Manta doesn't name give GQ a key, just IMPRECISE
				Assert.assertTrue(var.getAnnotation(VariantRec.GENOTYPE_QUALITY) == null);
						
				Double strandbias = LSMparser.getStrandBiasScore();
				Assert.assertTrue(strandbias.equals(-1.0));
						
				Double rp = LSMparser.getRPScore();
				Assert.assertTrue(rp.equals(-1.0));
				
			}
			
			
			// manta, INV (4th variant)
			if (i == 3) { 
				String contig = LSMparser.getContig();
				Assert.assertTrue(var.getContig().equals("5"));
				
				Integer pos = LSMparser.getPos();
				Assert.assertTrue(var.getStart() == pos);
				Assert.assertTrue(pos == 170834881);
				
				String ref = LSMparser.getRef();
				Assert.assertTrue(var.getRef().equals(ref));
				Assert.assertTrue(ref.equals("C"));
				
				String alt = LSMparser.getAlt();
				Assert.assertTrue(var.getAlt().equals(alt));
				Assert.assertTrue(alt.equals("<INV>"));					

				Assert.assertTrue(var.getGenotype().equals(".")); //no GT field
				
				GTType hetero = LSMparser.isHetero();
				Assert.assertTrue(hetero == GTType.UNKNOWN); //no GT field
				Assert.assertTrue(var.getZygosity() == GTType.UNKNOWN);
						
				GTType homo = LSMparser.isHomo();
				Assert.assertTrue(homo == GTType.UNKNOWN);
						
				Boolean phase = LSMparser.isPhased();
				Assert.assertFalse(phase);

				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(170834881)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("C"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("<INV>"));
											
				Integer depth = LSMparser.getDepth();
				Assert.assertTrue(var.getProperty(VariantRec.DEPTH).equals( new Double(depth)));
				int DepthSum = 163 + 1 + 1157 + 6;
				Assert.assertTrue(depth==DepthSum);

				Integer varDepth = LSMparser.getVariantDepth();
				int varDepthSum = 1 + 6;
				Assert.assertTrue(varDepth==varDepthSum);
						
				//Manta doesn't name give GQ a key, just IMPRECISE
				Assert.assertTrue(var.getAnnotation(VariantRec.GENOTYPE_QUALITY) == null);
						
				Double strandbias = LSMparser.getStrandBiasScore();
				Assert.assertTrue(strandbias.equals(-1.0));
						
				Double rp = LSMparser.getRPScore();
				Assert.assertTrue(rp.equals(-1.0));
				
			}
			
			i++;
									
		}
		
		System.out.println("\tVCFParser tests for parsing variants (not normalized) passed on a lofreq-scalpel-manta VCF.");
	}
	
	@Test
	public void TestNormalizeParseLoFreqScalpelMantaVariants() throws IOException {
		VCFParser normLSMparser = new VCFParser(LoFreqScalpelMantaVCF);		

		System.out.println("Lofreq-Scalpel-Manta VCF test, parse variants with normalization");
		int i=0;

		//Go through file
		while(normLSMparser.advanceLine() && i<5) {	
			VariantRec var = normLSMparser.toVariantRec();
			var = VCFParser.normalizeVariant(var);
			System.out.println(var.toSimpleString());		

			// Check second variant, from Scalpel (nothing to normalize)
			if (i == 1) {
				Integer pos = normLSMparser.getPos();
				Assert.assertTrue(pos == 190068427);
				
				String ref = normLSMparser.getRef();
				Assert.assertTrue(ref.equals("TACACAC"));
				
				String alt = normLSMparser.getAlt();
				Assert.assertTrue(alt.equals("T"));		
				
				String genotype = normLSMparser.getGT();
				Assert.assertTrue(genotype.equals("ACACAC/-")); 
				
				GTType hetero = normLSMparser.isHetero();
				Assert.assertTrue(hetero == GTType.HET);
						
				GTType homo = normLSMparser.isHomo();
				Assert.assertTrue(homo == GTType.HET);
						
				Boolean phase = normLSMparser.isPhased();
				Assert.assertFalse(phase);
											
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(190068427)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("TACACAC"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("T"));
				
				Integer depth = normLSMparser.getDepth();
				Assert.assertTrue(depth==13);

				Integer varDepth = normLSMparser.getVariantDepth();
				Assert.assertTrue(varDepth==6);
			}
			
			// Check last variant from scalpel, requires normalization
			if (i == 4) { 
				String contig = normLSMparser.getContig();
				Assert.assertTrue(var.getContig().equals("5"));
				
				Integer pos = normLSMparser.getPos();
				Assert.assertTrue(var.getStart() == 170837223);
				
				Assert.assertTrue(var.getRef().equals("-"));
				Assert.assertTrue(var.getAlt().equals("AG"));				

				String data = normLSMparser.getGT();
				Assert.assertTrue(var.getGenotype().equals("AG/AG")); 
				
				GTType hetero = normLSMparser.isHetero();
				Assert.assertTrue(hetero == GTType.HOM); 
				Assert.assertTrue(var.getZygosity() == GTType.HOM);
						
				GTType homo = normLSMparser.isHomo();
				Assert.assertTrue(homo == GTType.HOM);
						
				Boolean phase = normLSMparser.isPhased();
				Assert.assertFalse(phase);

				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(170837222)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("C"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("CAG"));
											
				Integer depth = normLSMparser.getDepth();
				Assert.assertTrue(var.getProperty(VariantRec.DEPTH).equals( new Double(depth)));
				Assert.assertTrue(depth==73);

				Integer varDepth = normLSMparser.getVariantDepth();
				Assert.assertTrue(varDepth==73);
						
				//Manta doesn't name give GQ a key, just IMPRECISE
				Assert.assertTrue(var.getAnnotation(VariantRec.GENOTYPE_QUALITY) == null);
						
				Double strandbias = normLSMparser.getStrandBiasScore();
				Assert.assertTrue(strandbias.equals(-1.0));
						
				Double rp = normLSMparser.getRPScore();
				Assert.assertTrue(rp.equals(-1.0));
				
			}
			
			i++;
									
		}
		
		System.out.println("\tVCFParser tests for parsing normalized variants passed on a lofreq-scalpel-manta VCF.");
	}
	
	@Test
	public void TestSolidIonTorrentVCFVars() throws IOException {
		//Check fields for solid tumor VCF (Ion Torrent), normalized.
		System.out.println("Solid Tumor IonTorrent VCF test, normalized:");
		VCFParser parserTorr = new VCFParser(solidTumorVCF);
		
		
		//Go through file
		int i=0;
		while(parserTorr.advanceLine()) {	
			// Check third variant
			if (i == 2) {		
				VariantRec var = parserTorr.toVariantRec();
				var = parserTorr.normalizeVariant(var);
				
				Assert.assertTrue(var.getRef().equals("A"));
				
				String genotype = parserTorr.getGT();
				Assert.assertTrue(genotype.equals("G/G")); 
				
				GTType hetero = parserTorr.isHetero();
				Assert.assertTrue(hetero == GTType.HOM);
						
				GTType homo = parserTorr.isHomo();
				Assert.assertTrue(homo == GTType.HOM);
						
				Boolean phase = parserTorr.isPhased();
				Assert.assertFalse(phase);
				
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(55141055)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("A"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("G"));
				
				Integer depth = parserTorr.getDepth();
				Assert.assertTrue(depth==1119);

				Integer varDepth = parserTorr.getVariantDepth();
				Assert.assertTrue(varDepth==1119);
						
				String genotypeQual = parserTorr.getGenotypeQuality();
				Assert.assertTrue(genotypeQual.equals("99"));
					
				Double vqsr = parserTorr.getVQSR();
				Assert.assertTrue(vqsr.equals(-1.0));
						
				Double fs = parserTorr.getStrandBiasScore();
				Assert.assertTrue(fs.equals(0.5));
				
				Double rp = parserTorr.getRPScore();
				Assert.assertTrue(rp.equals(-1.0));
			}
			
			// Check second to last variant, which will break VCFParser since GT is 2/3 but there is just 1 alt
			if (i == 19) {					
				//Variant is not compatible with GT
				try {
					VariantRec var = parserTorr.toVariantRec();
					Assert.assertTrue(false); //should not get to this step
				} catch (IllegalStateException e) {
					// expect this variant to break VCFParser
				}
				
			}
							
			i++;		

		}
		System.out.println("\tVCFParser tests for parsing normalized variants passed on a solid tumor VCF (Ion Torrent).");
	}
	
	@Test
	public void TestNormSolidIonTorrentVars() throws IOException {
		//Check fields for solid tumor VCF (Ion Torrent), do strip trailing matching bases
		System.out.println("Solid Tumor Ion Torrent VCF test, normalized:");
		VCFParser parserTorrStrip = new VCFParser(solidTumorVCF);
		
		//Go through file
		int i=0;
		while(parserTorrStrip.advanceLine() && i < 20) {
					
			// Check third variant
			if (i == 2) {
				VariantRec var = parserTorrStrip.toVariantRec();
				var = VCFParser.normalizeVariant(var);
				
				String genotype = parserTorrStrip.getGT();
				Assert.assertTrue(genotype.equals("G/G"));
					
				GTType hetero = parserTorrStrip.isHetero();
				Assert.assertTrue(hetero == GTType.HOM);
					
				GTType homo = parserTorrStrip.isHomo();
				Assert.assertTrue(homo == GTType.HOM);
						
				Boolean phase = parserTorrStrip.isPhased();
				Assert.assertFalse(phase);
				
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(55141055)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("A"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("G"));				
						
				Integer depth = parserTorrStrip.getDepth();
				Assert.assertTrue(depth==1119);

				Integer varDepth = parserTorrStrip.getVariantDepth();
				Assert.assertTrue(varDepth==1119);
						
				String genotypeQual = parserTorrStrip.getGenotypeQuality();
				Assert.assertTrue(genotypeQual.equals("99"));
					
				Double vqsr = parserTorrStrip.getVQSR();
				Assert.assertTrue(vqsr.equals(-1.0));
					
				Double fs = parserTorrStrip.getStrandBiasScore();
				Assert.assertTrue(fs.equals(0.5));
				
				Double rp = parserTorrStrip.getRPScore();
				Assert.assertTrue(rp.equals(-1.0));
				
			} 
			
			//Check second to last variant
			if (i==19) {
				//Variant is not compatible with GT
				try {
					VariantRec var = parserTorrStrip.toVariantRec();
					Assert.assertTrue(false); //should not get to this step
				} catch (IllegalStateException e) {
					// expect this variant to break VCFParser
				}
			}		
						
			i++;		

		}
		System.out.println("\tVCFParser tests for parsing normalized variants passed on a solid tumor VCF (Ion Torrent) (strip trailing matching bases).");
		
	}
	
	@Test
	public void TestBcrAblIonTorrentVCF() throws IOException {
		//Check fields for bcr-abl VCF (Ion Torrent)

		System.out.println("BCR-ABL IonTorrent VCF test, normalized:");
		VCFParser parserBCR = new VCFParser(bcrablVCF);
		//Go through file
		int i=0;
		while(parserBCR.advanceLine() && i<2) {	
			VariantRec var = parserBCR.toVariantRec();
			var = parserBCR.normalizeVariant(var);

			// Check second variant
			if (i == 1) {
				String genotype = parserBCR.getGT();
				Assert.assertTrue(genotype.equals("C/T"));
				
				GTType hetero = parserBCR.isHetero();
				Assert.assertTrue(hetero == GTType.HET);
								
				GTType homo = parserBCR.isHomo();
				Assert.assertTrue(homo == GTType.HET);
								
				Boolean phase = parserBCR.isPhased();
				Assert.assertFalse(phase);
				
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(944)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("C"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("T"));
								
				Integer depth = parserBCR.getDepth();
				Assert.assertTrue(depth==1983);

				Integer varDepth = parserBCR.getVariantDepth();
				Assert.assertTrue(varDepth==444);
								
				Assert.assertTrue(var.getAnnotation(VariantRec.GENOTYPE_QUALITY).equals("99"));
							
				Double vqsr = parserBCR.getVQSR();
				Assert.assertTrue(vqsr.equals(-1.0));
								
				Double fs = parserBCR.getStrandBiasScore();
				Assert.assertTrue(fs.equals(0.518097));
						
				Double rp = parserBCR.getRPScore();
				Assert.assertTrue(rp.equals(-1.0));
			}
			i++;		

		}
		System.out.println("\tVCFParser tests for parsing normalized variants passed on a bcr-abl VCF (Ion Torrent).");
	
	}
	
	@Test
	public void TestEmptyVCF() throws IOException {
		//Check fields for empty VCF 
		System.out.println("Empty VCF test, normalize:");
		VCFParser parserEmpty = new VCFParser(emptyVCF);
		
		//Go through file			
		while(parserEmpty.advanceLine()) {	
			VariantRec var = parserEmpty.toVariantRec();
			var = parserEmpty.normalizeVariant(var);
			
			String genotype = parserEmpty.getGT();
			Assert.assertTrue(genotype.equals("-"));
						
			GTType hetero = parserEmpty.isHetero();
			Assert.assertFalse(hetero == GTType.UNKNOWN);
							
			GTType homo = parserEmpty.isHomo();
			Assert.assertTrue(homo == GTType.UNKNOWN);
							
			Boolean phase = parserEmpty.isPhased();
			Assert.assertFalse(phase);
			
			Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(-1)));
			Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("-"));
			Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("-"));
								
			Integer depth = parserEmpty.getDepth();
			Assert.assertTrue(depth==-1);

			Integer varDepth = parserEmpty.getVariantDepth();
			Assert.assertTrue(varDepth==-1);
							
			Assert.assertTrue(var.getAnnotation(VariantRec.GENOTYPE_QUALITY) == null);
								
			Double vqsr = parserEmpty.getVQSR();
			Assert.assertTrue(vqsr.equals(-1.0));
								
			Double fs = parserEmpty.getStrandBiasScore();
			Assert.assertTrue(fs.equals(-1.0));
						
			Double rp = parserEmpty.getRPScore();
			Assert.assertTrue(rp.equals(-1.0));
		}
	
		System.out.println("\tVCFParser tests for parsing normalized variants passed on an empty VCF.");
	}
	 
	@Test
	public void TestZygosity() throws IOException {				

		System.out.println("Zygosity test, normalized:");
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
				
				String genotype = parserPanel.getGT();
				Assert.assertTrue(genotype.equals("G/A"));
		
				GTType hetero = parserPanel.isHetero();
				Assert.assertTrue(hetero == GTType.HET);
						
				GTType homo = parserPanel.isHomo();
				Assert.assertTrue(homo == GTType.HET);
				
				GTType hom = (var.getZygosity());
				Assert.assertTrue(hom == GTType.HET);
				
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
					
					String genotype = parserPanel.getGT();
					Assert.assertTrue(genotype.equals("T/T"));
			
					GTType hetero = parserPanel.isHetero();
					Assert.assertTrue(hetero == GTType.HOM);
							
					GTType homo = parserPanel.isHomo();
					Assert.assertTrue(homo == GTType.HOM);
					
					GTType hom = var.getZygosity(); //Test the variant itself
					Assert.assertTrue(hom == GTType.HOM);
				}			
			else if (i == 15) {
				// Check the 16th variant (hom)
						Integer pos = parserPanel.getPos();
						Assert.assertTrue(pos == 228163453);
						
						String genotype = parserPanel.getGT();
						Assert.assertTrue(genotype.equals("A/A"));
								
						GTType hetero = parserPanel.isHetero();
						Assert.assertTrue(hetero == GTType.HOM);
								
						GTType homo = parserPanel.isHomo();
						Assert.assertTrue(homo == GTType.HOM);
						
						GTType hom = var.getZygosity(); //Test the variant itself
						Assert.assertTrue(hom == GTType.HOM);
					}			
			else if (i == 16) {
				// Check the 17th variant (hom)
						Integer pos = parserPanel.getPos();
						Assert.assertTrue(pos == 228168748);
						
						String genotype = parserPanel.getGT();
						Assert.assertTrue(genotype.equals("A/A"));
								
						GTType hetero = parserPanel.isHetero();
						Assert.assertTrue(hetero == GTType.HOM);
								
						GTType homo = parserPanel.isHomo();
						Assert.assertTrue(homo == GTType.HOM);
						
						GTType hom = var.getZygosity(); //Test the variant itself
						Assert.assertTrue(hom == GTType.HOM);
					}		
				i++;							
		}
									
		System.out.println("\tVCFParser tests on parsing homozygotes/heterozytgotes passed on panel VCF.");

	}
		
	
	
	@Test
	public void TestCompleteGenomicsVCF() throws IOException {

		VCFParser parserGT = new VCFParser(completeGenomicsVCF);
		
					//Go through file
		int i=0;
		while(parserGT.advanceLine() && i < 9) {	
			VariantRec var = parserGT.toVariantRec();
			var = parserGT.normalizeVariant(var);
			
			if (i == 0) {	
				// Check the first variant (GT="1/.")
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(2668245)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("CA"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("C"));

				Assert.assertTrue(var.getStart() == 2668246);
				String genotype = parserGT.getGT();
				Assert.assertTrue(genotype.equals("-/."));
				
				GTType hetero = parserGT.isHetero();
				Assert.assertTrue(hetero == GTType.HET);
			} else if (i == 1) {
				// Check the 2nd  variant (GT="1|0")
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(2289063)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("T"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("G"));

				String genotype = parserGT.getGT();
				Assert.assertTrue(genotype.equals("G|T"));
				
				GTType hetero = parserGT.isHetero();
				Assert.assertTrue(hetero == GTType.HET);
			} else if (i == 2) {
				// Check the 3rd  variant (GT="0|.")
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(1813054)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("T"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("."));
				
				String genotype = parserGT.getGT();
				Assert.assertTrue(genotype.equals("T|."));
				
				GTType hetero = parserGT.isHetero();
				Assert.assertTrue(hetero == GTType.UNKNOWN);
			} else if (i == 3) {
				// Check the 4th  variant (GT=".")
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(2699516)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("GTTAA"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("."));
				
				String genotype = parserGT.getGT();
				Assert.assertTrue(genotype.equals("."));
				
				GTType hetero = parserGT.isHetero();
				Assert.assertTrue(hetero == GTType.UNKNOWN);
			} else if (i == 4) {
				// Check the last  variant (GT="1")
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(2701185)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("C"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("T"));
		
				String genotype = parserGT.getGT();
				Assert.assertTrue(genotype.equals("T"));
				
				GTType hetero = parserGT.isHetero();
				Assert.assertTrue(hetero == GTType.HEMI);
			} else if (i == 5) {
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(2701200)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("CA"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("C"));
		
				String genotype = parserGT.getGT();
				Assert.assertTrue(genotype.equals("./A"));
				
				GTType hetero = parserGT.isHetero();
				Assert.assertTrue(hetero == GTType.UNKNOWN);
                        } else if (i == 6) {
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(2701210)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("CA"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("C"));
		
				String genotype = parserGT.getGT();
				Assert.assertTrue(genotype.equals("./-"));
				
				GTType hetero = parserGT.isHetero();
				Assert.assertTrue(hetero == GTType.HET);
                        } else if (i == 7) {
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(2701220)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("CA"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("C"));
		
				String genotype = parserGT.getGT();
				Assert.assertTrue(genotype.equals("A/."));
				
				GTType hetero = parserGT.isHetero();
				Assert.assertTrue(hetero == GTType.UNKNOWN);

                        } else if (i == 8) {
				Assert.assertTrue(var.getProperty(VariantRec.VCF_POS).equals(new Double(2701230)));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_REF).equals("CA"));
				Assert.assertTrue(var.getAnnotation(VariantRec.VCF_ALT).equals("C"));
		
				String genotype = parserGT.getGT();
				Assert.assertTrue(genotype.equals("./."));
				
				GTType hetero = parserGT.isHetero();
				Assert.assertTrue(hetero == GTType.UNKNOWN);
                        } 
                   
			
			i++;		
		}
		
		System.out.println("\tVCFParser tests on parsing genotypes from Complete Genomics VCF passed (normalized variants).");			
		
	}
	
	
	@Test
	public void TestParseMNPProperties() throws IOException{
		
		VCFParser parser = new VCFParser(germlineMNPSVCF);
		List<VariantRec> vars = new ArrayList<VariantRec>();
		while(parser.advanceLine()) {
			VariantRec var = parser.toVariantRec();
			vars.add(var);
		}
		
		Assert.assertTrue(vars.size() == 3);
		Assert.assertTrue(vars.get(0).getProperty("var.freq") == 0.4651);
		Assert.assertTrue(vars.get(0).getProperty(VariantRec.DEPTH) == 43);
		
		Assert.assertTrue(vars.get(1).getProperty("var.freq") == 0.1250);
		Assert.assertTrue(vars.get(1).getProperty(VariantRec.DEPTH) == 8);
		
		Assert.assertTrue(vars.get(2).getProperty("var.freq") == 1.0);
		Assert.assertTrue(vars.get(2).getProperty(VariantRec.DEPTH) == 58);
		
	}

	// Test single-sample Lithium filtered VCF
	@Test
	public void TestLithiumFilterVCF() throws IOException {	
		System.err.println("Testing VCFLineParser: Lithium Filtered VCF ...");

		VCFParser parser = new VCFParser(lithiumVCF);
		List<VariantRec> vars = new ArrayList<VariantRec>();
		while(parser.advanceLine()) {
			VariantRec var = parser.toVariantRec();
			vars.add(var);
		}
		Assert.assertTrue(vars.size() == 4);
					
	        // Check Lithium Deletion Score: "MLDEL"
	        Double lithiumDel = vars.get(0).getProperty("lithium.del.score");
                Assert.assertTrue(lithiumDel == 0.544549);	
                Assert.assertTrue(vars.get(0).getProperty("lithium.ins.score") == null);
									
	        // Check Lithium insertion score: "MLINS"
	        Double lithiumIns = vars.get(1).getProperty("lithium.ins.score");
                Assert.assertTrue(lithiumIns == 0.9411);	
                Assert.assertTrue(vars.get(1).getProperty("lithium.del.score") == null);
						

                // The third SNV does not have Lithium SNV score
                Assert.assertTrue(vars.get(2).getProperty("lithium.snv.score") == null);

                // The fourth variant, a SNV has Lithiium SNV score 
                Double lithiumSNV = vars.get(3).getProperty("lithium.snv.score");
                Assert.assertTrue(lithiumSNV == 0.25);
               
		System.err.println("\tVCFLineParser tests passed on Lithium Filtered VCF.");

	}

}
	

	
