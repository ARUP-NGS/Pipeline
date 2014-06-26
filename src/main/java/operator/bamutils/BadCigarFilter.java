package operator.bamutils;

import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;

/**
 * Removes reads with bad Cigars, which are sometimes produced by bwa-mem and cause the GATK to crash.
 * @author brendan
 *
 */
public class BadCigarFilter extends AbstractBAMFilter {

	public BadCigarFilter() {
		List<ReadFilter> readFilters = new ArrayList<ReadFilter>();
		readFilters.add(new CigFilter());
		setFilters(readFilters);
	}
	
	/**
	 * Fail read if cigar is empty or if it starts or ends with a deletion. 
	 * @author brendan
	 *
	 */
	class CigFilter implements ReadFilter {

		@Override
		public boolean readPasses(SAMRecord read) {
			Cigar cig = read.getCigar();
			if (cig.isEmpty() || cig.numCigarElements()==0)
				return false;
			
			CigarElement firstEl = cig.getCigarElement(0);
			if (firstEl.getOperator() == CigarOperator.D
					|| firstEl.getOperator() == CigarOperator.DELETION) {
				return false;
			}
			
			if (cig.numCigarElements()>1) {
				CigarElement lastEl = cig.getCigarElement( cig.numCigarElements()-1 );
				if (lastEl.getOperator() == CigarOperator.D
						|| lastEl.getOperator() == CigarOperator.DELETION) {
					return false;
				}
			}
			return true;
		}
		
	}


}
