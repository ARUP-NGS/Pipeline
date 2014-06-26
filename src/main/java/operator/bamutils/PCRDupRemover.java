package operator.bamutils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import operator.IOOperator;
import operator.OperationFailedException;
import util.bamWindow.BamWindow;
import util.bamWindow.MappedRead;
import buffer.BAMFile;

/**
 * Just an experimental operator for removing bam reads
 * @author brendan
 *
 */
public class PCRDupRemover extends IOOperator {

	@Override
	public void performOperation() throws OperationFailedException {
		
		//Create input bam reader & BamWindow object 
		BAMFile inputBam = (BAMFile) getInputBufferForClass(BAMFile.class);
		final SAMFileReader reader = new SAMFileReader(inputBam.getFile());
		reader.setValidationStringency(ValidationStringency.LENIENT);
		BamWindow window = new BamWindow(inputBam.getFile());
		
		//Create output BAM writer...
		BAMFile outputBam = (BAMFile) getOutputBufferForClass(BAMFile.class);
		SAMFileWriterFactory factory = new SAMFileWriterFactory();
		final SAMFileWriter writer = factory.makeBAMWriter(reader.getFileHeader(), false, outputBam.getFile());

		
		long reads = 0;
		long readsWritten = 0;
		for(String contig : window.getContigs()) {
			System.out.println("Processing contig " + contig);
			window.advanceToContig(contig);
			window.advanceBy(100);
			int contigLength = window.getContigMap().get(contig);
			while(window.getCurrentPosition() < contigLength && window.hasMoreReadsInCurrentContig()) {
				
				int windowExtent = window.extent();
				if (windowExtent == 0) {
					windowExtent = 100;
				}
				reads += window.size();
				
				//System.out.println("Advancing by " + windowSize + " from " + contig + ": " + window.getCurrentPosition());
				readsWritten += processWindow(window, writer);
				
				try {
					window.advanceBy(windowExtent-5);
				}
				catch (IllegalArgumentException ex) {
					//dont sweat it, but 
					System.err.println("Error reading contig " + contig + ": " + ex.getLocalizedMessage() + " skipping this contig.");
					break;
				}
			}
		}
		System.out.println("Read " + reads + " total reads");
		System.out.println("Wrote " + readsWritten + " total reads");
		reader.close();
		writer.close();
		window.close();
	
	}
	
	/**
	 * Take a window, iterator over all reads in it, and figure out what we'd like to include in the output
	 * @param window
	 * @param writer
	 */
	private static int processWindow(BamWindow window, SAMFileWriter writer) {
		Iterator<MappedRead> it = window.getIterator();
		
		//The reads are sorted by start position, so to identify groups of reads that share the same start
		
		int prevStartPos = -1;
		int readsWritten = 0;
		List<MappedRead> readsSharingStartPos = new ArrayList<MappedRead>(128);
		while(it.hasNext()) {
			MappedRead read = it.next();
			int startPos = read.getRecord().getAlignmentStart();
			System.out.println("Read start " + read.getRecord().getAlignmentStart());
			
			if (startPos != prevStartPos && readsSharingStartPos.size()>0) { 
				System.out.println("Processing new set of " + readsSharingStartPos.size() + " reads");
				readsWritten += processSameStartReads(readsSharingStartPos, writer);
				readsSharingStartPos.clear();
			}
			
			readsSharingStartPos.add(read);
			prevStartPos = startPos;
		}
		return readsWritten;
	}

	/**
	 * Given a list of reads that all share the same starting alignment position, figure out which should be written
	 * to the output bam writer. This is where there real algo is.
	 * @param readsSharingStartPos
	 * @param writer
	 */
	private static int processSameStartReads(
			List<MappedRead> reads, SAMFileWriter writer) {
	
		//Sanity check, make sure they all have the same start position
		int pos = reads.get(0).getRecord().getAlignmentStart();
		for(MappedRead read : reads) {
			if (read.getRecord().getAlignmentStart() != pos) {
				System.out.println("Whoa!");
			}
		}
		
		//Not sure! How about we just remove half, randomly? 
		int readsWritten = 0;
		for(MappedRead read : reads) {
			if (Math.random() < 1.5) {
				writer.addAlignment(read.getRecord());
				readsWritten++;
			}
		}
		return readsWritten;
	}


}
