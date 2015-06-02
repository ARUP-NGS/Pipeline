package math.hmm;

import org.apache.commons.math3.distribution.AbstractRealDistribution;

public interface EmissionProbModel {
	
	/**
	 * Returns the list of emission probability functions associated with the given position
	 * @param which
	 * @return
	 */
	public AbstractRealDistribution[] getEmissionProbsForIndex(int position);
	
	/**
	 * Returns the number of states in the model, which is equal to the length of the array 
	 * returned by getEmissionProbsForIndex
	 * @return
	 */
	public int getDimension();

}
