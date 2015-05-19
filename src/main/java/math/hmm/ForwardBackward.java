package math.hmm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class ForwardBackward {

	final HiddenMarkovModel hmm; //Transition and emission probs
	final List<Observation> obs; //Observation and position info
	
	private List<RealVector> smoothed; //Final results, combines forward and backward probs
	
	
	
	public ForwardBackward(HiddenMarkovModel hmm, List<Observation> obs) {
		this.hmm = hmm;
		this.obs = obs;
	}
	
	public void computeAndStoreProbs() {
		List<Double> sums = new ArrayList<Double>();
		
		List<RealVector> forwards = computeForwardProbs(sums);
		List<RealVector> backwards = computeBackwardProbs();
		
		smoothed = new ArrayList<RealVector>();
		
		
		
		//Compute 'smoothed' values
		//System.out.println("Smoothed values:");
		for(int i=0; i<forwards.size(); i++) {
			RealVector result = new ArrayRealVector(hmm.getStateCount());
			
			RealVector f = forwards.get(i);
			RealVector b = backwards.get(i);

			for(int j=0; j<hmm.getStateCount(); j++) {
				result.setEntry(j, f.getEntry(j)*b.getEntry(j) );
			}
			
			result = Utils.renormalize(result);
			//System.out.println(obs.get(i).position + "\t" + obs.get(i).value + "\t" + f + "\t" + b + "\t" + result);


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
		
		int prevPos = 0;
		RealVector state = hmm.stationaries;
		for(Observation o : this.obs) {
			int dist = o.position - prevPos;
			state = computeForwardStep(state, dist, o, forwardSums);
			forwards.add(state);
			prevPos = o.position;
		}
		
		return forwards;
	}
	
	public RealVector computeForwardStep(RealVector prevProbs, int dist, Observation o, List<Double> sums) {
		RealMatrix tp = hmm.transitionProbs.power(dist);
		RealVector newState = tp.preMultiply(prevProbs);
		
		//Result now contains the probability of being in state i given the previous state vector
		//and a distance, but what we want is the probability of being in state i AND observing obsVal
		RealVector result = new ArrayRealVector(hmm.getStateCount());
		double sum = 0.0;
		for(int i=0; i<newState.getDimension(); i++) {
			double newVal = newState.getEntry(i)*hmm.emissionProbs.getEmissionProbsForIndex(o.position)[i].density( o.value );
			result.setEntry(i, newVal);
			sum += newVal;
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
			int dist = prevPos - o.position ;
			state = computeBackwardStep(state, dist, o);
			backwards.add(state);
			prevPos = o.position;
		}
		
		//Backwards steps were done in reverse order, so we reverse the resulting list so the order matches
		//forward steps
		Collections.reverse(backwards);
		
		return backwards;
	}

	
	private RealVector computeBackwardStep(RealVector state, int dist, Observation o) {
		
		RealMatrix tp = hmm.transitionProbs
				.transpose()
				.power(dist);
		
		RealVector newState =tp.preMultiply(state);
		
		//Result now contains the probability of being in state i given the previous state vector
		//and a distance, but what we want is the probability of being in state i AND observing obsVal
		RealVector result = new ArrayRealVector(hmm.getStateCount());
		for(int i=0; i<newState.getDimension(); i++) {
			double newVal = newState.getEntry(i)*hmm.emissionProbs.getEmissionProbsForIndex(o.position)[i].density( o.value );
			result.setEntry(i, newVal);
		}
		
		
		return result;
	}

	public RealVector getStateProbsAtPos(int obsIndex) {
		return smoothed.get(obsIndex);
	}
}
