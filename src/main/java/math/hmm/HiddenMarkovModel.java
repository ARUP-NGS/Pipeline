package math.hmm;

import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * TODO: Emission probs need to be observation-dependent
 * TODO: Need to be able to read and write models containing 
 * Could use existing python model-builder in mean time if we dont want to write a model building class now
 * @author brendan
 *
 */
public class HiddenMarkovModel {

	RealMatrix transitionProbs;
	EmissionProbModel emissionProbs;
	List<Observation> observations;
	RealVector stationaries;
	
	public HiddenMarkovModel(RealMatrix transitionProbs, EmissionProbModel emissionProbs) {
		if (transitionProbs.getColumnDimension()==0) {
			throw new IllegalArgumentException("Transition matrix cannot be empty");
		}
		if (!transitionProbs.isSquare()) {
			throw new IllegalArgumentException("Transition matrix must be square");
		}
		if (transitionProbs.getColumnDimension() != emissionProbs.getDimension()) {
			throw new IllegalArgumentException("Number of states and emission probability functions must be equal");
		}
		this.transitionProbs = transitionProbs;
		this.emissionProbs = emissionProbs;
		
		computeStationaries();
	}
	
	private void computeStationaries() {
		//Compute stationaries state of transition matrix
		//, there's no telling where the primary (largest-magnitude) eigenvalue will end up
		//so we have to look for it
		EigenDecomposition ev = new EigenDecomposition(transitionProbs.transpose());
		int index = -1;
		for(int i=0; i<transitionProbs.getRowDimension(); i++) {
			if (Math.abs( ev.getRealEigenvalue(i)-1.0)<0.000001) {
				index = i;
			}
			
			if (ev.getRealEigenvalue(i)>1.000001) {
				throw new IllegalArgumentException("Found an eigenvalue greater than 1.0! (value=" + ev.getRealEigenvalue(i) + " index: " + i);
			}
		}
		if (index == -1) {
			throw new IllegalArgumentException("No eigenvalue has magnitude 0");
		}
		this.stationaries = Utils.renormalize(ev.getEigenvector(index));
		System.err.println("First eigenvalue: " + ev.getRealEigenvalue(index));
		System.err.println("Transition stationaries: " + this.stationaries.toString());
				
	}
	
	public int getStateCount() {
		return transitionProbs.getRowDimension();
	}
	
	public void setObservations(List<Observation> obs) {
		this.observations = Collections.unmodifiableList(obs);
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
	
	
}
