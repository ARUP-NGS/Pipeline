package operator.qc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.logging.Logger;

import math.Histogram;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMRecord;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.BAMFile;
import buffer.BAMMetrics;
import buffer.FileBuffer;
import buffer.TextBuffer;

/**
 * Computes some simple summary metrics for a bam file. BamMetrics means mostly base quality metrics, but also 
 * some general stats like number of aligned reads, duplicates, etc. 
 * @author brendan
 *
 */
public class BamMetrics extends IOOperator {

	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		FileBuffer inputBAM = super.getInputBufferForClass(BAMFile.class);
		if (inputBAM == null) {
			throw new OperationFailedException("No input bam file specified", this);
		}
		
		buffer.BAMMetrics metrics = (BAMMetrics) super.getOutputBufferForClass(buffer.BAMMetrics.class);;
		if (metrics == null) {
			throw new OperationFailedException("No output BAM metrics object specified", this);
		}
		
		
		logger.info("Computing summary metrics for input bam file " + inputBAM.getAbsolutePath());
		
		FileBuffer outputFile = getOutputBufferForClass(TextBuffer.class);
		
		
		computeBAMMetrics( (BAMFile)inputBAM, metrics);
		
		String metricsSummary = getBAMMetricsSummary(metrics);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.getFile()));
			writer.write(metricsSummary);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new OperationFailedException("Error writing to output file: " + outputFile.getAbsolutePath(), this);
		}
		
		logger.info("Done computing summary metrics for input bam file " + inputBAM.getAbsolutePath());
	}
	
	public static BAMMetrics computeBAMMetrics(BAMFile inputBAM) {
		return computeBAMMetrics(inputBAM, new buffer.BAMMetrics());
	}
	
	public static BAMMetrics computeBAMMetrics(BAMFile inputBAM, buffer.BAMMetrics metrics) {
		SAMFileReader.setDefaultValidationStringency(ValidationStringency.LENIENT);
		if (inputBAM.getFile() == null) {
			throw new IllegalArgumentException("File associated with inputBAM " + inputBAM.getAbsolutePath() + " is null");
		}
		final SAMFileReader inputSam = new SAMFileReader(inputBAM.getFile());
		inputSam.setValidationStringency(ValidationStringency.LENIENT);		

		int readCount = 0;
		int unmappedCount = 0;
		int unmappedMate = 0;
		Histogram mqHisto = new Histogram(0, 80, 80);
		Histogram insertSizeHisto = new Histogram(0, 1000, 100);
		Histogram baseQHisto = new Histogram(0, 51, 51);
		int dupCount = 0;
		int failsVendorQuality = 0;
		int hugeInsertSize = 0;
		long basesAbove30 = 0;
		long basesAbove20 = 0;
		long basesAbove10 = 0;
		long totalBaseCount = 0;
		Histogram[] posHisto = null; 
		
		final int baseSubSample = 4; //Only count 1 of every this many bases
		
		for (final SAMRecord samRecord : inputSam) {
			readCount++;
						
			mqHisto.addValue( samRecord.getMappingQuality() );
			
			if (samRecord.getMateUnmappedFlag())
				unmappedMate++;
			if (samRecord.getReadUnmappedFlag())
				unmappedCount++;
			if (samRecord.getReadFailsVendorQualityCheckFlag())
				failsVendorQuality++;
			if (samRecord.getDuplicateReadFlag())
				dupCount++;
			
			byte[] baseQuals = samRecord.getBaseQualities();
			if (samRecord.getSecondOfPairFlag()) {
				for(int i=0; i<baseQuals.length/2; i++) {
					byte tmp = baseQuals[i];
					int secondPos = baseQuals.length-i-1;
					baseQuals[i] = baseQuals[secondPos];
					baseQuals[secondPos] = tmp;
				}
			}
//			int[] baseQuals = new int[rawBaseQuals.length];
//			for(int i=0; i<baseQuals.length; i++) {
//				baseQuals[i] = (int)rawBaseQuals[i];
//			}
			
			if (posHisto == null) {
				posHisto = new Histogram[ Math.max(100, baseQuals.length) ];
				System.out.println("Creating new position histogram array with length : " + posHisto.length);
				for(int i=0; i<posHisto.length; i++)
					posHisto[i] = new Histogram(0, 40, 40);
			}
			
			
			int start = readCount % baseSubSample;
			for(int i=start; i<baseQuals.length; i+=baseSubSample) {
				final int bq = (int) baseQuals[i];
				if (bq > 30)
					basesAbove30+=baseSubSample;
				if (bq > 20)
					basesAbove20+=baseSubSample;
				if (bq > 10)
					basesAbove10+=baseSubSample;
				//int bq = (int)baseQuals[i];
				baseQHisto.addValue( bq );
				
				if (i < posHisto.length) {
					int index = i;
					if (samRecord.getSecondOfPairFlag()) //invert for reverse orientation
						index = posHisto.length - i -1;
					posHisto[index].addValue( bq );
				}
			}
			
			totalBaseCount += baseQuals.length;
			int insertSize = Math.abs( samRecord.getInferredInsertSize() );
			if (insertSize > 10000) {
				hugeInsertSize++;
			}
			else {
				insertSizeHisto.addValue(insertSize);
			}				
		} //end loop over all reads


		inputSam.close();
		
		metrics.path = inputBAM.getFile().getAbsolutePath();
		metrics.totalReads = readCount;
		metrics.unmappedReads = unmappedCount;
		metrics.duplicateReads = dupCount;
		metrics.unmappedMates = unmappedMate;
		metrics.lowVendorQualityReads = failsVendorQuality;
		metrics.insertSizeHistogram = insertSizeHisto;
		metrics.baseQualityHistogram = baseQHisto;
		metrics.hugeInsertSize = hugeInsertSize;
		metrics.basesQAbove10 = basesAbove10;
		metrics.basesQAbove20 = basesAbove20;
		metrics.basesQAbove30 = basesAbove30;
		metrics.mqHistogram = mqHisto;
		metrics.basesRead = totalBaseCount;
		metrics.readPosQualHistos = posHisto;
		return metrics;
	}
	
	public static String getBAMMetricsSummary(BAMMetrics metrics) {
		DecimalFormat formatter = new DecimalFormat("#0.00");
		String lineSep = System.getProperty("line.separator");
		StringBuilder sum = new StringBuilder("Summary for .bam file: " + metrics.path + lineSep);
		sum.append("Total number of reads : " + metrics.totalReads + lineSep);
		
		sum.append("Number of unmapped reads : " + metrics.unmappedReads + " ( " + formatter.format(100.0*metrics.unmappedReads / metrics.totalReads) + "% )" + lineSep);
		sum.append("Number of reads with unmapped mates : " + metrics.unmappedMates + " ( " + formatter.format(100.0*metrics.unmappedMates / metrics.totalReads) + "% )"+ lineSep);
		sum.append("Number of duplicate reads : " + metrics.duplicateReads + " ( " + formatter.format(100.0*metrics.duplicateReads / metrics.totalReads) + "% )" + lineSep);
		sum.append("Number of low vendor quality reads : " + metrics.duplicateReads + " ( " + formatter.format(100.0*metrics.lowVendorQualityReads / metrics.totalReads) + "% )" + lineSep);
		sum.append("Number of pairs with insert size > 10K : " + metrics.hugeInsertSize + lineSep);
		
		sum.append("Bases with quality > 30 : " + metrics.basesQAbove30 + " ( " + formatter.format(100.0*metrics.basesQAbove30 / metrics.basesRead) + "% )" + lineSep );
		sum.append("Bases with quality > 20 : " + metrics.basesQAbove20 + " ( " + formatter.format(100.0*metrics.basesQAbove20 / metrics.basesRead) + "% )" + lineSep );
		sum.append("Bases with quality > 10 : " + metrics.basesQAbove10 + " ( " + formatter.format(100.0*metrics.basesQAbove10 / metrics.basesRead) + "% )" + lineSep );
		
		sum.append(" Distribution of insert sizes : " + lineSep);
		sum.append(metrics.insertSizeHistogram.toString() + lineSep);
		sum.append(" Mean insert size:" + formatter.format(metrics.insertSizeHistogram.getMean()) + lineSep);		
		sum.append(" Stdev insert size:" + formatter.format(metrics.insertSizeHistogram.getStdev()) + lineSep );
		sum.append(" Insert size range: " + metrics.insertSizeHistogram.getMinValueAdded() + " - " + metrics.insertSizeHistogram.getMaxValueAdded() + lineSep );
		
		sum.append("Distribution of base qualities: " + lineSep);
		sum.append(metrics.baseQualityHistogram.toString() + lineSep);
		sum.append(" Mean quality :" + formatter.format(metrics.baseQualityHistogram.getMean()) + lineSep);		
		sum.append(" Stdev quality:" + formatter.format(metrics.baseQualityHistogram.getStdev()) + lineSep );
		
		
		return sum.toString();
	}
	

}
