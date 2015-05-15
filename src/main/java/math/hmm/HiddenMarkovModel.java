package math.hmm;

import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;


public class HiddenMarkovModel {

	RealMatrix transitionProbs;
	AbstractRealDistribution[] emissionProbs;
	List<Observation> observations;
	RealVector stationaries;
	
	public HiddenMarkovModel(RealMatrix transitionProbs, AbstractRealDistribution[] emissionProbs) {
		if (transitionProbs.getColumnDimension()==0) {
			throw new IllegalArgumentException("Transition matrix cannot be empty");
		}
		if (!transitionProbs.isSquare()) {
			throw new IllegalArgumentException("Transition matrix must be square");
		}
		if (transitionProbs.getColumnDimension() != emissionProbs.length) {
			throw new IllegalArgumentException("Number of states and emission probability functions must be equal");
		}
		this.transitionProbs = transitionProbs;
		this.emissionProbs = emissionProbs;
		
		//Compute stationaries
		EigenDecomposition ev = new EigenDecomposition(transitionProbs, 1.0);
		this.stationaries = ev.getEigenvector(0);
		
	}
	
	public int getStateCount() {
		return transitionProbs.getRowDimension();
	}
	
	public void setObservations(List<Observation> obs) {
		this.observations = Collections.unmodifiableList(obs);
	}
	
	private RealVector renormalize(RealVector vec) {
		double sum = 0;
		for(int i=0; i<vec.getDimension(); i++) {
			sum += vec.getEntry(i);
		}
		for(int i=0; i<vec.getDimension(); i++) {
			vec.setEntry(i, vec.getEntry(i)/sum);
		}
		return vec;
	}

	
	
	public RealMatrix getPoweredTransitionMat(int p) {
		return this.transitionProbs.power(p);
	}
	
	/**
	 * Get stationary state of transition matrix
	 * @return
	 */
	public RealVector getStationaries() {
		return this.stationaries;
	}
	
	/**
	 * Given an initial state and a distance describing the number of Markov steps
	 * compute a new observed value at the given distance. This is accomplished by
	 * powering the transition matrix by the distance given, then pseudo-randomly 
	 * selecting a state from the powered matrix, then pseudo-randomly selecting a value
	 * from the emission probability function associated with the given state.    
	 * @param state
	 * @param distance
	 * @return
	 */
	public StateObservation simulateSingleUpdate(RealVector state, int distance) {
		double before = transitionProbs.getEntry(0, 0);
		RealMatrix powered = transitionProbs.power(distance);
		double after = transitionProbs.getEntry(0, 0);
		if (before != after) {
			throw new IllegalArgumentException("Really?");
		}
		RealVector result = powered.preMultiply(state);
		
		//Make sure result sum is still one
		double sum = 0.0;
		for(int i=0; i<result.getDimension(); i++) {
			sum += result.getEntry(i);
		}
		if (Math.abs(sum-1.0)>0.01) {
			throw new IllegalStateException("Sum isn't one: " + sum + " dif: " + Math.abs(sum-1.0));
		}
		
		result = renormalize(result);
		sum = 0.0;
		for(int i=0; i<result.getDimension(); i++) {
			sum += result.getEntry(i);
		}
		
		
		int newState = randomIndex(result);
		
		AbstractRealDistribution emissionProb = emissionProbs[newState];
		double observedValue = emissionProb.sample();
		StateObservation so = new StateObservation();
		so.observedVal = observedValue;
		so.state = result;
		return so;
				
	}
	
	private int randomIndex(RealVector probs) {
		double r = Math.random();
		for(int i=0; i<probs.getDimension(); i++) {
			double val = probs.getEntry(i);
			if (r<=val) {
				return i;
			}
			r -= val;
		}
		return probs.getDimension()-1;
	}
	
	class StateObservation {
		double observedVal;
		RealVector state;
	}
	
	public static void main(String[] args) {
		
		double[][] tMat = {
				{0.5, 0.5}, 
				{0.1, 0.9}
			};
		
		AbstractRealDistribution[] emissionProbs = new AbstractRealDistribution[]{
			new NormalDistribution(1.0, 0.2),
			new NormalDistribution(10.0, 0.2)
		};
		
		HiddenMarkovModel hmm = new HiddenMarkovModel(new BlockRealMatrix(tMat), emissionProbs);
		
		double[] initState = new double[]{0.0, 1.0};
		RealVector state = new ArrayRealVector(initState);
		int pos = 0;
		int step = 50;
		for(int i=0; i<50; i++) {
			StateObservation so = hmm.simulateSingleUpdate(state, step);
			pos += step;
			System.out.println("Pos: " + pos + "\tval:" + so.observedVal + "\t\t" + state);
		}
		
	}
}
