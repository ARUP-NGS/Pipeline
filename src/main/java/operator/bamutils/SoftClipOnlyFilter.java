package operator.bamutils;

import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.Cigar;
import net.sf.samtools.SAMRecord;

public class SoftClipOnlyFilter extends AbstractBAMFilter{

	public SoftClipOnlyFilter() {
		List<ReadFilter> readFilters = new ArrayList<ReadFilter>();
		readFilters.add(new CigFilter());
		setFilters(readFilters);
	}
	
	/**
	 * Fail read if cigar ISN'T soft clipped. Used as part of breakpoint discovery pipeline	 
	 * Basic idea is to remove all reads without soft clipping, do a read depth analysis, and look for jumps 
	 * @author dave
	 *
	 */
	class CigFilter implements ReadFilter {

		@Override
		public boolean readPasses(SAMRecord read) {
			Cigar cig = read.getCigar();
			String temp = cig.toString();
			System.out.println(temp);
			if (temp.matches("S")){
				System.out.println("Found one!");
				
				return true;
			}
			return false;
		}
		
	}
}
