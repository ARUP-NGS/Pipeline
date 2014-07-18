package operator.bamutils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.tools.ant.types.CommandlineJava.SysProperties;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.FileBuffer;

/**
 * Base class for operators that read the contents of bam files, do some processing, and emit a new
 * BAM file for each classification option. Currently only supports pass/fail categorization.
 * @author daniel
 *
 */
public abstract class BAMClassifier extends IOOperator {

	//Created a class for returning both a return value (e.g., Boolean) and an alignment record
	public class ReturnRecord {
		public final boolean returnValue;
		public final SAMRecord samRecord;
		public ReturnRecord(SAMRecord samRecord, boolean returnValue) {
			this.returnValue = returnValue;
			this.samRecord = samRecord;
		}
		
	}
	
	@Override
	public void performOperation() throws OperationFailedException, NumberFormatException, IOException {
				
		Logger.getLogger(Pipeline.primaryLoggerName).info("Initializing BAMClassifier " + getObjectLabel());
		
		BAMFile inputBAM = (BAMFile)super.getInputBufferForClass(BAMFile.class);
		List<FileBuffer> outputBAMs = super.getAllOutputBuffersForClass(BAMFile.class);
		BAMFile outputBAMpass = (BAMFile)outputBAMs.get(0);
		BAMFile outputBAMfail = (BAMFile)outputBAMs.get(1);
		if (inputBAM == null)
			throw new OperationFailedException("No input BAM file found", this);
		//TODO: Generalize to any number of output BAMs
		classifyBAMFile(inputBAM, outputBAMpass, outputBAMfail);
			
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("BAMClassifier " + getObjectLabel() + " has completed");
	}

	
	
	public void classifyBAMFile(BAMFile inputBAM, BAMFile outputBAMpass, BAMFile outputBAMfail) throws OperationFailedException, NumberFormatException, IOException {
		SAMFileReader.setDefaultValidationStringency(ValidationStringency.LENIENT);
		if (inputBAM.getFile() == null) {
			throw new IllegalArgumentException("File associated with inputBAM " + inputBAM.getAbsolutePath() + " is null");
		}
		final SAMFileReader inputSam = new SAMFileReader(inputBAM.getFile());
		inputSam.setValidationStringency(ValidationStringency.LENIENT);
		
		SAMFileWriterFactory factory = new SAMFileWriterFactory();
		System.out.println("Attempting to write passing records to file " + outputBAMpass.getAbsolutePath());
		System.out.println("Attempting to write failing records to file " + outputBAMfail.getAbsolutePath());
		
		final SAMFileWriter writerPass = factory.makeBAMWriter(inputSam.getFileHeader(), false, outputBAMpass.getFile());
		final SAMFileWriter writerFail = factory.makeBAMWriter(inputSam.getFileHeader(), false, outputBAMfail.getFile());
		
		long recordsRead = 0;
		long recordsPassed = 0;
		long recordsFailed = 0;
		long recordsWritten = 0;
		for (final SAMRecord samRecord : inputSam) {
			ReturnRecord returnRecord = processRecord(samRecord);
			SAMRecord outputRecord = returnRecord.samRecord;
			recordsRead++;
			
			if (returnRecord.returnValue == true) {
				writerPass.addAlignment(outputRecord);
				recordsWritten++;
				recordsPassed++;
			}
			else if (returnRecord.returnValue == false) {
				writerFail.addAlignment(outputRecord);
				recordsWritten++;
				recordsFailed++;
			}
			else {
				throw new OperationFailedException("This should never happen - requires that a boolean be neither true nor false", this);
			}
			
		}
		inputSam.close();
		Logger.getLogger(Pipeline.primaryLoggerName).info(getObjectLabel() + " wrote " + recordsWritten + " of " + recordsRead + " from file " + inputBAM.getAbsolutePath());
		Logger.getLogger(Pipeline.primaryLoggerName).info(getObjectLabel() + " passed " + recordsPassed + " of " + recordsRead + " from file " + inputBAM.getAbsolutePath());
		Logger.getLogger(Pipeline.primaryLoggerName).info(getObjectLabel() + " failed " + recordsFailed + " of " + recordsRead + " from file " + inputBAM.getAbsolutePath());
		inputSam.close();
		writerPass.close();
		writerFail.close();
	}
	
	/**
	 * Peform processing of single record from bam file, return record 
	 * if read should not be in output file. 
	 * @param samRecord
	 * @return
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 * @throws NumberFormatException 
	 * @throws OperationFailedException 
	 */
	public abstract ReturnRecord processRecord(SAMRecord samRecord) throws FileNotFoundException, NumberFormatException, IOException, OperationFailedException;
}
