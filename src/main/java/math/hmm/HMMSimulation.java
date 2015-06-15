package math.hmm;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class HMMSimulation {
	
	final HiddenMarkovModel hmm;
	
	public HMMSimulation(HiddenMarkovModel hmm) {
		this.hmm = hmm;
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
	public StateObservation simulateSingleUpdate(RealVector state, int distance, int pos) {
		double before = this.hmm.transitionProbs.getEntry(0, 0);
		RealMatrix powered = this.hmm.transitionProbs.power(distance);
		double after = this.hmm.transitionProbs.getEntry(0, 0);
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
		
		result = Utils.renormalize(result);
		sum = 0.0;
		for(int i=0; i<result.getDimension(); i++) {
			sum += result.getEntry(i);
		}
		
		
		int newState = randomIndex(result);
		RealVector newStateVec = new ArrayRealVector( this.hmm.getStateCount() );
		newStateVec.set(0);
		newStateVec.setEntry(newState, 1.0);
		
		AbstractRealDistribution emissionProb = this.hmm.emissionProbs.getEmissionProbsForIndex(pos)[newState];
		double observedValue = emissionProb.sample();
		StateObservation so = new StateObservation();
		so.observedVal = observedValue;
		
		
		so.state = newState;
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
		int state;
		int pos;
	}
	
	/**
	 * Return the index associated with the greatest value
	 * @param v
	 * @return
	 */
	public static int mostProbIndex(RealVector v) {
		double maxVal = v.getEntry(0);
		int maxIndex = 0;
		for(int i=1; i<v.getDimension(); i++) {
			if (v.getEntry(i)>maxVal) {
				maxVal = v.getEntry(i);
				maxIndex = i;
			}
		}
		return maxIndex;
		
	}
	
	
	
	public static void main(String[] args) {
		
		double[][] tMat = {
				{0.90, 0.1, 0.0}, 
				{0.005, 0.99, 0.005},
				{0.0, 0.1, 0.90},
			};
		
		
		SparseEmissionProbModel epModel = new SparseEmissionProbModel(3);
		AbstractRealDistribution[] ep1 = new AbstractRealDistribution[]{
			new NormalDistribution(1.0, 1.5),
			new NormalDistribution(2.0, 1.5),
			new NormalDistribution(2.5, 1.0)
		};
		AbstractRealDistribution[] ep2 = new AbstractRealDistribution[]{
				new NormalDistribution(2.0, 0.5),
				new NormalDistribution(3.0, 0.5),
				new NormalDistribution(4.0, 1.0)
		};
		
		
		List<Integer> positions = new ArrayList<Integer>();
		int posi = 0;
		for(int i=0; i<250; i++) {
			int dist = (int) Math.round(20.0*Math.random());
			
			if (posi < 200) {
				epModel.addFuncsAtPos(posi, ep1);
			} else {
				epModel.addFuncsAtPos(posi, ep2);
			}
			positions.add(posi);
			posi += dist;
		}
		
		
		HiddenMarkovModel hmm = new HiddenMarkovModel(new BlockRealMatrix(tMat), epModel);
		HMMSimulation simulator = new HMMSimulation(hmm);
		
		RealVector state = hmm.stationaries;
		int prevPos = 0;
		List<StateObservation> actual = new ArrayList<StateObservation>();
		List<Observation> obs = new ArrayList<Observation>();
		for(Integer pos : positions) {
			StateObservation so = simulator.simulateSingleUpdate(state, pos-prevPos, pos);
			so.pos = pos;
			actual.add(so);
			Observation o = new Observation(so.observedVal, "X", so.pos);
			obs.add(o);
			
			state = new ArrayRealVector(hmm.getStateCount());
			state.setEntry(so.state, 1.0);
			prevPos = pos;
		}
		
		ForwardBackward fwb = new ForwardBackward(hmm, obs);
		fwb.computeAndStoreProbs();
		
		for(int i=0; i< actual.size(); i++) {
			StateObservation so = actual.get(i);
			RealVector calc = fwb.getStateProbsAtPos(i);
			int guessIndex = mostProbIndex(calc);
		
			String verdict = "";
			if (guessIndex == so.state) {
				verdict = "Sweet";
			} else {
				verdict = "Dang!";
			}
			System.out.println("Pos: " + so.pos + "\t obs:" + so.observedVal + "\t" + so.state + "\t" + calc + "\t" + verdict);
			
		}
		
	}
	
}
