package math;

/**
 * A distribution with support in [0, 1]
 * @author brendan
 *
 */
public class UnitDistribution implements ContinuousDistribution {

	private double[] densities;
	
	/**
	 * Construct a new UnitDistro. with the given array of probabilities as those that
	 * will be returned by getPdf.  The array can be of any length, but the bins are assumed
	 * to be of equal size and to always span 0..1 evenly
	 * @param densities
	 */
	public UnitDistribution(double[] densities) {
		this.densities = densities;
	}
	
	private void normalizeDensities() {
		double sum = 0;
		for(int i=0; i<densities.length; i++) {
			sum += densities[i];
		}
		
		for(int i=0; i<densities.length; i++) {
			densities[i] = densities[i]/sum;
		}
	}
	
	@Override
	public double getPDF(double x) {
		if (x<0 || x>1.0) 	
			throw new IllegalArgumentException("value not in 0..1, got:" + x);

		int bin = (int)Math.floor(x*densities.length);
		if (bin == densities.length)
			bin--;
		return densities[bin];
	}

}
