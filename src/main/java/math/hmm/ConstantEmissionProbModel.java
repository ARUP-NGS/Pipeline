package math.hmm;

import org.apache.commons.math3.distribution.AbstractRealDistribution;

/**
 * An emission probability model that always returns the same set of emission probability 
 * functions. This is used for traditional time/space homogeneous HMMs.
 * @author brendan
 *
 */
public class ConstantEmissionProbModel implements EmissionProbModel {
	
	final AbstractRealDistribution[] probs;
	
	public ConstantEmissionProbModel( AbstractRealDistribution[] probs) {
		this.probs = probs;
	}

	@Override
	public AbstractRealDistribution[] getEmissionProbsForIndex(int ignored) {
		return probs;
	}

	@Override
	public int getDimension() {
		return probs.length;
	}
	
	

}
