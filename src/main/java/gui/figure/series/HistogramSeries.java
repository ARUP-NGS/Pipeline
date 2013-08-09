/********************************************************************
*
* 	Copyright 2011 Brendan O'Fallon
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
***********************************************************************/


package gui.figure.series;

import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.List;

import math.Histogram;

/**
 * A type of XY series that wraps a Histogram. All point data re stored in the histogram itself,
 * there's no List<Point> or array of doubles here
 * @author brendan
 *
 */
public class HistogramSeries extends XYSeries {

	Histogram histo;
	
	public HistogramSeries(String name, int bins, double min, double max) {
		super(name);
		histo = new Histogram(min, max, bins);
		pointList = null;
	}

	public HistogramSeries(String name, int bins, double min, double max, List<Point2D> values) {
		super(name);
		histo = new Histogram(min, max, bins);
		for(Point2D p : values) {
			histo.addValue(p.getY());
		}
		pointList = null;
	}
	
	public HistogramSeries(String name, List<Point2D> points, int bins, double min, double max) {
		super(name);
		replace(points, bins, min, max);
		pointList = null;
	}
	
	public HistogramSeries(String name, Histogram histo) {
		super(name);
		this.histo = histo;
		pointList = null;
	}
	
	public HistogramSeries(int bins, double min, double max) {
		this("Density", bins, min, max);
	}
	
//	public void addValue(double x) {
//		histo.addValue(x);
//	}
	
//	public void removeValue(double x) {
//		histo.removeValue(x);
//	}

	
	public void replace(double[] points, int bins, double min, double max) {
		histo = new Histogram(min, max, bins);
		for(int i=0; i<points.length; i++) {
			histo.addValue(points[i]);
		}
	}
	
	public void replace(List<Point2D> points, int bins, double min, double max) {
		histo = new Histogram(min, max, bins);
		for(int i=0; i<points.size(); i++) {
			histo.addValue(points.get(i).getY());
		}
	}
	
	public void clear() {
		if (histo != null)
			histo.clear();
	}

	
	public void addPointInOrder(Point2D newPoint) {
		//addValue(newPoint.getY());
		histo.addValue(newPoint.getY());
		
		if (Double.isNaN(maxY) || newPoint.getY() > maxY)
			maxY = newPoint.getY();
	}
	
	/**
	 * Add a new value to this histogram series
	 * @param val
	 */
	public void addValue(double val) {
		histo.addValue(val);
		if (val > maxY) {
			maxY = val;
		}
	}
	
	/**
	 * The number of bins in the histogram
	 * @return
	 */
	public int getBinCount() {
		return histo.getBinCount();
	}
		

	/**
	 * Return the x-value of the point with the given index
	 * @param index
	 * @return
	 */
	public double getX(int index) {
		return histo.getMin() + index*histo.getBinWidth();
	}
	
	/**
	 * return y-yalue of point at given index
	 * @param index
	 * @return
	 */
	public double getY(int index) {
		return (double)histo.getCount(index)/(double)histo.getCount();
	}
	
	
	public Point2D[] getLineForXVal(double xVal) {
		int lower = getIndexForXVal(xVal);
		
		if (lower<0 || lower>=(histo.getBinCount()-1))
			return null;
		
		int upper = lower+1;
		Point2D[] line = new Point2D[2];
		line[0] = new Point2D.Double(getX(lower), getY(lower));
		line[1] = new Point2D.Double(getX(upper), getY(upper));
		return line;
	}

	/**
	 * Returns the index with the highest x found such that points.get(index).x < xVal. 
	 */
	public int getIndexForXVal(double xVal) {
		return histo.getBin(xVal);
	}
	
	/**
	 * Returns the Point with the highest index found such that points.get(index).x < xVal. 
	 * Since the x values are sorted, we can use a bisection search. Additionally, since we
	 * usually expect x-values to be linearly increasing, we can make an educated guess about
	 * what the right index is at the start
	 */
	public Point2D getClosePointForXVal(double xVal) {
		int index = getIndexForXVal(xVal);
		
		if (index<0 || index>=histo.getBinCount())
			return null;
		else
			return new Point2D.Double(getX(index), histo.getCount(index));
	}
	
	
	/**
	 * Return the minimum x value in the list. Since the list is sorted, this is always the x-val of the first 
	 * point in the list. 
	 * @return
	 */
	public double getMinX() {
		return Math.max(histo.getMin(), histo.getMinValueAdded());
	}
	
	/**
	 * Return the maximum x-value in the list. Since the list is sorted by x-value, this is always the x-val
	 * of the last point in the list. 
	 * 
	 * @return
	 */
	public double getMaxX() {
		return Math.min(histo.getMax(), histo.getMaxValueAdded());
	}
	
	public double getMinY() {
		return 0;
	}
	
	
	public double getMaxY() {
		return (double)histo.getMaxCount()/(double)histo.getCount();
	}
	
	/**
	 * Return the greatest x-val such that the y-val of all subsequent elements is zero. Useful for
	 * some data sets which tend to generate long lists of 0-valued points (such as allele frequency spectra)
	 * @return
	 */
	public int lastNonZero() {
		return histo.getBinCount();
	}
	
	/**
	 * The number bins between getMinX and getMaxX
	 */
	public int size() {
		return histo.getBinCount();
	}
	
	public Double getBoxWidth() {
		return histo.getBinWidth();
	}

	
	/**
	 * Returns the Point at the given index in this list of points, or null if  i> this.size()
	 * @param i
	 * @return
	 */
	public Point2D get(int i) {
		if (i>=histo.getBinCount())
			return null;
		else {
			return new Point2D.Double(getX(i), getY(i));
		}
	}
	
	private Comparator<Point2D> getXComparator() {
		return new XComparator();
	}

	class XComparator implements Comparator<Point2D> {

		public int compare(Point2D a, Point2D b) {
			return a.getX() > b.getX() ? 1 : -1;
		}
		
	}

	
}
