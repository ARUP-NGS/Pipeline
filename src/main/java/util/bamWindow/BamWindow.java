package util.bamWindow;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

/**
 * Represents a collection of SAMRecords that cover a particular genomic location and which can be
 * moved (advanced) in one direction. Useful if you're interested in looking at any kind of local 
 * alignment & doing something with it. Originally derived from SNPSVM code.  
 * 
 * @author brendan
 *
 */
public class BamWindow {

	public static final boolean DEBUG = false; //Emit some debugging messages (yes, we should have better logging...)
	
	final File bamFile;
	final SAMFileReader samReader; 
	private SAMRecordIterator recordIt; //Iterator for traversing over SAMRecords
	private SAMRecord nextRecord; //The next record to be added to the window, may be null if there are no more
	
	private String currentContig = null;
	private int currentPos = -1; //In reference coordinates
	
	final LinkedList<MappedRead> records = new LinkedList<MappedRead>(); //List of records mapping to currentPos
	final LinkedList<MappedTemplate> templates = new LinkedList<MappedTemplate>(); //List of 'templates' overlapping current pos
	
	private Map<String, Integer> contigMap = null;
	private SAMSequenceDictionary sequenceDict = null;
	private int minMQ = 0;

	public BamWindow(File bamFile, int minMQ) {
		this(bamFile);
		this.minMQ = minMQ;
	}
	
	public BamWindow(File bamFile) {
		this.bamFile = bamFile;
		
		SAMFileReader.setDefaultValidationStringency(ValidationStringency.SILENT);
		samReader = new SAMFileReader(bamFile);
		samReader.setValidationStringency(ValidationStringency.SILENT);
		SAMFileHeader header = samReader.getFileHeader();
		sequenceDict = header.getSequenceDictionary();
		contigMap = new HashMap<String, Integer>();
		for(SAMSequenceRecord seqRec : sequenceDict.getSequences()) {
			contigMap.put(seqRec.getSequenceName(), seqRec.getSequenceLength());
		}
		
		
		recordIt = samReader.iterator();
		nextRecord = recordIt.next();
	}
	
	public int getCurrentPosition() {
		return currentPos;
	}
	
	public String getCurrentContig() {
		return currentContig;
	}
	/**
	 * Return total number of reads at the current position
	 * @return
	 */
	public int size() {
		//number of records on current position
		return records.size();
	}
	
	/**
	 * Return an approximate number of templates (the things that get sequenced to produce reads) 
	 * that map to this location. It's not always possible to do this accurately since sometimes reads
	 * are mapped incorrectly, but we can probably do it OK most of the time when both reads in a pair
	 * are mapped unambiguously.  
	 * 
	 * @return Approx number of templates overlapping the current position
	 */
	public int templateCount() {
		return templates.size();
		
	}
	
	/**
	 * Emit some debugging info to stdout
	 */
	public void emitTemplates() {
		System.out.println("Position : " + currentPos + " ... "  + templates.size() + " overlapping templates:");
		for(MappedTemplate tmpl : templates) {
			System.out.println("\t" + tmpl.toString() + ", aln start: " + tmpl.firstRead.getRecord().getAlignmentStart() + " - " + tmpl.firstRead.getRecord().getAlignmentEnd());
		}
	}
	
	
	/**
	 * Return the mean inferred insertion size of all records in this window
	 * @return
	 */
	public double meanInsertSize() {
		double sum = 0;
		double excludedReads = 0;
		for(MappedRead rec : records) {
			if(excludeReadInsertSize(rec)){
				excludedReads++;
				continue;
			}
			sum += Math.abs(rec.getRecord().getInferredInsertSize());
		}
		return sum / ((double)records.size() - excludedReads);
	}

	private boolean excludeReadInsertSize(MappedRead rec){
		// Don't include read's insert size when calculating mean insert
		// size if a mate is unmapped or mapped to a different contig.
		// In these cases, a value of 0 is returned, but it is not representative
		// of the actual insert size.
		if(rec.read.getReadUnmappedFlag() ||
		   rec.read.getReadPairedFlag() && rec.read.getMateUnmappedFlag() ||
		   rec.read.getReferenceIndex() != rec.read.getMateReferenceIndex()){
			return true;
		}
		return false;
	}
	
	/**
	 * Obtain an interator for the SAMRecords at the current position
	 * @return
	 */
	public Iterator<MappedRead> getIterator() {
		return records.descendingIterator(); //iterator();
	}
	
	public boolean hasReadsInRegion(String chr, int start, int end) {
		//Would be pretty cool if we could do this quickly
		return true;
	}
	
	/**
	 * Advance the current position by the given number of bases
	 * @param bases
	 */
	public boolean advanceBy(int bases) {
		int newTarget = currentPos + bases;
		if (! hasMoreReadsInCurrentContig()) {
			if (DEBUG)
				System.out.println("No more reads in contig : " + currentContig);
			return false;
		}
		
		advanceTo(currentContig, newTarget);
		return true;
	}
	
	public int extent() {
		int size = size();
		if (size == 0) {
			return 0;
		}
		SAMRecord first = records.get(0).getRecord();
		SAMRecord last = records.get(size-1).getRecord();
		
		return first.getAlignmentEnd() - last.getAlignmentStart();
	}
	
	/**
	 * True if there is another read to be read in the current contigg
	 * @return
	 */
	public boolean hasMoreReadsInCurrentContig() {
		return nextRecord != null && nextRecord.getReferenceName().equals(currentContig);
	}
	
	/**
	 * A sanity check to ensure that all reads span the current position
	 */
	public void checkReads() {
		if (records.size()>0) {
			Iterator<MappedRead> it = getIterator();
			MappedRead read = it.next();
			while(read != null) {
				if (read.getRecord().getAlignmentStart() <= currentPos && read.getRecord().getAlignmentEnd() >= currentPos) {
					//cool
				}
				else {
					System.out.println("Read start: " + read.getRecord().getAlignmentStart() + " end:" + read.getRecord().getAlignmentEnd());
					throw new IllegalArgumentException("Uugh, read does not span current position of : " + currentPos);
				}
				try {
					read = it.next();
				}
				catch(NoSuchElementException ex) {
					read = null;
				}
			}	
		}
	}
	
	/**
	 * Advance to given contig if necessary, then advance to given position
	 * @param contig
	 * @param pos
	 */
	public void advanceTo(String contig, int pos) {
		//Advance to wholly new site
		//Expand leading edge until the next record is beyond target pos
		
		advanceToContig(contig);
		
		if (pos > contigMap.get(contig)) {
			throw new IllegalArgumentException("Contig " + contig + " has only " + contigMap.get(contig) + " bases, can't advance to " + pos);
		}
		
		
		if (nextRecord != null) {
			if (! nextRecord.getReferenceName().equals(currentContig)) {
				throw new IllegalArgumentException("Whoa! We're not searching the right contig, record contig is : " + contig +  " but current is : " + currentContig);
			}
		}
		
		//Must occur BEFORE we try to get new records...
		currentPos = pos;
		int count = 0;
		while(nextRecord != null 
				&& nextRecord.getAlignmentStart() <= pos
				&& nextRecord.getReferenceName().equals(currentContig)) {
			expand();
			if (count %128 == 0) {
				shrinkTrailingEdge();
				count++;
			}
		}
		
		shrinkTrailingEdge();
	
		//Nice sanity check...
		//checkReads();
	}
	
	/**
	 * True if this bam knows about the given contig. 
	 * @param contig
	 * @return
	 */
	public boolean containsContig(String contig) {
		return contigMap.containsKey(contig);
	}
	
	/**
	 * If given contig equals the currentContig, do nothing. Else, clear records in queue and set
	 * current position to zero, then search for given contig
	 * @param contig
	 */
	public void advanceToContig(String contig) {
		if (contig.equals(currentContig)) {
			return; //Already there
		}
		
		if (! contigMap.containsKey(contig)) {
			throw new IllegalArgumentException("Unrecognized contig name : "  + contig);
		}
		
		if (DEBUG)
			System.err.println("Advancing to contig : " + contig);
		
		recordIt.close();
		currentPos = 0;
		records.clear();
		
		int length = contigMap.get(contig);
		recordIt = samReader.queryOverlapping(contig, 1, length);
		
		//Going to a new contig, clear current queue
		nextRecord = recordIt.hasNext() 
				? recordIt.next()
				: null;
		
		if (nextRecord != null)
			currentContig = contig;
		else {
			if (DEBUG)
				System.err.println("Could not find any reads that mapped to contig : " + contig);
		}
	}
	
	public Set<String> getContigs() {
		return contigMap.keySet();
	}
	
	public Map<String, Integer> getContigMap() {
		return Collections.unmodifiableMap(contigMap);
	}
	
	public void close() {
		samReader.close();
	}
	
	public int getLeadingEdgePos() {
		return records.getFirst().getRecord().getAlignmentEnd();
	}
	
	public int getTrailingEdgePos() {
		return records.getLast().getRecord().getAlignmentStart();
	}
	
	public MappedRead getTrailingRecord() {
		if (records.size()==0)
			return null;
		return records.getLast();
	}
	
	private static MappedTemplate inferTemplateFromRead(SAMRecord read) {
		int tStart = read.getAlignmentStart();
		int tEnd = read.getAlignmentEnd();
		if (read.getProperPairFlag()
				&& (! read.getMateUnmappedFlag()) 
				&& (read.getReferenceIndex() == read.getMateReferenceIndex())) {
			tEnd = read.getMateAlignmentStart() + read.getReadLength();
		}
		
		int templateStart = Math.min(tStart, tEnd);
		int templateEnd = Math.max(tStart, tEnd);
		
		if (templateStart == read.getAlignmentStart()) {
			return new MappedTemplate(templateStart, templateEnd, new MappedRead(read));
		}
		
		return null;
	}

	/**
	 * Push new records onto the queue. Unmapped reads and reads with unmapped mates are skipped.
	 */
	private void expand() {
		if (nextRecord == null)
			return;

		records.add(new MappedRead(nextRecord));
		
		//Add the MappedTemplate
		MappedTemplate templ=  inferTemplateFromRead(nextRecord);
		
		
		if (templ != null) {
			templates.add(templ);
		}
		
		//Find next suitable record
		nextRecord = recordIt.hasNext() 
				? recordIt.next()
				: null;
		
		//Automagically skip reads with mapping quality less than minMQ
		while(nextRecord != null && (nextRecord.getMappingQuality() < minMQ)) {
			nextRecord = recordIt.hasNext()
					? recordIt.next()
					: null;
		}
	}

	
	/**
	 * Remove from queue those reads whose right edge is less than the current pos
	 */
	private void shrinkTrailingEdge() {
		if (!records.isEmpty()) {
			Iterator<MappedRead> it = records.iterator();
			MappedRead read = it.next();
			while(it.hasNext()) {
				if (read.getRecord().getAlignmentEnd() < currentPos) {
					it.remove();
				}

				read = it.next();
				if (read.getRecord().getAlignmentEnd() > (currentPos+20)) {
					break;
				}
			}
			if (read.getRecord().getAlignmentEnd() < currentPos) {
				it.remove();
			}
		}
		
		//Ditto for templates
		if (!templates.isEmpty()) {
			Iterator<MappedTemplate> mit = templates.iterator();

			MappedTemplate templ = mit.next();
			while(mit.hasNext()) {
				if (templ.end < currentPos) {
					mit.remove();
				}

				templ = mit.next();
			}
			if (templ.end < currentPos) {
				mit.remove();
			}
		}

	}

}
