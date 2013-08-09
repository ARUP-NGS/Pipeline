package operator.bamutils;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import operator.IOOperator;
import operator.OperationFailedException;

import org.apache.log4j.Logger;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import buffer.BAMFile;

public class BQRebinOperator extends IOOperator {

	public static final String BINS = "bins";
	final int defaultBins = 4;
	int bins = defaultBins;
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		
		BAMFile inputBAM = (BAMFile) getInputBufferForClass(BAMFile.class);
		BAMFile outputBAM = (BAMFile) getOutputBufferForClass(BAMFile.class);
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("Rebinning base qualities in file " + inputBAM.getFilename() + " to output " + outputBAM.getFilename());
		
		rebinBAMFile(inputBAM, outputBAM);
	}
	
	protected void rebinBAMFile(BAMFile inputBAM, BAMFile output) {
		SAMFileReader.setDefaultValidationStringency(ValidationStringency.LENIENT);
		if (inputBAM.getFile() == null) {
			throw new IllegalArgumentException("File associated with inputBAM " + inputBAM.getAbsolutePath() + " is null");
		}
		final SAMFileReader inputSam = new SAMFileReader(inputBAM.getFile());
		inputSam.setValidationStringency(ValidationStringency.LENIENT);		

		SAMFileWriterFactory factory = new SAMFileWriterFactory();
		final SAMFileWriter writer = factory.makeBAMWriter(inputSam.getFileHeader(), false, output.getFile());
		
		long readCount = 0;
		for (final SAMRecord samRecord : inputSam) {
			readCount++;
			
			byte[] qualities = samRecord.getBaseQualities();
			byte[] newQualities = new byte[qualities.length];
			
			for(int i=0; i<qualities.length; i++) {
				int oldQuality = qualities[i];
				double fracQuality = oldQuality / 40.0;
				fracQuality = fracQuality > 1.0 ? 1.0 : fracQuality;
				int bin = (int)(Math.floor(bins*fracQuality+0.5));
				newQualities[i] = (byte)Math.floor( (double)bin / (double)bins * 40.0);
			}
			
			samRecord.setBaseQualities(newQualities);
			samRecord.setOriginalBaseQualities(null);
			writer.addAlignment(samRecord);
		}
		writer.close();
		inputSam.close();
	}
	
	public void initialize(NodeList children) {
		super.initialize(children);
		
		String binsAttr = this.getAttribute(BINS);
		if (binsAttr != null) {
			try {
				bins = Integer.parseInt(binsAttr);
			}
			catch (NumberFormatException nfe) {
				throw new IllegalArgumentException("Could not parse number of bins");
			}
		}
	}

}
