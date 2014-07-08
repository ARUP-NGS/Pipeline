package operator.bamutils;

import net.sf.samtools.SAMRecord;

/**
 * Things that can filter SAMRecords (reads), often implemented by BAMFilter operators, which
 * remove reads based on things like mapping quality, presence in a BED file, etc. 
 * @author brendan
 *
 */
public interface ReadFilter {

	/**
	 * Returns true if the given read passes the filter, false otw
	 * @param read
	 * @return
	 */
	public boolean readPasses(SAMRecord read);
	
}
