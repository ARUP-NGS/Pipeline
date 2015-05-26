package math;

import java.util.List;

import math.hmm.Utils;

import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

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
	 */
	public GammaDistribution fitData(List<Double> vals) {
		return Utils.newGammaWithMeanAndStdev( mean(vals) , stdev(vals));
	}
	
	public static double mean(List<Double> vals){
		double sum = 0;
		for(Double v : vals) {
			sum += v;
		}
		return sum / (double) vals.size();
	}
	
	public static double stdev(List<Double> vals){
		StandardDeviation std = new StandardDeviation();
		return std.evaluate(toDoubleArray(vals));
	}
	
	public static double[] toDoubleArray(List<Double> vals) {
		//Apparently there's really not a better way to do this
		double[] arr = new double[vals.size()];
		for(int i=0; i<vals.size(); i++) {
			arr[i] = vals.get(i).doubleValue();
		}
		return arr;
	}
	
}
