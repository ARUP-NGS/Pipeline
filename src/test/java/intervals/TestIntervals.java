package intervals;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import buffer.BEDFile;

public class TestIntervals {
	
	String bed1Path = "src/test/java/testBEDs/singleInterval.bed";
	String bed2Path = "src/test/java/testBEDs/small.bed";
	
	@Test
	public void TestBEDFileReading() {
		BEDFile bed1 = new BEDFile(new File(bed1Path));
		try {
			bed1.buildIntervalsMap();
		} catch (Exception ex) {
			Assert.fail();
		}
		
		Assert.assertTrue(bed1.getIntervalCount()==1);
		Assert.assertTrue(bed1.getExtent()==10);
		Assert.assertTrue(bed1.contains("1", 15));
		Assert.assertFalse(bed1.contains("1", 10));
		Assert.assertTrue(bed1.contains("1", 11));
		Assert.assertTrue(bed1.contains("1", 20));
		Assert.assertFalse(bed1.contains("1", 21));
		
		
		BEDFile bed2 = new BEDFile(new File(bed2Path));
		try {
			bed2.buildIntervalsMap();
		} catch (Exception ex) {
			Assert.fail();
		}
		
		Assert.assertTrue(bed2.getIntervalCount()==4);
		Assert.assertTrue(bed2.getExtent()==10+50+100+250);
		Assert.assertTrue(bed2.contains("1", 15));
		Assert.assertFalse(bed2.contains("1", 10));
		Assert.assertTrue(bed2.contains("1", 11));
		Assert.assertTrue(bed2.contains("1", 20));
		Assert.assertTrue(bed2.contains("1", 99));
		Assert.assertTrue(bed2.contains("1", 100));
		Assert.assertTrue(bed2.contains("1", 101));
		Assert.assertTrue(bed2.contains("1", 102));
		Assert.assertTrue(bed2.contains("1", 199));
		Assert.assertTrue(bed2.contains("1", 200));
		Assert.assertFalse(bed2.contains("1", 201));
		
		
		
	}

}

