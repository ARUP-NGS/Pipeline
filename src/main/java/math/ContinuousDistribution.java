package math;

public interface ContinuousDistribution {

	/**
	 * Return the probability density of this function at the given value
	 * @param x
	 * @return
	 */
	public double getPDF(double x);
	
}
