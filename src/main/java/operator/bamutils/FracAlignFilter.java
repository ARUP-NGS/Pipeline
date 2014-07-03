package operator.bamutils;

import java.util.ArrayList;
import java.util.List;

import operator.OperationFailedException;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.SAMRecord;

/**
 * Removes reads with less than a fraction of the read being mapped.
 * @author daniel
 *
 */
public class FracAlignFilter extends AbstractBAMFilter {
	
	public static final String FRACTION = "fraction";
	
	public FracAlignFilter() throws OperationFailedException{
		List<ReadFilter> readFilters = new ArrayList<ReadFilter>();
		readFilters.add(new CigFilter());
		setFilters(readFilters);
	}
	
	/**
	 * Fail read if less than a given fraction is mapped. 
	 * @author daniel
	 *
	 */
	class CigFilter implements ReadFilter {
			
		@Override
		public boolean readPasses(SAMRecord read) {
			float defaultFrac = (float) 0.7;
			float fraction = defaultFrac;
			if(getAttribute(FRACTION) != null){
				fraction = Float.parseFloat(getAttribute(FRACTION));
			}
			//If the fraction is not between 0 and 1, set it to the default value.
			if(fraction<0 || fraction >1){
				System.out.println("Fraction provided is not in the scope of [0,1]. \n" + Float.toString(fraction));
				fraction = defaultFrac;
			}
			int sum=0;
			int sumMap=0;
			Cigar cig = read.getCigar();
			List<CigarElement> cigContents=cig.getCigarElements();
			int numCig = cigContents.size();
			for (int i=0;i<numCig;i++) {
				sum+=Integer.parseInt(cigContents.get(i).toString().replaceAll("\\D+",""));
				if(cigContents.get(i).toString().contains("M")) {
					sumMap += Integer.parseInt(cigContents.get(i).toString().replaceAll("\\D+",""));
				}
					
			}
			if((float) sumMap/sum < fraction) {
				return false;
			}
			else
				return true;
		}
		
	}

}
