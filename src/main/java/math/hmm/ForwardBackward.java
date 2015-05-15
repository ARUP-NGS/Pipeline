package math.hmm;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

public class ForwardBackward {

	final HiddenMarkovModel hmm; //Transition and emission probs
	final Observation[] obs; //Observation and position info
	
	private DoubleMatrix2D forwards; //Forward probabilities
	private DoubleMatrix2D backwards; //backward probabilities
	
	public ForwardBackward(HiddenMarkovModel hmm, Observation[] obs) {
		this.hmm = hmm;
		this.obs = obs;
	}
	
	public void computeAndStoreProbs(Observation[] obs) {
		computeForwardProbs();
		computeBackwardProbs();
	}
	
	
	/**
	 * The greatest position across all observations
	 * @return
	 */
	public int getLastPos() {
		return obs[obs.length-1].position;
	}
	/**
	 * Compute and store forward probability vectors
	 */
	private void computeForwardProbs() {
		
	}
	
	public RealVector computeForwardStep(RealVector prevProbs, int dist, double obsVal) {
		
		RealMatrix tp = hmm.transitionProbs.power(dist);
		RealVector newState = tp.preMultiply(prevProbs);
		
		//Result now contains the probability of being in state i given the previous state vector
		//and a distance, but what we want is the probability of being in state i AND observing obsVal
		RealVector result = new ArrayRealVector(hmm.getStateCount());
		for(int i=0; i<newState.getDimension(); i++) {
			result.setEntry(i, computeMarginalObsProb(newState, obsVal));
		}
		
		return result;
	}
	
	/**
	 * Compute the probability density of observing i conditional on state vector p
	 * 
	 * @param prevProbs
	 * @return
	 */
	private double computeMarginalObsProb(RealVector p, double x) {
		double sum = 0;
		for(int i=0; i<p.getDimension(); i++) {
			sum += p.getEntry(i)*hmm.emissionProbs[i].density(x);
		}
		return sum;
	}
	
	private void computeBackwardProbs() {
		// TODO Auto-generated method stub
		
	}

	
	
	public DoubleMatrix1D getStateProbsAtPos(int p) {
		return null;
		
	}
}
