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

/**
 * Implements the idea of a named list of points, where the points have a x- and y-labels of some sort. Various 
 * subclasses implement series with double-valued points for ordinal series or string=valued x-values and double-valued
 * y values for categorical data. 
 * @author brendan
 *
 */
public abstract class AbstractSeries {

	protected String name;
	
	public AbstractSeries() {
		name = "Untitled series";
	}
	
	/**
	 * Obtain the number of entries in this series
	 * @return
	 */
	public abstract int size();
	
	/**
	 * Obtain the x-value of the point at the given index
	 * @param index
	 * @return
	 */
	public abstract double getX(int index);
	
	/**
	 * Obtain the y-valueo f the point at the given index
	 * @param index
	 * @return
	 */
	public abstract double getY(int index);
	
	
	/**
	 * Returns the smallest x-value in the series
	 * @return
	 */
	public abstract double getMinX();
	
	/**
	 * Obtain the smallest y-value added to the series
	 * @return
	 */
	public abstract double getMinY();
	
	/**
	 * Obtain the largest x-value in this series
	 * @return
	 */
	public abstract double getMaxX();
	
	/**
	 * Obtain the largest y-value in the series
	 * @return
	 */
	public abstract double getMaxY();
	
	
	/**
	 * Series may optionally provide information about the width of boxes used to represent data columns when
	 * we're displaying this series in an XYSeriesElement. If we return null here, the Element attempts to
	 * make a best guess. If a value is returned, it is assumed to be in data coordinates. 
	 * @return
	 */
	public Double getBoxWidth() {
		return null;
	}
	
	/**
	 * Returns the index with the highest x found such that this.getX(index) < xVal. 
	 */
	public abstract int getIndexForXVal(double xVal);
	
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
	public abstract Point2D[] getLineForXVal(double xVal);
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	

	
	
}
