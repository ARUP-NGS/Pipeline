package math;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.OptimizationData;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;

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
//	public GammaDistribution fitData(List<Double> vals) throws GammaFitException {
//		if (vals.size()<5) {
//			throw new GammaFitException("Too few values for fitting (need at least 5, found " + vals.size() + ")");
//		}
//		double mean = Utils.mean(vals);
//		if (mean < 1E-6) {
//			throw new GammaFitException("Mean is really tiny for this interval (" + mean + "), we probably can't fit a gamma to it.");
//		}
//		return Utils.newGammaWithMeanAndStdev( mean , Utils.stdev(vals));
//	}
	
	
	public GammaDistribution fitData(List<Double> data) throws GammaFitException {
		
		//Use a simple Nelder-Mead optimizer to find the shape and scale parameters that best 
		//fit the input data 
		try {
			NelderMeadSimplex nms = new NelderMeadSimplex(2);		
			GammaFitFunc f = new GammaFitFunc(data);

			SimplexOptimizer so = new SimplexOptimizer(1e-4, 1e-4);
			PointValuePair result = so.optimize(new OptimizationData[]{
					new MaxIter(1000), 
					new MaxEval(1000),
					new ObjectiveFunction(f),
					GoalType.MAXIMIZE, 
					new InitialGuess(new double[]{1.0, 1.0}), 
					nms 
			});


			double[] p = result.getPoint();
			return new GammaDistribution(p[0], p[1]);
		}
		catch (Exception ex) {
			throw new GammaFitException("Could not fit a distribution to the data: " + ex.getMessage());
		}
		
	}
	
	public class GammaFitException extends Exception {
		
		public GammaFitException(String msg) {
			super(msg);
		}
	}
	
	/**
	 * This is the function that we'd like to maximize when we call the fitData(values..) method. It
	 * gets initialized with the normalized depth values, and each call to value(..) returns the log-likelihood
	 * that all of the depth values came from a distribution the the shape and scale parameters given 
	 * to the value function 
	 * @author brendan
	 *
	 */
	static class GammaFitFunc implements MultivariateFunction, OptimizationData {
		
		List<Double> values;
		
		public GammaFitFunc(List<Double> data) {
			this.values = data;
		}
		
		/**
		 * Returns the (log-)likelihood that the data were sampled from a gamma distribution
		 * with the given shape and scale params (in the input array)  
		 */
		@Override
		public double value(double[] params) {
			double shape = params[0];
			double scale = params[1];
			
			if (shape < 0.00001) {
				return Double.NEGATIVE_INFINITY;
			}
			if (scale < 0.000001) {
				return Double.NEGATIVE_INFINITY;
			}
			GammaDistribution dist = new GammaDistribution(shape, scale);
			double logProb = 0;
			for(Double val : values) {
				double lp = dist.logDensity(val); 
				logProb += lp;
			}
			
			return logProb;
		}
	}
	
	
	
	public static void main(String[] args) throws GammaFitException {
		
		NelderMeadSimplex nms = new NelderMeadSimplex(2);

		
		List<Double> data = new ArrayList<Double>();
		GammaDistribution real = new GammaDistribution(1.0, 1.0);
		for(int i=0; i<50; i++) {
			data.add( real.sample());
		}
		
		GammaFitFunc f = new GammaFitFunc(data);
		
		SimplexOptimizer so = new SimplexOptimizer(1e-4, 1e-4);
		PointValuePair result = so.optimize(new OptimizationData[]{
				new MaxIter(1000), 
				new MaxEval(1000),
				new ObjectiveFunction(f),
				GoalType.MAXIMIZE, 
				new InitialGuess(new double[]{1.0, 1.0}), 
				nms 
		});
		
		
		double[] p = result.getPoint();
		System.out.println("Final shape: " + p[0]);
		System.out.println("Final scale: " + p[1]);
		
	}
	
}
