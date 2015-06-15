package math.hmm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * Implements the Forward-Backward algorithm to compute the most likely state in an HMM given a series of Observations.
 * Probabilities for all states are computed with the .computeAndStoreProbs() method, after that they can be retrieved
 * via calls to getStateProbsAt(pos) or getStateProbsForObs(observation)
 * @author brendan
 *
 */
public class ForwardBackward {

	final Double TINY_POSITIVE = 1E-200; //Really small non-zero value
	final HiddenMarkovModel hmm; //Transition and emission probs
	final List<Observation> obs; //Observation and position info
	private double maxObsVal = Double.MAX_VALUE;
	
	//Cap observations values (normalized read counts) to the given number to avoid numerical issues
	private Double maxObservationVal = 10.0;
	private List<RealVector> smoothed; //Final results, combines forward and backward probs
	
	
	public ForwardBackward(HiddenMarkovModel hmm, List<Observation> obs) {
		this.hmm = hmm;
		this.obs = obs;
	}
	
	/**
	 * Actually perform the algorithm and store all of the state probabilities. 
	 */
	public void computeAndStoreProbs() {
		List<Double> sums = new ArrayList<Double>();
		
		List<RealVector> forwards = computeForwardProbs(sums);
		List<RealVector> backwards = computeBackwardProbs();
		
		smoothed = new ArrayList<RealVector>();
		
		
		
		//Compute 'smoothed' values
		for(int i=0; i<forwards.size(); i++) {
			RealVector result = new ArrayRealVector(hmm.getStateCount());
			
			RealVector f = forwards.get(i);
			RealVector b = backwards.get(i);

			for(int j=0; j<hmm.getStateCount(); j++) {
				result.setEntry(j, f.getEntry(j)*b.getEntry(j) );
			}
			
			result = Utils.renormalize(result);

			smoothed.add(result);
		}
	}
	
	
	/**
	 * The greatest position across all observations
	 * @return
	 */
	public int getLastPos() {
		return obs.get(obs.size()-1).position;
	}
	/**
	 * Compute and store forward probability vectors
	 */
	private List<RealVector> computeForwardProbs(List<Double> forwardSums) {
		List<RealVector> forwards = new ArrayList<RealVector>();
		
		int prevPos = this.obs.get(0).position-1; //We 'start' at one base prior to th first observation
		RealVector state = hmm.stationaries;
		for(Observation o : this.obs) {
			int dist = o.position - prevPos;
			
			try {
				state = computeForwardStep(state, dist, o, forwardSums);
				assertValidVector(state);
			} catch (IllegalStateException ex) {
				System.err.println("Invalid vector state: " + ex);
			}
			
			forwards.add(state);
			prevPos = o.position;
		}
		
		return forwards;
	}
	
	public RealVector computeForwardStep(RealVector prevProbs, int dist, Observation o, List<Double> sums) {
		assertValidVector(prevProbs);
		
		RealMatrix tp = hmm.transitionProbs.power(dist);
		RealVector newState = tp.preMultiply(prevProbs);
		
		//Result now contains the probability of being in state i given the previous state vector
		//and a distance, but what we want is the probability of being in state i AND observing obsVal
		RealVector result = new ArrayRealVector(hmm.getStateCount());
		double sum = 0.0;
		
		
		int failures = 0;
		AbstractRealDistribution[] emissionDists = hmm.emissionProbs.getEmissionProbsForIndex(o.position);

		for(int i=0; i<newState.getDimension(); i++) {
			
			double oval = o.value;
			if (oval==0.0) {
				oval = TINY_POSITIVE;
			}
			oval = Math.min(maxObservationVal, oval);
			double density = emissionDists[i].density(oval);
			if (Double.isNaN(density) || density<TINY_POSITIVE) {
				density = TINY_POSITIVE;
				failures++;
			}
			oval = Math.min(oval, getMaxObsVal());
			
			double newVal = newState.getEntry(i)*density;
			
			if (Double.isNaN(newVal) && newState.getEntry(i) < TINY_POSITIVE) {
				newVal = TINY_POSITIVE;
			}
			result.setEntry(i, newVal);
			sum += newVal;
		}
	
		
		//If this is true, then we couldn't compute ANY state correctly. 
		if (failures==newState.getDimension()) {
			System.err.println("Dang, couldn't compute ANY forward state prob correctly for, defaulting to previous dist " + o);
			result=newState;
			sum = 0;
			for(int i=0; i<newState.getDimension(); i++) {
				sum += newState.getEntry(i);
			}
		}
		
		sums.add(sum);
		
		
		//Normalize vector
		for(int i=0; i<newState.getDimension(); i++) {
			result.setEntry(i, result.getEntry(i)/sum);
		}
		
		return result;
	}
	
	
	
	private List<RealVector> computeBackwardProbs() {
		List<RealVector> backwards = new ArrayList<RealVector>();
		
		int prevPos = this.obs.get( obs.size()-1).position;
		RealVector state = hmm.stationaries;
		for(int i=this.obs.size()-1; i>=0; i--) {
			Observation o = this.obs.get(i);
			int dist = prevPos - o.position;
			try {
				state = computeBackwardStep(state, dist, o);
				assertValidVector(state);
			} catch (IllegalStateException ex) {
				System.err.println("Invalid vector state: " + ex);
			}
			
			backwards.add(state);
			prevPos = o.position;
		}
		
		//Backwards steps were done in reverse order, so we reverse the resulting list so the order matches
		//forward steps
		Collections.reverse(backwards);
		
		return backwards;
	}

	public double getMaxObsVal() {
		return maxObsVal;
	}

	public void setMaxObsVal(double maxObsVal) {
		this.maxObsVal = maxObsVal;
	}
	
	private RealVector computeBackwardStep(RealVector state, int dist, Observation o) {
		assertValidVector(state);
		
		RealMatrix tp = hmm.transitionProbs
				.transpose()
				.power(dist);
		
		RealVector newState =tp.preMultiply(state);
		
		//Result now contains the probability of being in state i given the previous state vector
		//and a distance, but what we want is the probability of being in state i AND observing obsVal
		int failures = 0;
		RealVector result = new ArrayRealVector(hmm.getStateCount());
		AbstractRealDistribution[] emissionDists = hmm.emissionProbs.getEmissionProbsForIndex(o.position);
		
		for(int i=0; i<newState.getDimension(); i++) {
			double oval = o.value;
			if (oval==0.0) {
				oval = TINY_POSITIVE;
			}
			oval = Math.min(oval, getMaxObsVal());
			
			oval = Math.min(maxObservationVal, oval);
			double density = emissionDists[i].density(oval);
			if (Double.isNaN(density) || density<TINY_POSITIVE) {
				density = TINY_POSITIVE;
				failures++;
			}
			
			double newVal = newState.getEntry(i)*density;
			result.setEntry(i, newVal);
		}
		
		if (failures==newState.getDimension()) {
			result = newState;
		}
		
		result = Utils.renormalize(result);
		
		return result;
	}

	private void assertValidVector(RealVector v) {
		double sum = 0;
		for(int i=0; i<v.getDimension(); i++) {
			if (Double.isInfinite(v.getEntry(i)) || Double.isNaN(v.getEntry(i))) {
				throw new IllegalStateException("Invalid vector values: " + v.toString());
			}
			sum += v.getEntry(i);
		}
		
		if (sum==0.0) {
			throw new IllegalStateException("Invalid vector values: " + v.toString());
		}
	}
	
	public RealVector getStateProbsAtPos(int obsIndex) {
		return smoothed.get(obsIndex);
	}
	
	public RealVector getStateProbsForObs(Observation o) {
		return getStateProbsAtPos( obs.indexOf(o) );
	}

	public Double getMaxObservationVal() {
		return maxObservationVal;
	}

	public void setMaxObservationVal(Double maxObservationVal) {
		this.maxObservationVal = maxObservationVal;
	}
}
