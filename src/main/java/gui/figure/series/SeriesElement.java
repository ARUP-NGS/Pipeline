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

import gui.figure.Figure;
import gui.figure.FigureElement;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * A generic element that displays a series of data, either ordinal or non-ordinal. Subclasses implement
 * drawing schemes for XYSeries and CategorySeries data types. 
 * @author brendan
 *
 */
public abstract class SeriesElement extends FigureElement {
	
	//Marker types... maybe this should be an enum?
	public static final String LINES = "Lines";
	public static final String POINTS = "Points";
	public static final String POINTS_AND_LINES = "Points and lines";
	public static final String BOXES = "Boxes";
	
	protected AbstractSeries series; //Stores the data we're representing
	
	protected Color color;
	
	//This is the default mode 
	protected String currentMode = LINES;
	
	//List of marker shapes
	//If you change this list, you must also add corresponding code to drawMarker
	public static final String[] markerTypes = {"Circle", "Square", "Diamond", "Plus", "X"};
	
	//List of ways in which to draw the points, aka 'modes'
	public static final String[] styleTypes = {XYSeriesElement.LINES, XYSeriesElement.POINTS, XYSeriesElement.POINTS_AND_LINES, XYSeriesElement.BOXES };
	
	//The strokes used to paint the line and the highlighted line
	BasicStroke normalStroke;
	BasicStroke highlightStroke;
	protected float highlightWidthIncrease = 3.25f; //Width increase of highlight stroke over normal stroke
	
	//If we want to display multiple series with box shapes, then we divide their 
	//size by a certain factor, and offset them by a certain amount
	protected int boxWidthDivisor = 1;
	protected double boxOffset = 0;
	
	boolean includeInLegend = true;
	
	public SeriesElement(Figure parent, AbstractSeries series) {
		super(parent);
		this.series = series;
	}
	
	/**
	 * Returns true if this element should be displayed in the legend, the default is true.
	 * @return
	 */
	public boolean includeInLegend() {
		return includeInLegend;
	}
	
	/**
	 * Set a flag to indicate whether or not this element should be included in the legend
	 * @param include
	 */
	public void setIncludeInLegend(boolean include) {
		this.includeInLegend = include;
	}
	
	/**
	 * Draw the marker for this series using the specified graphics element at the specified position.
	 * @param g
	 * @param x
	 * @param y
	 */
	public abstract void drawMarker(Graphics2D g, int x, int y);
	
	public abstract double getMinY();
	
	public abstract double getMaxY();
	
	public abstract double getMinX();
	
	public abstract double getMaxX();
	
	
	public AbstractSeries getSeries() {
		return series;
	}
	
	/**
	 * Set both both the stroke used to paint this series to the new value (and the highlight stroke to
	 * something a bit bigger)
	 * @param width
	 */
	public void setLineWidth(float width) {
		normalStroke = new BasicStroke(width);
		highlightStroke = new BasicStroke(width+highlightWidthIncrease);		
	}
	
	public String getType() {
		return currentMode;
	}
	
	public Color getLineColor() {
		return color;
	}

	
	/**
	 * Return the name of the series
	 * @return
	 */
	public String getName() {
		return series.getName();
	}
	
	/**
	 * Set the name of the series to the new value
	 * @param name
	 */
	public void setName(String name) {
		series.setName(name);
	}
	
	/**
	 * Set the color of the lines and boxes drawn to represent this series. 
	 * @param c
	 */
	public void setLineColor(Color c) {
		color = c;
		highlightColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 150);
	}
	
	
	/**
	 * Changes the 'mode' (how the series is represnted, e.g. lines, points, boxes, etc)
	 * @param newMode
	 */
	public void setMode(String newMode) {
		if (modeIsValid(newMode)) {
			currentMode = newMode;
		}
		else {
			throw new IllegalArgumentException("Cannot set a series mode to : " + newMode);
		}
	}
	
	protected boolean modeIsValid(String mode) {
		if (mode.equals(XYSeriesElement.LINES) || 
			mode.equals(XYSeriesElement.POINTS) || 
			mode.equals(XYSeriesElement.POINTS_AND_LINES) || 
			mode.equals(XYSeriesElement.BOXES)) 
		{
			return true;	
		}
		else
			return false;
	}
	
	
	/**
	 * Sets the box width divisor value and box offset value for painting multiple
	 * box series' on the same chart (that don't overlap). These values represent
	 * not pixels but the "fractions of box width", a divisor of two shrinks the box by
	 * a factor of two. Similarly an offset of 1 shifts the box by one boxWidth value to
	 * the left.
	 * 
	 * @param boxWidthDivisor
	 * @param offset
	 */
	public void setBoxWidthAndOffset(int boxWidthDivisor, double offset) {
		this.boxWidthDivisor = boxWidthDivisor;
		this.boxOffset = offset;
	}

	
	

}
