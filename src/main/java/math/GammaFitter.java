package math;

import java.util.List;

import math.hmm.Utils;

import org.apache.commons.math3.distribution.GammaDistribution;

/**
 * Given an input list of observed values, this creates a gamma function that fits 
 * the values. For simplicity, right now this just tries to match the mean and variance
 * of the input values - it doesn't really do any fancy curve fitting. 
 * @author brendan
 *
 */
public class GammaFitter {

	/**
	 * Create a new GammaDistribution fitted to the given vals list. Vals are assumed to be
	 * individual observatins drawn from the distribution - they're not densities or frequencies
	 * @param vals
	 * @return
	 * @throws GammaFitException 
	 */
	public GammaDistribution fitData(List<Double> vals) throws GammaFitException {
		if (vals.size()<5) {
			throw new GammaFitException("Too few values for fitting (need at least 5, found " + vals.size() + ")");
		}
		double mean = Utils.mean(vals);
		if (mean < 1E-6) {
			throw new GammaFitException("Mean is really tiny for this interval (" + mean + "), we probably can't fit a gamma to it.");
		}
		return Utils.newGammaWithMeanAndStdev( mean , Utils.stdev(vals));
	}
	
	public class GammaFitException extends Exception {
		
		public GammaFitException(String msg) {
			super(msg);
		}
	}
	
}
