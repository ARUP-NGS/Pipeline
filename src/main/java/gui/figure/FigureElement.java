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


package gui.figure;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Base class of anything that is part of a chart - axes, labels, etc. 
 * Boundaries are all between 0 and 1, so the elements can easily be 'stretched'
 * to any size. Subclasses include TextElements, AxesElements, BoxElements, etc.
 * 
 * 
 * @author brendan
 *
 */
public abstract class FigureElement {
	
	//The bounding rectangle for this elements, in 'figure' (0..1) scale
	protected Rectangle2D.Double bounds;
	
	//The number of pixels per unit of ChartElement size. Since chartElement coordinates are always
	//from 0..1, the xFactor should be the size of the canvas the object is being drawn on. 
	protected double xFactor = Double.NaN;  
	protected double yFactor = Double.NaN;
	
	protected Color foregroundColor = Color.black;
	protected Color highlightColor = new Color(10, 225, 225);
	
	//True is this element can be dragged around by the user
	protected boolean isMobile = false;


	//Indicates a z position for the elements. Higher numbers here get painted at the top
	//lowest are on the bottom
	protected Integer zPosition = 0;
	
	//If this is set to true, double clicking will cause
	//popupConfigureTool to be called
	protected boolean canConfigure = false;

	protected Figure parent;
	
	protected boolean isSelected = false;

	public FigureElement(Figure parent) {
		this.parent = parent;
		bounds = new Rectangle2D.Double();
	}
	
	/**
	 * Set the relative 'stacking order' of this element. Elements with higher Z-values are painted on top
	 * of elements with lower Z-values. The actual value means nothing beyond the ordering.
	 * @param z
	 */
	public void setZPosition(int z) {
		zPosition = z;
	}
	
	public Integer getZPosition() {
		return zPosition;
	}
	
	public Point2D getPosition() {
		return new Point2D.Double(bounds.x, bounds.y);
	}
	
	public double getX() {
		return bounds.x;
	}
	
	public double getY() {
		return bounds.y;
	}
	
	public Point2D getCenterPoint() {
		return new Point2D.Double(bounds.x + bounds.width/2.0, bounds.y+bounds.height/2.0);
	}
	
	public double getCenterX() {
		return bounds.x + bounds.width/2.0;
	}
	
	public double getCenterY() {
		return bounds.y + bounds.height/2.0;
	}
	
	/**
	 * Elements that return true will 'eat' mouse clicks, such that clicks will not
	 * be passed to any further elements in the list. Returning true here prevents
	 * multiple elements that are on top of each other from all opening their
	 * configuration tools here at once. 
	 * @return Whether or not this consumes mouse clicks
	 */
	public boolean consumesMouseClick() {
		return true;
	}
	
	public void rescalePosition(double x, double y) {
		bounds.x *= x;
		bounds.y *= y;
	}
	
	public void rescaleSize(double x, double y) {
		bounds.width *= x;
		bounds.height *= y;
	}
	
	public void shiftX(double dx) {
		bounds.x += dx;
	}
	
	public void shiftY(double dy) {
		bounds.y += dy;
	}
	
	public double getWidth() {
		return bounds.width;
	}
	
	public double getHeight() {
		return bounds.height;
	}
	
	public abstract void paint(Graphics2D g);
	
	public void setCanConfigure(boolean conf) {
		this.canConfigure = conf;
	}

	public boolean isMobile() {
		return isMobile;
	}

	public void setMobile(boolean isMobile) {
		this.isMobile = isMobile;
	}

	/**
	 * Mostly for debugging purposes, draws the border of this element.
	 * @param g
	 */
	protected void drawBorder(Graphics2D g) {
		Color c = g.getColor();
		g.setColor(Color.gray);
		g.drawRect(round(bounds.x*xFactor), round(bounds.y*yFactor), round(bounds.width*xFactor), round(bounds.height*yFactor));
		g.setColor(c);
	}
	
	/**
	 * Translate a point in 'FigureElement' coordinates, into a pixel value suitable for passing to a graphics object. 
	 * Passing in a zero here returns the pixel value for the left edge of the element, passing in a one returns a pixel
	 * value for the right edge of the element. 
	 * @param x
	 * @return
	 */
	public int toPixelX(double x) {
		return	round(xFactor*(bounds.x + x*bounds.width));
	}
	
	/**
	 * Translates the given point in pixel coordinates into a 0..1 value where 0 indicates the left edge of the element, 
	 * 
	 * @param pixX A x location in pixel units
	 * @return An x location in units relative to this elements x-boundaries
	 */
	public double toElementX(int pixX) {
		return (pixX/xFactor-bounds.x)/bounds.width;
	}

	/**
	 * Translates the given point in pixel coordinates into a 0..1 value where 0 indicates the top edge of the element, 
	 * and 1 indicates the bottom edge of the element. This doesn't do any range checking so can return values < 0 or >1
	 * @param pixY A y location in pixel units
	 * @return A y location in units relative to this elements x-boundaries
	 */
	public double toElementY(int pixY) {
		return (pixY/yFactor-bounds.y)/bounds.height;
	}
	
	/**
	 * Translate a point in 'FigureElement' coordinates, into a pixel value suitable for passing to a graphics object. 
	 * Passing in a zero here returns the pixel value for the top edge of the element, passing in a one returns a pixel
	 * value for the bottom edge of the element. 
	 * @param x
	 * @return
	 */
	public int toPixelY(double y) {
		return	round(yFactor*(bounds.y + y*bounds.height));
	}
	
	
	/**
	 * The following four methods are called only if this figure element has been 
	 * added to the list of mouseListeningElements in Figure (via Figure.addMouseListeningElement ). 
	 * Do what you will here. 
	 * @param pos The mouse position in bounds (0..1) coordinates
	 */
	protected void mouseMoved(Point2D pos) {	};
	
	protected void mousePressed(Point2D pos) {	};
	
	protected void mouseReleased(Point2D pos) {	};
	
	protected void mouseDragged(Point2D pos) {	};
	
	
	
	/**
	 * Called (by Figure) when this element is single clicked
	 */
	public void clicked(Point pos) { 
		setSelected(true);
	}

	/**
	 * Called (by Figure) when this element is double clicked
	 */
	public void doubleClicked(Point pos) { 
		setSelected(true);
		if (canConfigure) {
			popupConfigureTool(pos);
		}
	};
	
	
	/**
	 * A noop by default, subclasses can override this to provide configuration
	 * functionality. This is called if canConfigure is true and the user 
	 * double clicks on the this element.
	 */
	public void popupConfigureTool(java.awt.Point pos) {
		
	}
	
	/**
	 * Called (by Figure) when the mouse has been clicked on the figure,
	 * but this element was not clicked on.
	 */
	public void unClicked() { 
		setSelected(false);
	}
	
	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}
	
	/**
	 * Sets the bounds.x and bounds.y properties to the new values
	 * @param x
	 * @param y
	 */
	public void setPosition(double x, double y) {
		bounds.x = x;
		bounds.y = y;
	}
	
	public void move(double dx, double dy) {
		bounds.x += dx;
		bounds.y += dy;
		if( bounds.x < 0)
			bounds.x = 0;
		if (bounds.y < 0)
			bounds.y = 0;
	}
	
	public void setWidth(double w) {
		bounds.width = w;
	}
	
	public void setHeight(double h) {
		bounds.height = h;
	}
	
	public void setBounds(double x, double y, double w, double h) {
		bounds.x = x;
		bounds.y = y;
		bounds.width = w;
		bounds.height = h;
	}
	
	/**
	 * Establishes the scale of the element (by telling it how big the containing figure is), this
	 * is guaranteed to get called before paint
	 * 
	 * @param xFactor Width of the figure in pixels
	 * @param yFactor Height of the figure in pixels
	 */
	public void setScale(double xFactor, double yFactor, Graphics g) {
		this.xFactor = xFactor;
		this.yFactor = yFactor;
	}

	public boolean contains(double x, double y) {
		boolean cont = bounds.contains(x, y) ;
		return cont;
	}
	
	public final boolean contains(Point2D p) {
		return contains(p.getX(), p.getY());
	}
	
	public Rectangle2D getBounds() {
		return bounds;
	}
	
	
	public static int round(double x) {
		return (int)Math.floor(x+0.5);
	}
	
	public double getXFactor() {
		return xFactor;
	}
	
	public double getYFactor() {
		return yFactor;
	}
	
}
