package gui.figure.series;

import java.awt.geom.Point2D;
import java.util.Arrays;

/**
 * A type of series where the number of elements always remains constant. Convenient for histograms,
 * @author brendano
 *
 */
public class ConstSizeSeries extends AbstractSeries {

	private double[] yvals;
	private double[] xvals;
	private double minY = Double.NaN;
	private double maxY = Double.NaN;
	
	public ConstSizeSeries(String name, double[] yvals, double[] xvals) {
		setName(name);
		this.yvals = yvals;
		this.xvals = xvals;
		findMinMaxY();
	}
	
	public void setYVals(double[] yvals) {
		if (yvals.length != xvals.length) {
			throw new IllegalArgumentException("YValues array must be same size has X-values array");
		}
		this.yvals = yvals;
		findMinMaxY();
	}
	
	private void findMinMaxY() {
		minY = Double.POSITIVE_INFINITY;
		maxY = Double.NEGATIVE_INFINITY;
		
		for(int i=0; i<yvals.length; i++) {
			if (yvals[i] < minY)
				minY = yvals[i];
			if (yvals[i] > maxY)
				maxY = yvals[i];
		}
	}
	
	@Override
	public int size() {
		return xvals.length;
	}

	@Override
	public double getX(int index) {
		return xvals[index];
	}

	@Override
	public double getY(int index) {
		return yvals[index];
	}

	@Override
	public double getMinX() {
		return xvals[0];
	}

	@Override
	public double getMinY() {
		return minY;
	}

	@Override
	public double getMaxX() {
		return xvals[xvals.length-1];
	}

	@Override
	public double getMaxY() {
		return maxY;
	}

	@Override
	public int getIndexForXVal(double xVal) {
		int index = Arrays.binarySearch(xvals, xVal);
		if (index < 0)
			index = -1*index - 1;
		return index;
	}

	@Override
	public Point2D[] getLineForXVal(double xVal) {
		int lower = getIndexForXVal(xVal)-1;
		
		if (lower<0 || lower>=(xvals.length-1))
			return null;
		
		int upper = lower+1;
		Point2D[] line = new Point2D[2];
		line[0] = new Point2D.Double(xvals[lower], yvals[lower]);
		line[1] = new Point2D.Double(xvals[upper], yvals[upper]); 
		return line;
	}

}
