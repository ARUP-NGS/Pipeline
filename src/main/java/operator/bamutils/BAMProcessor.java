package operator.bamutils;

import java.util.logging.Logger;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.BAMFile;

/**
 * Base class for operators that read the contents of bam files, do some processing, and emit a new
 * bam file. Right now concurrency has not been implemented, so just one at a time.
 * @author brendan
 *
 */
public abstract class BAMProcessor extends IOOperator {

	@Override
	public void performOperation() throws OperationFailedException {
				
		Logger.getLogger(Pipeline.primaryLoggerName).info("Initializing BAMProcessor " + getObjectLabel());
		
		BAMFile inputBAM = (BAMFile)super.getInputBufferForClass(BAMFile.class);
		BAMFile outputBAM = (BAMFile)super.getOutputBufferForClass(BAMFile.class);
		
		if (inputBAM == null)
			throw new OperationFailedException("No input BAM file found", this);

//		if (inputBAM == null) {
//			throw new OperationFailedException("No input BAM file found", this);

			
		processBAMFile(inputBAM, outputBAM);
			
			
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("BAMProcessor " + getObjectLabel() + " has completed");
	}

	
	
	public void processBAMFile(BAMFile inputBAM, BAMFile outputBAM) {
		SAMFileReader.setDefaultValidationStringency(ValidationStringency.LENIENT);
		if (inputBAM.getFile() == null) {
			throw new IllegalArgumentException("File associated with inputBAM " + inputBAM.getAbsolutePath() + " is null");
		}
		final SAMFileReader inputSam = new SAMFileReader(inputBAM.getFile());
		inputSam.setValidationStringency(ValidationStringency.LENIENT);
		
		SAMFileWriterFactory factory = new SAMFileWriterFactory();
		final SAMFileWriter writer = factory.makeBAMWriter(inputSam.getFileHeader(), false, outputBAM.getFile());
		
		long recordsRead = 0;
		long recordsWritten = 0;
		for (final SAMRecord samRecord : inputSam) {
			SAMRecord outputRecord = processRecord(samRecord);
			recordsRead++;
			
			if (outputRecord != null) {
				writer.addAlignment(outputRecord);
				recordsWritten++;
			}
			
			
			
		}
		
		Logger.getLogger(Pipeline.primaryLoggerName).info(getObjectLabel() + " wrote " + recordsWritten + " of " + recordsRead + " from file " + inputBAM.getAbsolutePath());
		inputSam.close();
		writer.close();
	}
	
	/**
	 * Peform processing of single record from bam file, return record to be written to output. Return null
	 * if read should not be in output file. 
	 * @param samRecord
	 * @return
	 */
	public abstract SAMRecord processRecord(SAMRecord samRecord);
}
