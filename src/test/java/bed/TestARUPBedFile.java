package bed;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;

import buffer.ArupBEDFile;
import buffer.ArupBEDFile.ARUPBedInterval;
import junit.framework.TestCase;
import util.Interval;

/**
 * Test our ability to read and parse ARUP BED files
 * @author brendan
 *
 */
public class TestARUPBedFile extends TestCase {

	final String arupBEDPath = "src/test/java/bed/data/test.bed";
	
	public void testRead() {
		ArupBEDFile bed = new ArupBEDFile(new File(arupBEDPath));
		
		try {
			bed.buildIntervalsMap();
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Error parsing bed file: " + e.getMessage());
			return;
		}
		
		List<Interval> intervals = bed.getOverlappingIntervals("3", 123356917);
		Assert.assertTrue(intervals.size()==1);
		
		ARUPBedInterval info = (ARUPBedInterval)intervals.get(0).getInfo();
		Assert.assertTrue( info.gene.equals("MYLK"));
		Assert.assertTrue( info.transcripts.length==1);
		Assert.assertTrue( info.transcripts[0].equals("NM_053025"));
		Assert.assertTrue( info.exonNum==29);
		
		
		//First line of file
		intervals = bed.getOverlappingIntervals("1", 2160124);
		Assert.assertTrue(intervals.size()==1);
		info = (ARUPBedInterval)intervals.get(0).getInfo();
		Assert.assertTrue( info.gene.equals("SKI"));
		Assert.assertTrue( info.transcripts.length==1);
		Assert.assertTrue( info.transcripts[0].equals("NM_003036"));
		Assert.assertTrue( info.exonNum==1);
		
		
		
		//Last line of file
		intervals = bed.getOverlappingIntervals("X", 153599233);
		Assert.assertTrue(intervals.size()==1);
		info = (ARUPBedInterval)intervals.get(0).getInfo();
		Assert.assertTrue( info.gene.equals("FLNA"));
		Assert.assertTrue( info.transcripts.length==1);
		Assert.assertTrue( info.transcripts[0].equals("NM_001456"));
		Assert.assertTrue( info.exonNum==2);
		
		
		
	}
}
