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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * A list of x,y values with a few drawing options that are relicts of a more... civilized era
 * This should be rehashed at some point so it doesn't rely in element.Point and can be constructed in a more
 * general manner. 
 * 
 *  These are immutable outside of increasing x-order appending operations, which allows for some additional optimizations. 
 * 
 * @author brendan
 *
 */
public class XYSeries extends AbstractSeries {

	protected String name; //Arbitrary label for series
	

	protected List<Point2D> pointList;
	protected double ySum = 0; //Stores sum of y-values, useful for calculating mean
	protected double prevYSum; //Sum of y-values not including last-added item 
	protected double m2n; //Running (online) sum of squares of differences from current mean, useful for computing stdev quickly without rescanning list

	//To prevent repeated scans of the list we store the max and min y-values here
	protected double maxY = Double.NaN;
	protected double minY = Double.NaN;
	
	public XYSeries(List<Point2D> points, String name) {
		this.name = name;
		this.pointList = points;
		sortByX();
	}

	/**
	 * Create a new XY series with the given name and no data
	 * @param name
	 */
	public XYSeries(String name) {
		this.name = name;
		pointList = new ArrayList<Point2D>(1024);
	}
	
	public XYSeries(List<Point2D> points) {
		this(points, "Untitled series");
	}
	
	/**
	 * Remove all values from point list
	 */
	public void clear() {
		pointList.clear();
		ySum = 0;
	}
	
	/**
	 * Get the label assigned to this series, most for UI purposes. 
	 */
	public String getName() {
		return name;
	}

	
	/**
	 * Set an arbitrary label for this series
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Returns mean of of y-values
	 * @return
	 */
	public double getYMean() {
		return ySum / pointList.size();
	}
	
	
	/**
	 * Returns standard deviation of y-values
	 * @return
	 */
	public double getYStdev() {
		if (pointList.size()<2)
			return 0.0;
		return Math.sqrt( m2n / (pointList.size()-1.0) );
	}
	
	/**
	 * Returns the y-value of the point at the end of the list
	 * @return
	 */
	public double lastYValue() {
		if (pointList.size()==0)
			return Double.NaN;
		else
			return pointList.get( pointList.size()-1).getY();
	}
	
	/**
	 * Append a new point to the end of this series. The x-value of the point must be 
	 * greater than the x-value of the previous point
	 * @param newPoint
	 */
	public void addPointInOrder(Point2D newPoint) {
		if (pointList.size()>0 && newPoint.getX() < getMaxX())
			throw new IllegalArgumentException("Non-increasing x value");
		prevYSum = ySum;
		if (Double.isNaN(maxY) || newPoint.getY() > maxY)
			maxY = newPoint.getY();

		if (Double.isNaN(minY) || newPoint.getY() < minY)
			minY = newPoint.getY();

		//System.out.println("Adding point " + newPoint.getY() + " to series");
		double y = newPoint.getY();
		ySum += y;
		if (pointList.size()>1)
			m2n += (y-ySum/(pointList.size()+1.0))*(y-prevYSum/(pointList.size()));
		pointList.add(newPoint);
	}
	
	public List<Point2D> getPointList() {
		return pointList;
	}
	
	
	/**
	 * Sorts the values in X order
	 */
	private void sortByX() {
		Collections.sort(pointList, getXComparator());
	}

	/**
	 * Return the x-value of the point with the given index
	 * @param index
	 * @return
	 */
	public double getX(int index) {
		return pointList.get(index).getX();
	}
	
	/**
	 * return y-yalue of point at given index
	 * @param index
	 * @return
	 */
	public double getY(int index) {
		return pointList.get(index).getY();
	}
	
	/**
	 * Obtain two Point2D objects that satisfy the following characteristics:
	 * bound the given x
	 * Point[0].x < xVal
	 * Point[1].x > xVal
	 * 
	 * There is some i such that Point[0].x = getX(i) and Point[0].y = getY(i)
	 * (that is, the point's x and y values must be a point in this series)
	 * same for Point[1]
	 * 
	 * @param xVal
	 * @return
	 */
	public Point2D[] getLineForXVal(double xVal) {
		int lower = getIndexForXVal(xVal);
		

		if (lower<0 || lower>=(pointList.size()-1))
			return null;
		int upper = lower+1;
		
		Point2D[] line = new Point2D[2];
		line[0] = pointList.get(lower);
		line[1] = pointList.get(upper);
		//System.out.println("click data x: " + xVal + " lower x: " + pointList.get(lower).getX() + " upper x: " + pointList.get(upper).getX());
		if (pointList.get(lower).getX() > xVal || pointList.get(upper).getX() < xVal) {
			System.err.println("Yikes, didn't pick th right index, got " + lower + " lower point is : " + pointList.get(lower));
		}
		return line;
	}

	/**
	 * Returns the index with the highest x found such that points.get(index).x < xVal. 
	 * Since the x values are sorted, we can use a bisection search. Additionally, since we
	 * usually expect x-values to be linearly increasing, we can make an educated guess about
	 * what the right index is at the start
	 */
	public int getIndexForXVal(double xVal) {
		int upper = pointList.size()-1;
		int lower = 0;
		
		if (pointList.size()==0)
			return 0;
		
		//TODO Are we sure an binarySearch(pointList, xVal) wouldn't be a better choice here?
		//it can gracefully handle cases where the key isn't in the list of values...
		double stepWidth = (pointList.get(pointList.size()-1).getX()-pointList.get(0).getX())/(double)pointList.size();
		
		int index = (int)Math.floor( (xVal-pointList.get(0).getX())/stepWidth );
		//System.out.println("Start index: " + index);
		
		//Check to see if we got it right
		if (index>=0 && (index<pointList.size()-1) && pointList.get(index).getX() < xVal && pointList.get(index+1).getX()>xVal) {
			//System.out.println("Got it right on the first check, returning index : " + index);
			return index;
		}
				
		//Make sure the starting index is sane (between upper and lower)
		if (index<0 || index>=pointList.size() )
			index = (upper+lower)/2; 
		
		if (xVal < pointList.get(0).getX()) {
			return 0;
		}
			
		if(xVal > pointList.get(pointList.size()-1).getX()) {
			return pointList.size()-1;
		}
		
		int count = 0;
		while( upper-lower > 1) {
			if (xVal < pointList.get(index).getX()) {
				upper = index;
			}
			else
				lower = index;
			index = (upper+lower)/2;
			count++;
		}
		
		return index;
	}
	
	/**
	 * Returns the Point with the highest index found such that points.get(index).x < xVal. 
	 * Since the x values are sorted, we can use a bisection search. Additionally, since we
	 * usually expect x-values to be linearly increasing, we can make an educated guess about
	 * what the right index is at the start
	 */
	public Point2D getClosePointForXVal(double xVal) {
		int index = getIndexForXVal(xVal);
		
		if (index<0 || index>=pointList.size())
			return null;
		else
			return pointList.get(index);
	}
	
	
	/**
	 * Return the minimum x value in the list. Since the list is sorted, this is always the x-val of the first 
	 * point in the list. 
	 * @return
	 */
	public double getMinX() {
		if (pointList.size()==0) {
			return 0;
		}
		return pointList.get(0).getX();
	}
	
	public double getMinY() {
		if (Double.isNaN(minY))
			minY = findMinY();
		
		return minY;
		

	}
	
	/**
	 * Find minimum y-value in list
	 * @return
	 */
	private double findMinY() {
		if (pointList.size()==0) {
			return 0;
		}
		double min = pointList.get(0).getY();
		for(int i=0; i<pointList.size(); i++)
			if (min>pointList.get(i).getY())
				min = pointList.get(i).getY();
		return min;
	}
	
	/**
	 * Return the maximum x-value in the list. Since the list is sorted by x-value, this is always the x-val
	 * of the last point in the list. 
	 * 
	 * @return
	 */
	public double getMaxX() {
		if (pointList.size()==0) {
			return 0;
		}
		return pointList.get(pointList.size()-1 ).getX();
	}
	
	public double getMaxY() {
		if (Double.isNaN(maxY)) 
			maxY = findMaxY();
		
		return maxY;
	}
	
	/**
	 * Find maximym y-value in list
	 * @return
	 */
	private double findMaxY() {
		if (pointList.size()==0) {
			return Double.NaN;
		}
		double max = pointList.get(0).getY();
		for(int i=0; i<pointList.size(); i++)
			if (max<pointList.get(i).getY())
				max = pointList.get(i).getY();
		return max;
	}
	
	/**
	 * Remove the point at the given index
	 * @param i
	 */
	public Point2D removePoint(int i) {
		Point2D p = pointList.remove(i);
		if (p != null) {
			ySum -= p.getY();
			minY = Double.NaN;
			maxY = Double.NaN;
		}
		return p;
	}
	
	/**
	 * Return the greatest x-val such that the y-val of all subsequent elements is zero. Useful for
	 * some data sets which tend to generate long lists of 0-valued points (such as allele frequency spectra)
	 * @return
	 */
	public int lastNonZero() {
		int i;
		for(i=pointList.size()-1; i>=0; i--) {
			if (pointList.get(i).getY() > 0)
				return i;
		}
		return 0;
	}
	
	/**
	 * The number of points in the list
	 */
	public int size() {
		return pointList.size();
	}

	/**
	 * Series may optionally provide information about the width of boxes used to represent data columns when
	 * we're displaying this series in an XYSeriesElement. If we return null here, the Element attempts to
	 * make a best guess. If a value is returned, it is assumed to be in data coordinates. 
	 * @return
	 */
	public Double getBoxWidth() {
		return null;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		String lineSep = System.getProperty("line.separator");
		for(int i=0; i<pointList.size(); i++) {
			str.append("" + pointList.get(i).getX() + ", " + pointList.get(i).getY() + lineSep);
		}
		return str.toString();
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
