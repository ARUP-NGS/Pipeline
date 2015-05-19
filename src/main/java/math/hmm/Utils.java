package math.hmm;

import org.apache.commons.math3.linear.RealVector;

public class Utils {

	/**
	 * Divide each entry in the vector by the sum of all of the entries. The input vector is returned
	 * for convenience. 
	 * @param vec
	 * @return
	 */
	public static RealVector renormalize(RealVector vec) {
		double sum = 0;
		for(int i=0; i<vec.getDimension(); i++) {
			sum += vec.getEntry(i);
		}
		for(int i=0; i<vec.getDimension(); i++) {
			vec.setEntry(i, vec.getEntry(i)/sum);
		}
		return vec;
	}
	
}
