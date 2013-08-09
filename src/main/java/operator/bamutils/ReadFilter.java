package operator.bamutils;

import net.sf.samtools.SAMRecord;

/**
 * Anything which can perform read filtering
 * @author brendan
 *
 */
public interface ReadFilter {

	public boolean readPasses(SAMRecord read);
	
}
