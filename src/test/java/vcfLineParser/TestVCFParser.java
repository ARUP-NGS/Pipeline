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
			
			System.err.println("\tVCFParser tests passed on single-sample FreeBayes VCF.");
			
			
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

			System.err.println("\tVCFParser tests passed on single-sample GATK VCF.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
	}
	
	@Test
	public void TestParseVariants() {
		System.err.println("here");
		try {
			VCFParser parser = new VCFParser(complexVCF);
			int i=0;
	
			//Go through file
			while(parser.advanceLine()) {	

				VariantRec var = parser.toVariantRec();
				System.out.println(var.toSimpleString());		
				
				// Check first variant, first alt
				if (i == 0) {
					Integer depth = parser.getDepth();
					Assert.assertTrue(depth==18);

					Integer varDepth = parser.getVariantDepth();
					System.out.println(depth);
					Assert.assertTrue(varDepth==9);
				}
				
/*				// Check first variant, second alt
				if (i == 1) {
					Integer depth = parser.getDepth();
					Assert.assertTrue(depth==18);

					Integer varDepth = parser.getVariantDepth();
					Assert.assertTrue(varDepth==19);
				}*/
				
				i++;
										
			}
			
			System.err.println("\tVCFParser tests passed on a complex VCF.");
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}
}
