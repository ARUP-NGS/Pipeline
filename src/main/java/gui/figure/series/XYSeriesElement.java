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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


/**
 * An element that draws a series of points in x-y space. Different markers can be placed
 * on data points, or lines can be drawn through points, or the series can be represented by
 * 'boxes' column graph style. 
 * 
 * @author brendan
 *
 */
public class XYSeriesElement extends SeriesElement {
	
	protected AbstractSeries xySeries;
	private Point2D[] path = null;
	protected GeneralPath markerShape;
	
	AxesElement axes;
	Shape shapeToDraw = null; //For debugging only
	SeriesConfigFrame configFrame;
	
	//Flag to indicate if we should recalculate data bounds
	//False indicates we should recalculate
	boolean dataBoundsSet = false;
	
	boolean scaleHasBeenSet = false;
	

	//Tracks current marker type and size
	String currentMarkerType = markerTypes[2];
	int markerSize = 6;
	
	Color boxOutlineColor = Color.GRAY;
	
	Rectangle2D lineRect; //Use to test if this series contains certain points
	
	//Ensure boxes are linked 
	boolean connectBoxes = true;
	
	//The transformation object that transforms 'figure' points (in 0..1 scale) into pixel space
	//We keep track of it to avoid having to make a new one all the time, and 
	//so that, when new transforms are needed, we can call the .invert() method
	//on it to unapply the previous transform before a new one is applied
	AffineTransform currentTransform;
	
	//Draws fancier looking boxes
	protected boolean decorateBoxes = true;
	
	Rectangle2D boxRect = new Rectangle2D.Double(0, 0, 0, 0); //Used repeatedly to draw boxes
	
	//Some allocated space for marker polygon drawing
	int[] xvals;
	int[] yvals;
	
	public XYSeriesElement(AbstractSeries series, AxesElement axes, XYSeriesFigure parent) {
		super(parent, series);
		this.xySeries = series;
		this.axes = axes;
		this.xFactor = 1;
		this.yFactor = 1;
		dataBoundsSet = false; 
		
		currentTransform = new AffineTransform();
		currentTransform.setToIdentity();
		
		//configFrame = new SeriesConfigFrame(this, parent);
		
		normalStroke = new BasicStroke(1.25f);
		highlightStroke = new BasicStroke(1.25f + highlightWidthIncrease);
		
		lineRect = new Rectangle.Double(0, 0, 0, 0);
		
		//Some buffers for drawing marker polygons
		xvals = new int[5];
		yvals = new int[5];
	}
	
	
	/**
	 * Set the line stroke property to be the given stroke. The highlight stroke is automagically set to be something a bit wider
	 * @param newStroke
	 */
	public void setStroke(BasicStroke newStroke) {
		normalStroke = newStroke;
		highlightStroke = new BasicStroke(newStroke.getLineWidth()+highlightWidthIncrease, newStroke.getEndCap(), newStroke.getLineJoin(), newStroke.getMiterLimit(), newStroke.getDashArray(), newStroke.getDashPhase());
	}
	
	public AbstractSeries getSeries() {
		return xySeries;
	}
	
	public void setSeries(XYSeries newSeries) {
		super.series = newSeries;
		this.xySeries = newSeries;
		dataBoundsSet = false;
	}
	

	@Override
	public double getMaxY() {
		return xySeries.getMaxY();
	}

	@Override
	public double getMinY() {
		return xySeries.getMinY();
	}
	
	@Override
	public double getMaxX() {
		return xySeries.getMaxX();
	}

	@Override
	public double getMinX() {
		return xySeries.getMinX();
	}
	
	
	public int indexForMode() {
		for(int i=0; i<styleTypes.length; i++) {
			if (styleTypes[i].equals(currentMode)) {
				return i;
			}
		}
		
		throw new IllegalStateException("Illegal current mode in XYSeries Element : " + currentMode);		
	}
	
	
	/**
	 * Returns the index of the current marker type in markerTypes. 
	 * @param markerType
	 * @return The index of the current marker type in markerTypes
	 */
	public int indexForMarkerType() {
		for(int i=0; i<markerTypes.length; i++) {
			if (currentMarkerType.equals(markerTypes[i])) {
				return i;
			}
		}
		
		//We should never get here
		throw new IllegalStateException("Illegal current marker type in XYSeries Element : " + currentMarkerType);
	}
	
	
	/**
	 * Set the marker type for this series to the given type, which should be a member of markerTypes. This
	 * throws an IllegalArgumentException if the supplied type is not a valid type.
	 * @param markerType
	 */
	public void setMarker(String markerType) {
		boolean valid = false;
		for(int i=0; i<markerTypes.length; i++) {
			if (markerTypes[i].equals(markerType)) {
				currentMarkerType = markerType;
				valid = true;
			}
		}
		
		if (!valid) {
			throw new IllegalArgumentException("Cannot set marker type to : " + markerType);
		}
	}
	
	public void popupConfigureTool(java.awt.Point pos) {
		//System.out.println("Showing configure tool");
		if (configFrame == null)
			configFrame = new SeriesConfigFrame(this,(XYSeriesFigure) parent);
		
		if (xySeries instanceof HistogramSeries) {
			//TODO pop up a different tool where you can configure bin number?
			configFrame.display(getName(), currentMode, getLineColor(), round(((BasicStroke)normalStroke).getLineWidth()), markerSize, currentMarkerType);
		}
		else
			configFrame.display(getName(), currentMode, getLineColor(), round(((BasicStroke)normalStroke).getLineWidth()), markerSize, currentMarkerType);
	}
	
	/**
	 * Sets the various options (colors, linewidths, etc) of this series to those specified in ops
	 * @param ops Container object for various series options.
	 */
	public void setOptions(SeriesConfigFrame.SeriesOptions ops) {
		boolean resort = false;

		xySeries.setName( ops.name );
		setLineColor(ops.lineColor);
		
//		normalStroke = new BasicStroke((float)ops.lineWidth);
//		highlightStroke = new BasicStroke((float)ops.lineWidth+highlightWidthIncrease);
		
		normalStroke =  new BasicStroke((float)ops.lineWidth, normalStroke.getEndCap(), normalStroke.getLineJoin(), normalStroke.getMiterLimit(), normalStroke.getDashArray(), normalStroke.getDashPhase());;
		highlightStroke = new BasicStroke(normalStroke.getLineWidth()+highlightWidthIncrease, normalStroke.getEndCap(), normalStroke.getLineJoin(), normalStroke.getMiterLimit(), normalStroke.getDashArray(), normalStroke.getDashPhase());

		
		currentMarkerType = ops.markerType;
		markerSize = ops.markerSize;
		//Make sure lines are painted on top of boxes
		if (currentMode != ops.type && parent instanceof XYSeriesFigure) {
			resort = true;
			if (ops.type == BOXES) 
				setZPosition(-5);
			else 
				setZPosition(0);
		}
		currentMode = ops.type;
		
		if (resort) {
			((XYSeriesFigure)parent).getElementList().resort();
		}
		
		parent.repaint();
	}
	
	/**
	 * This call informs this element of what the data x and y bounds are.  It must be called
	 * prior to any painting operation. Also, calls to this function are relatively expensive since
	 * they involve creating a bunch of new objects, and ideally this should only be called when the
	 * x or y boundaries are changed (but not, for instance, when the figure size has changed).
	 * 
	 * @param dBounds
	 */
	public void setDataBounds() {
		regenerateShape();
		dataBoundsSet = true;
	}
	
	private void regenerateShape() {
		if (xySeries.size()==0) {
			return;
		}
		
		if (axes.getXFactor()==0 || axes.getYFactor()==0 || Double.isNaN(axes.getXFactor()) || Double.isNaN(axes.getYFactor())) {
			axes.setScale(this.getWidth(), this.getHeight(), null);
			//System.out.println("Rescaling axes size to : xFactor: " + axes.getXFactor() + " yFactor: " + axes.getYFactor());
		}

		if (path == null || path.length != xySeries.size()) {
			path = new Point2D[ xySeries.size() ];
			
		}

		
		if (currentMode == LINES || currentMode == POINTS_AND_LINES || currentMode == POINTS) {
			if (xySeries.size()>1) {
				double x1 = axes.dataXtoBoundsX(xySeries.getX(0)  );
				double y1 = axes.dataYtoBoundsY(xySeries.getY(0) );
				double x2 = axes.dataXtoBoundsX( xySeries.getX(1));
				double y2 = axes.dataYtoBoundsY( xySeries.getY(1) );
				
					
				boolean connect = true;
							
				for(int i=1; i<xySeries.size(); i++) {
					x1 = axes.dataXtoBoundsX( xySeries.getX(i) );
					y1 = axes.dataYtoBoundsY( xySeries.getY(i) );
					
					path[i] = new Point2D.Double(x1, y1);
					
					
				}
			}
		}
				
		
//		if (currentMode == BOXES) {
//			// Currently there is no pathShape that defines the boundaries for BOXES mode. We use getboxForIndex(...)
//			// to calculate a rectangle corresponding to the box for a particular index in the seiries, and that same
//			// rectangle is used in the contains(x, y) method. 
//		}
	
	}
	
	/**
	 * Calculate the standard width of the rectangle used to draw a box, if we're drawing boxes. The
	 * default here is to pack the boxes tightly, but that can be controlled if the boxWidthDivisor
	 * variable is set.
	 * @return
	 */
	private double calculateBoxWidth() {
		double boxWidth;
		if (xySeries.getBoxWidth() != null) {
			boxWidth = (axes.dataXtoFigureX( xySeries.getBoxWidth() ) - axes.dataXtoFigureX( 0 )) / (double)boxWidthDivisor ;
		}
		else {
			double boxesShowing = xySeries.size()*(axes.getXMax()-axes.getXMin())/(xySeries.getMaxX()-xySeries.getMinX());
			boxWidth = axes.getGraphAreaBounds().width / boxesShowing / (double)boxWidthDivisor;
		}
		return boxWidth;
	}
	
	public boolean contains(double x, double y) {		
		
		if (currentMode == POINTS_AND_LINES || currentMode == LINES) {
			double dataX = axes.boundsXtoDataX(x);
			//System.out.println("Clicking on point: " + x + ", " + y );
			lineRect.setRect(x*xFactor-4, y*yFactor-4, 8, 8);
			Point2D[] line = xySeries.getLineForXVal(dataX);
			//System.out.println("Data: " + line[0].getX() + ", " + line[0].getY() + " - " + line[1].getX() + ", " + line[1].getY());
			if (line==null || Double.isNaN(line[0].getY()) || Double.isNaN(line[1].getY())) {
				return false;
			}
			else {
				double figX0 = axes.dataXtoFigureX(line[0].getX());
				double figY0 = axes.dataYtoFigureY(line[0].getY());
				double figX1 = axes.dataXtoFigureX(line[1].getX());
				double figY1 = axes.dataYtoFigureY(line[1].getY());
				//System.out.println("Rect : " + lineRect.getX() + ", " + lineRect.getY() + " - " + (lineRect.getX()+lineRect.getWidth()) + ", " + (lineRect.getY() + lineRect.getHeight()));
				//System.out.println("Line : " + figX0 + ", " + figY0 + " - " + figX1 + ", " + figY1);
				boolean contains = lineRect.intersectsLine(figX0, figY0, figX1, figY1);
//				double rectX = Math.min(figX0, figX1);
//				double rectY = Math.min(figY0, figY1);
//				double rectWidth = Math.abs(figX1-figX0);
//				double rectHeight = Math.abs(figY1-figY0)+2;
				return contains;
			}
			
		}
		
		if (currentMode == BOXES) {
			double boxWidth = calculateBoxWidth();


			double yAxis = axes.dataYtoFigureY(0);
			
			double dataX = axes.boundsXtoDataX(x+(boxWidth*boxOffset+Math.ceil(boxWidth/2.0))/xFactor);
			int boxIndex = xySeries.getIndexForXVal(dataX);
			Rectangle2D rect = getBoxForIndex(boxIndex, yAxis); 
			//System.out.println( " click x: " + x*xFactor + " data x: " + dataX + "Box index: " + boxIndex + " x: " + rect.getX() + " height: " + rect.getHeight() + " width: " + rect.getWidth() );
			Point2D pos = new Point2D.Double(x*xFactor, y*yFactor);
			if (rect == null)
				return false;
			else
				return rect.contains(pos);
		}
		
//		if (currentMode == POINTS) {
//			return pathShape.intersects(x*xFactor-3, y*yFactor-3, 5, 5);
//		}
		
		return false;
	}
	
	
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
	//	System.out.println("Double clicked, can configure is: " + canConfigure);
		if (canConfigure) {
			popupConfigureTool(pos);
		}
	};
	



	public void drawMarker(Graphics2D g, int x, int y) {
		if (currentMarkerType.equals("Circle")) {
			g.setColor(getLineColor());
			g.fillOval((int)Math.round(x-markerSize/2.0), (int)Math.round(y-markerSize/2.0), markerSize, markerSize);
		}
		if (currentMarkerType.equals("Square")) {
			g.setColor(getLineColor());
			g.fillRect((int)Math.round(x-markerSize/2.0), (int)Math.round(y-markerSize/2.0), markerSize, markerSize);			
		}
		if (currentMarkerType.equals("Diamond")) {
			g.setColor(getLineColor());
			xvals[0] = (int)Math.round(x-markerSize/2.0);
			xvals[1] = x;
			xvals[2] = (int)Math.round(x+markerSize/2.0);
			xvals[3] = x;
			xvals[4] = xvals[0];
			yvals[0] = y;
			yvals[1] = (int)Math.round(y-markerSize/2.0);
			yvals[2] = y;
			yvals[3] = (int)Math.round(y+markerSize/2.0);
			yvals[4] = y;
			g.fillPolygon(xvals, yvals, 5);
		}
		if (currentMarkerType.equals("Plus")) {
			g.setColor(getLineColor());
			g.drawLine((int)Math.round(x-markerSize/2.0), y, (int)Math.round(x+markerSize/2.0), y);
			g.drawLine(x, (int)Math.round(y-markerSize/2.0), x, (int)Math.round(y+markerSize/2.0));
		}
		if (currentMarkerType.equals("X")) {
			g.setColor(getLineColor());
			g.drawLine((int)Math.round(x-markerSize/2.0), (int)Math.round(y-markerSize/2.0), (int)Math.round(x+markerSize/2.0), (int)Math.round(y+markerSize/2.0));
			g.drawLine((int)Math.round(x-markerSize/2.0), (int)Math.round(y+markerSize/2.0), (int)Math.round(x+markerSize/2.0), (int)Math.round(y-markerSize/2.0));
		}
	}
	
	
	public void setScale(double xFactor, double yFactor, Graphics g) {
		setDataBounds();
				
		currentTransform.setToScale(xFactor, yFactor);
		if (path != null) {
			for(int i=0; i<path.length; i++) {
				if (path[i] != null)
					currentTransform.transform(path[i], path[i]);
			}
		}
		
		this.xFactor = xFactor;
		this.yFactor = yFactor;	
		scaleHasBeenSet = true;
	}
	
	
	/**
	 * Returns the rectangular box shape in pixel coordinates associated with the index i in the 
	 * data series. Requires knowing what the box width is and where the y-axis is.
	 */
	protected Rectangle2D getBoxForIndex(int i, double yZero) {
		if (i>=xySeries.size()) {
			return null;
		}
		double boxWidth = calculateBoxWidth();
		
		double halfBox = Math.ceil(boxWidth/2.0);
		double dataY = axes.dataYtoFigureY(xySeries.getY(i));
		double xOffset = boxOffset*boxWidth;
		if (xySeries.getY(i)>0) 
			boxRect.setRect(axes.dataXtoFigureX(xySeries.getX(i))-halfBox-xOffset, dataY, boxWidth, yZero-dataY);
		else 
			boxRect.setRect(axes.dataXtoFigureX(xySeries.getX(i))-halfBox-xOffset, yZero, boxWidth, dataY-yZero);

		return boxRect;
	}
	
	public void paint(Graphics2D g) {		
		if (! dataBoundsSet )
			setDataBounds();
	
		
		if (isSelected) {
			g.setColor(highlightColor);
			g.setStroke(highlightStroke);
			if (currentMode == LINES || currentMode == POINTS_AND_LINES || currentMode == BOXES) {
				drawPath(g);
		
			}
		}
		
		g.setStroke(normalStroke);
		
		if (currentMode == LINES) {
			g.setColor(getLineColor());
			drawPath(g);
			
		}
		
		if (currentMode == BOXES) {
			g.setColor(getLineColor());
			double yAxis = axes.dataYtoFigureY(0);
			
			for(int i=0; i<xySeries.size(); i++) {
				Rectangle2D rect = getBoxForIndex(i, yAxis);
				drawBox(g, rect);
			}
		}

		
		if (currentMode == POINTS ) {
			g.setColor(getLineColor());
			for(int i=0; i<xySeries.size(); i++) {
				drawMarker(g, round(axes.dataXtoFigureX(xySeries.getX(i))), round(axes.dataYtoFigureY(xySeries.getY(i))));
			}
		}
		
		if (currentMode == POINTS_AND_LINES ) {
			g.setColor(getLineColor());
			drawPath(g);
		
			
			g.setColor(getLineColor());
			for(int i=0; i<xySeries.size(); i++) {
				drawMarker(g, round(axes.dataXtoFigureX(xySeries.getX(i))), round(axes.dataYtoFigureY(xySeries.getY(i)))); 
			}
		}	
		
		g.setStroke(normalStroke);

	}



	private void drawPath(Graphics2D g) {
		if (path == null)
			return;
		Point2D first = path[0];
		for(int i=1; i<path.length; i++) {
			Point2D second = path[i];
			if (first != null && second != null)
				g.drawLine( (int)first.getX(), (int)first.getY(), (int)second.getX(), (int)second.getY());
			first = second;
		}
	}


	/**
	 * Draws the rectangular box that corresponds to a particular point in this series  
	 * @param rect
	 */
	private void drawBox(Graphics2D g, Rectangle2D rect) {
		g.setColor(getLineColor());
		g.fill(rect);

		if (isSelected) {
			g.setColor(highlightColor);
			g.setStroke(highlightStroke);
			g.draw(rect);
		}
		
		if (rect.getWidth()>4) {
			if (decorateBoxes) {
				int dwidth = (int)Math.round(rect.getWidth()/2.0);
				for(int i=0; i<dwidth; i++) {
					g.setColor(new Color(1.0f, 1.0f, 1.0f, (0.2f)*(1.0f-(float)i/(float)dwidth)));
					g.drawLine((int)Math.round(rect.getX()+i), (int)Math.round(rect.getY()), (int)Math.round(rect.getX()+i), (int)Math.round(rect.getY()+rect.getHeight()));
				}

			}
			
			g.setStroke(normalStroke);
			g.setColor(boxOutlineColor);
			g.draw(rect);
			
		}		

	}

	
}
