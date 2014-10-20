package intervals;

import java.io.File;

import org.junit.Test;

import buffer.BEDFile;

public class TestIntervals {
	
	String bed1Path = "src/test/java/testBEDs/singleInterval.bed";
	
	@Test
	public void TestBEDFileReading() {
		BEDFile bed1 = new BEDFile(new File(bed1Path));
		
	}

}
