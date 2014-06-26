package fastaReader;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import util.FastaReader;

/**
 * Tests FastaReader: 
 * 1) Create HashMap of contig-size pairs from fasta file
 * 2) Create string array of contigs from fasta file
 * @author elainegee
 *
 */
public class TestFastaReader{
	File smallFasta = new File("src/test/java/testfasta/small.fasta");
	
	@Test
	public void TestChrMap() {
		FastaReader smallRef = null;
		try {
			smallRef = new FastaReader(smallFasta);
			
			Map<String, Integer> ContigSizeMap = smallRef.getContigSizes();
			Assert.assertTrue(smallRef.getContigSizes().size() == 2);		
			Assert.assertTrue(ContigSizeMap.get("1") == 541);
			Assert.assertTrue(ContigSizeMap.get("2") == 1253);
			
			String[] contigs=smallRef.getContigs();
			Assert.assertTrue(contigs.length == 2);
			Assert.assertTrue(contigs[0].equals("1")); 
			Assert.assertTrue(contigs[1].equals("2")); 
			
			
			System.err.println("FastaReader tests on parsing fasta passed on 'small.fasta'.");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		

		
	}
};