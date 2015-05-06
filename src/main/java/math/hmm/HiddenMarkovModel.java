package math.hmm;

import java.util.Collections;
import java.util.List;

import math.ContinuousDistribution;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;


public class HiddenMarkovModel {

	Algebra algebra = new Algebra();
	DoubleMatrix2D transitionProbs;
	ContinuousDistribution[] emissionProbs;
	List<Observation> observations;
	
	public HiddenMarkovModel(double[][] transitionProbs, ContinuousDistribution[] emissionProbs) {
		if (transitionProbs.length==0) {
			throw new IllegalArgumentException("Transition matrix cannot be empty");
		}
		if (transitionProbs.length != transitionProbs[0].length) {
			throw new IllegalArgumentException("Transition matrix must be square");
		}
		if (transitionProbs.length != emissionProbs.length) {
			throw new IllegalArgumentException("Number of states and emission probability functions must be equal");
		}
		this.transitionProbs = new DenseDoubleMatrix2D(transitionProbs);
		this.emissionProbs = emissionProbs;
	}
	
	public int getStateCount() {
		return transitionProbs.rows();
	}
	
	public void setObservations(List<Observation> obs) {
		this.observations = Collections.unmodifiableList(obs);
	}
	
	public Observation simulate(DoubleMatrix1D state, int distance) {
		DoubleMatrix2D powered = algebra.pow(transitionProbs, distance);
		DoubleMatrix1D result = powered.zMult(state, null);
		return null;
	}
}
