package coverage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

import org.junit.Assert;
import org.junit.Test;

import util.Interval;
import util.coverage.CoverageCalculator;
import util.coverage.CoverageCalculator.IntervalCovSummary;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.IntervalsFile;

public class TestFastDOC {

	/**
	 * Test the new util.CoverageCalculator tool
	 */
	@Test
	public void TestFastDOCComputations() {
		
		
		BAMFile testBam = new BAMFile(new File("src/test/java/testbams/tinybam.bam"));
		
		//First just test to make sure we get back some data and no errors are thrown. 
		try {
			int[] depths = computeCovForRegion(testBam.getFile(), "12", 52305000, 52311000);
			Assert.assertNotNull(depths);
			Assert.assertTrue(depths.length > 0);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		}
		
		
		
		//Check out a space with no coverage...
		try {
			
			int[] depths = computeCovForRegion(testBam.getFile(), "1", 100, 200);
			Assert.assertEquals(100, depths[0]);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		}
		
		
		//This contig has reads but not in the intervals given
		try {
			
			int[] depths = computeCovForRegion(testBam.getFile(), "12", 100, 200);
			Assert.assertEquals(100, depths[0]);
			depths = computeCovForRegion(testBam.getFile(), "12", 60000000, 60000100);
			Assert.assertEquals(100, depths[0]);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		}




		//OK, now test an area that has some actual coverage
		try {
			int[] depths = computeCovForRegion(testBam.getFile(), "12", 52305854, 52305858);
			Assert.assertEquals(0, depths[0]);
			Assert.assertEquals(4, depths[1]);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		}


		//Look at a few single sites...

		try {
			int[] depths = computeCovForRegion(testBam.getFile(), "12", 52306120, 52306121);
			Assert.assertEquals(0, depths[0]);
			Assert.assertEquals(1, depths[6]);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		}


		try {
			int[] depths = computeCovForRegion(testBam.getFile(), "12", 52306240, 52306241);
			Assert.assertEquals(0, depths[0]);
			Assert.assertEquals(1, depths[20]);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		}

		
		try {
			int[] depths = computeCovForRegion(testBam.getFile(), "12", 52308915, 52308916);
			Assert.assertEquals(0, depths[0]);
			Assert.assertEquals(1, depths[16]);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		}

		
		try {
			int[] depths = computeCovForRegion(testBam.getFile(), "12", 52306000, 52307600);
			
			double[] props = CoverageCalculator.convertCountsToProportions(depths);
			double prev = 100.0;
			for(int i=0; i<props.length-1; i++) {
				Assert.assertTrue(props[i] <= prev);
				Assert.assertTrue(props[i] <= 100.0);
				Assert.assertTrue(props[i] >= 0.0);
				prev = props[i];
			}
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

	/*
	 * Test the minMQ parameter for this coverage tool.
	 */
	@Test
	public void TestMinMQ() throws IOException, InterruptedException{
		File bamFile = new File("src/test/java/testbams/Tiny.MTOR.bam") ;
		IntervalsFile intervals = new BEDFile(new File("src/test/java/testBEDs/TinyMTOR.bed"));
		CoverageCalculator calc0 = new CoverageCalculator(bamFile, intervals, 0);
		CoverageCalculator calc61 = new CoverageCalculator(bamFile, intervals, 61);
		CoverageCalculator calc60 = new CoverageCalculator(bamFile, intervals, 60);
		List<IntervalCovSummary> depths0 = calc0.computeCoverageByInterval();
		List<IntervalCovSummary> depths61 = calc61.computeCoverageByInterval();
		List<IntervalCovSummary> depths60 = calc60.computeCoverageByInterval();
		
		double totalExtent = 0;
		double totalDepth = 0;
		for(IntervalCovSummary cov : depths0) {
			totalExtent += (double)cov.intervalSize();
			totalDepth += cov.meanDepth*cov.intervalSize();
		}
		double grandMeanDepth0 = totalDepth / totalExtent;
		
		totalExtent = 0;
		totalDepth = 0;
		for(IntervalCovSummary cov : depths61) {
			totalExtent += (double)cov.intervalSize();
			totalDepth += cov.meanDepth*cov.intervalSize();
		}
		double grandMeanDepth61 = totalDepth / totalExtent;
		
		totalExtent = 0;
		totalDepth = 0;
		for(IntervalCovSummary cov : depths60) {
			totalExtent += (double)cov.intervalSize();
			totalDepth += cov.meanDepth*cov.intervalSize();
		}
		double grandMeanDepth60 = totalDepth / totalExtent;
		
		// Tests that the original value matches.
		Assert.assertTrue(Math.round(grandMeanDepth0) == 977);
		// Test that no reads in this set are above 60 MQ. (BWA)
		Assert.assertTrue(Math.round(grandMeanDepth61) == 0);
		// Test that the result is roughly what samtools view -L with minMQ looks like.
		Assert.assertTrue(Math.round(grandMeanDepth60) == 970);
	}
	
	public static int[] computeCovForRegion(File bamFile, String contig, int start, int end) throws IOException, InterruptedException {
		IntervalsFile intervals = new BEDFile(); //fake bed file, we just add an interval or two programmatically
		Interval interval = new Interval(start, end);
		List<Interval> intervalList = new ArrayList<Interval>();
		intervalList.add(interval);
		intervals.addIntervals(contig, intervalList);

		//First just test to make sure we get back some data and no errors are thrown. 
		CoverageCalculator calc = new CoverageCalculator(bamFile, intervals);
		int[] depths = calc.computeOverallCoverage();
		Assert.assertNotNull(depths);
		Assert.assertTrue(depths.length > 0);
		return depths;
	}
}
