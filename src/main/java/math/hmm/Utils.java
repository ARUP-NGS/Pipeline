package math.hmm;

import java.util.List;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

public class Utils {

	/**
	 * Divide each entry in the vector by the sum of all of the entries. The input vector is returned
	 * for convenience. 
	 * @param vec
	 * @return
	 */
	public static RealVector renormalize(RealVector vec) {
		double sum = 0;
		for(int i=0; i<vec.getDimension(); i++) {
			sum += vec.getEntry(i);
		}
		for(int i=0; i<vec.getDimension(); i++) {
			vec.setEntry(i, vec.getEntry(i)/sum);
		}
		return vec;
	}
	
	/**
	 * Create a return a new gamma distribution with the given mean and stdev 
	 */
	public static GammaDistribution newGammaWithMeanAndStdev(double mean, double stdev) {
		double variance = stdev*stdev;
		double shape = mean*mean/variance;
		double scale = mean/shape;
		return new GammaDistribution(shape, scale);
	}
	
	public static void writeDist(AbstractRealDistribution dist, double min, double max, int bins) {
		double step = (max-min)/(bins-1.0);
		double xval = min;
		for(int i=0; i<bins; i++) {
			System.out.println(xval + "\t:\t" + dist.probability(xval));
			xval += step;
		}
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
