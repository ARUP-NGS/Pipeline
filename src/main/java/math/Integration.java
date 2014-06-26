package math;

public class Integration {

	/**
	 * Simple trapezoidal quadrature of a function specified at the given x and y vals
	 * @param x
	 * @param y
	 * @return
	 */
	public static double trapezoidQuad(Double[] x, Double[] y, double maxX) {
		int size = x.length;
		
		double sum = 0;
		for(int i=0; i<size-1; i++) {
			
			double width = x[i+1] - x[i];
			double height = (y[i+1] + y[i])/2.0;
			double area = width * height;
			if (x[i+1] > maxX) {
				double prop = (maxX-x[i]) / (x[i+1]-x[i]);
				area *= prop;
				sum += area;
				break;
			}
			sum += area;
		}
		
		return sum;
	}
}
