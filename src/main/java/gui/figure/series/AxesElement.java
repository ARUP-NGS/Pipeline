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
import gui.figure.StringUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

/**
 * Paints graph axes in the specified boundaries, using the minXVal... etc to paint labels.
 * Note that there are at least three coordinate systems in use here. First is the 'data coordinates',
 * which describe points in data space. For instace, 0,0 is the origin of the axes, etc. 
 *  "Bounds" coords describes everything scaled to 0..1, so it's easy to stretch these to whatever
 * the figure size requires. Here (0, 0) describes the upper left corner of the figure, and 0.5, 0.5 is
 * the center of the figure. 
 *  "Figure" coords has units of pixels, with 0,0 meaning the upper-left corner of the containing Figure  
 * 
 * @author brendan
 *
 */
public class AxesElement extends FigureElement {


	private double minXVal = 0;
	private double maxXVal = 1;
	private double minYVal = 0;
	private double maxYVal = 1;
	
	//If true, calls to 'inferBounds...' will rescale the indicated parameter
	//If false, calls to inferBounds will not alter the parameter
	private boolean autoXMin = true;
	private boolean autoYMin = true;
	private boolean autoXMax = true;
	private boolean autoYMax = true;
	
	private double xTickSpacing = 1.0;
	private double yTickSpacing = 1.0;
//	private boolean hasUserXTickNum = false;	//True if user has set x tick number
//	private boolean hasUserYTickNum = false;	//True if user has set y tick number
	private double xTickWidth = 0.02;
	private double yTickWidth = 0.01;
	private int fontSize = 13;
	
	//These indicate if the user has set values for the x or y axis, and they we should not auto-set them
//	boolean hasUserX = false;
//	boolean hasUserY = false;
	
	private boolean drawYGrid = true;
	private Color yGridColor = Color.lightGray;
	
	private boolean drawXGrid = false;
	private Color xGridColor = Color.LIGHT_GRAY;
	
	private boolean recalculateBounds = true;
	
	
	private Font xLabelFont;
	private Font exponentFont;
	
	private static Color selectionRegionColor = new Color(0.0f, 0.25f, 0.5f, 0.1f);
	
	//These fields are set in paint(), which is a bit inefficient but seems not to matter
	//Ideally, they should only be set when the component is resized... but some of them depend
	//on knowing how big the labels are, which requires graphics, which we don't have when things
	//are resized
	private double bottomSpace;
	private double leftSpace;
	private double graphAreaWidth;
	private double graphAreaHeight;
	private double graphAreaBottom;
	private double graphAreaTop;
	private double graphAreaLeft;
	private double fontHeight;
	private double xAxisPos;
	private double yAxisPos;
	private double yAxisZero;
	private double zeroY;
	private double positiveFrac;
	
	private NumberFormat labelFormatter;
	private NumberFormat scientificFormatter;
	private NumberFormat mantissaFormatter;
	
	private String testLabel = "0.0000";
	
	private boolean isXSelected = false;
	private boolean isYSelected = false;
	
	private Stroke normalStroke;
	private Stroke highlightStroke;
	
	//boolean forceIntegralXLabels = false;
	private boolean drawMinorXTicks = true;

	private boolean drawMinorYTicks = true;

	private boolean drawMousePosTick = false;
	private java.awt.Point mousePos;
	
	private List<String> xLabelList = null; //An alternate listing of string-valued x labels. This is used for integer values if it is not null.

	private Point2D mouseDragStart = new Point2D.Double(0,0);
	private Point2D mouseDragEnd = new Point2D.Double(0, 0);
	private boolean mouseIsBeingDragged = false; 
	
	//Controls whether dragging the mouse causes the rectangular selection area to appear
	private boolean allowMouseDragSelection = true;
	
	//Font for drawing x-axis values during mouse drag
	private Font mouseDragNumberFont = new Font("Sans", Font.PLAIN, 9);

	//These define whether or not a range has been selected by the user via a mouse drag,
	//and what the left and right boundaries of the range are
	private int leftMarkerPos = 0;
	private int rightMarkerPos = 0;
	private boolean isRangeSelected = false;

	public AxesElement(Figure parent) {
		super(parent);
		labelFormatter  = new DecimalFormat("###.##");
		mantissaFormatter  = new DecimalFormat("#.##");
		scientificFormatter = new DecimalFormat("0.0##E0##");
		xLabelFont = new Font("Sans", Font.PLAIN, fontSize);
		exponentFont = new Font("Sans", Font.PLAIN, round(fontSize/1.3));
		//configFrame = new AxesConfigFrame(parent, "Configure Axis Values");
		
		normalStroke = new BasicStroke(1.0f);
		highlightStroke = new BasicStroke(3.0f);
		mousePos = new java.awt.Point(0,0);
	}
	
	/**
	 * Set the point size of the font used to draw the axes labels
	 * @param fontSize
	 */
	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
		xLabelFont = new Font("Sans", Font.PLAIN, fontSize);
		exponentFont = new Font("Sans", Font.PLAIN, round(fontSize/1.3));
	}
	
	protected void mouseMoved(Point2D pos) {
		if (bounds.contains(pos)) {
			double dataX = boundsXtoDataX(pos.getX());
			double dataY = boundsYtoDataY(pos.getY());
			if (dataX>= minXVal && dataX <= maxXVal && dataY >= minYVal && dataY <= maxYVal) {
				drawMousePosTick = true;
				mousePos.x = round(pos.getX()*xFactor);
				mousePos.y = round(pos.getY()*yFactor);
			}
			else {
				drawMousePosTick = false;
			}
			
		}
		else {
			drawMousePosTick = false;
		}
	}

	/**
	 * Sets the x labels to be those provided by this list. When provided, no numbers are drawn for the labels, and
	 * these are printed in order along the integer x-values of the x axis. Some values, such as x tick spacing, are
	 * ignored when this is set.  
	 * @param labels
	 */
	public void setXLabels(List<String> labels) {
		xLabelList = labels;
		xTickSpacing = 1.0;
	}
	
	/**
	 * Returns the x label scheme to the normal numerical variety. 
	 */
	public void clearXLabels() {
		xLabelList = null;
	}
	
	protected void mousePressed(Point2D pos) {
		mouseDragStart.setLocation(pos);  
		mouseDragEnd.setLocation(pos);
		isRangeSelected = false;
		leftMarkerPos = 0;
		rightMarkerPos = 0;
	}
	
	protected void mouseReleased(Point2D pos) {
		mouseIsBeingDragged = false;
		
		if (allowMouseDragSelection && mouseDragStart.getX() != mouseDragEnd.getX()) {
			double newXMin = Math.min(mouseDragStart.getX(), mouseDragEnd.getX());
			double newXMax = Math.max(mouseDragStart.getX(), mouseDragEnd.getX());

			newXMin = this.boundsXtoDataX(newXMin);
			newXMax = this.boundsXtoDataX(newXMax);

			if (mouseDragStart.getX() < mouseDragEnd.getX()) {
				leftMarkerPos = (int)Math.round( xFactor*mouseDragStart.getX());
				rightMarkerPos = (int)Math.round( xFactor*mouseDragEnd.getX());	
			}
			else {
				leftMarkerPos = (int)Math.round( xFactor*mouseDragEnd.getX());
				rightMarkerPos = (int)Math.round( xFactor*mouseDragStart.getX());
			}
		}
		
		mouseDragStart.setLocation(0, 0);
		mouseDragEnd.setLocation(0, 0);
	
	}
	
	/**
	 * Called by the parental figure as the mouse is being dragged across this element
	 */
	protected void mouseDragged(Point2D pos) {
		mouseDragEnd.setLocation(pos);
		mouseIsBeingDragged = true;
		isRangeSelected = true;
		if (mouseDragStart.getX() < mouseDragEnd.getX()) {
			leftMarkerPos = (int)Math.round( xFactor*mouseDragStart.getX());
			rightMarkerPos = (int)Math.round( xFactor*mouseDragEnd.getX());	
		}
		else {
			leftMarkerPos = (int)Math.round( xFactor*mouseDragEnd.getX());
			rightMarkerPos = (int)Math.round( xFactor*mouseDragStart.getX());
		}
	}
	
	/**
	 * If there is a currently selected range of data points, typically made by dragging
	 * the mouse in the axes area
	 * @return
	 */
	public boolean isRangeSelected() {
		return isRangeSelected;
	}
	
	/**
	 * Clears the current selection range and resets both marker positions to 0
	 */
	public void clearRangeSelection() {
		isRangeSelected = false;
		leftMarkerPos = 0;
		rightMarkerPos = 0;
	}
	
	/**
	 * Obtain the current range selection as a double[2] in DATA coords, where double[0] is the leftmost
	 * point and double[1] is the rightmost point. If there is no range selection than 0,0 is returned
	 * 
	 * @return
	 */
	public double[] getDataSelectionRange() {
		double[] range = new double[2];
		if (isRangeSelected==false) {
			range[0] = 0;
			range[1] = 0;
		}
		else {
			range[0] = figureXtoDataX(leftMarkerPos);
			range[1] = figureXtoDataX(rightMarkerPos);
		}
		return range;
	}
	
	public void setDataBounds(double xmin, double xmax, double ymin, double ymax) {
		this.maxXVal = xmax;
		this.maxYVal = ymax;
		this.minXVal = xmin;
		this.minYVal = ymin;
		recalculateBounds = true;
	}
	
//	public void popupConfigureTool(java.awt.Point pos) {
//		setSelected(true);
//		if (xAxisContains(pos.x, pos.y) )  {
//			isXSelected = true;
//			isYSelected = false;
//			configFrame.display(this, minXVal, maxXVal, xTickSpacing, fontSize, pos, AxesConfigFrame.X_AXIS);
//		}
//		else {
//			if (yAxisContains(pos.x, pos.y) ) {
//				isXSelected = false;
//				isYSelected = true;
//				configFrame.display(this, minYVal, maxYVal, yTickSpacing, fontSize, pos, AxesConfigFrame.Y_AXIS);
//			}
//
//		}
//	}
	
	public void setXTickSpacing(double spacing) {
		xTickSpacing = spacing;
		recalculateBounds = true;
	}
	
	public void setYTickSpacing(double spacing) {
		yTickSpacing = spacing;
		recalculateBounds = true;
	}
	
	public void setNumXTicks(int num) {
		xTickSpacing = (maxXVal - minXVal)/num;	
		recalculateBounds = true;
	}
	
	public void setNumYTicks(int num) {
		yTickSpacing = (maxYVal - minYVal)/num;
		recalculateBounds = true;
	}
	
	
	public Point getLowerLeftCorner() {
		return new Point(round(graphAreaLeft), round(graphAreaBottom) );
	}
	
	/**
	 * Returns the pixel coordinates where the data point (0, 0) should
	 * be plotted. Note that this may not be even remotely near the graph
	 * area boundaries
	 * 
	 * @return The point at which the data point (0, 0) should be plotted, in pixels;
	 */
	public Point2D getOrigin() {
		double x = dataXtoFigureX(0);
		double y = dataYtoFigureY(0);
		return new Point2D.Double(x, y);
	}
	
	public void setDrawMinorXTicks(boolean drawMinorXTicks) {
		this.drawMinorXTicks = drawMinorXTicks;
	}


	public void setDrawMinorYTicks(boolean drawMinorYTicks) {
		this.drawMinorYTicks = drawMinorYTicks;
	}
	
	public void setXMax(double xMax) {
		maxXVal = xMax;
		recalculateBounds = true;
	}
	
	public void setXMin(double xMin) {
		minXVal = xMin;
		recalculateBounds = true;
	}
	
	public void setYMin(double yMin) {
		minYVal = yMin;
		recalculateBounds = true;
	}
	
	public void setYMax(double yMax) {
		maxYVal = yMax;
		recalculateBounds = true;
	}
	
	public boolean isAutoXMin() {
		return autoXMin;
	}

	public void setAutoXMin(boolean autoXMin) {
		this.autoXMin = autoXMin;
	}

	public boolean isAutoYMin() {
		return autoYMin;
	}

	public void setAutoYMin(boolean autoYMin) {
		this.autoYMin = autoYMin;
	}

	public boolean isAutoXMax() {
		return autoXMax;
	}

	public void setAutoXMax(boolean autoXMax) {
		this.autoXMax = autoXMax;
	}

	public boolean isAutoYMax() {
		return autoYMax;
	}

	public void setAutoYMax(boolean autoYMax) {
		this.autoYMax = autoYMax;
	}
	
	public void setDrawXGrid(boolean drawXGrid) {
		this.drawXGrid = drawXGrid;
	}
	
	public void setDrawYGrid(boolean drawYGrid) {
		this.drawYGrid = drawYGrid;
	}
	
//	public void setXAxisOptions(AxesOptions ops) {
//		if (ops.min != Double.NaN) {
//			if (ops.min!=minXVal)
//				autoXMin = true;
//			minXVal = ops.min;
//		}
//		if (ops.max != Double.NaN) {
//			if (ops.max!=maxXVal)
//				hasUserX = true;
//			maxXVal = ops.max;
//		}
//		if (ops.tickSpacing > 0) {
//			this.xTickSpacing = ops.tickSpacing;
//		}
//
//		
//		drawXGrid = ops.drawAxis;
//		recalculateBounds = true;
//		setFontSize(ops.fontSize);
//		parent.repaint();
//	}
//	
//	public void setYAxisOptions(AxesOptions ops) {
//		if (ops.min != Double.NaN) {
//			if (ops.min!=minYVal)
//				hasUserY = true;
//			minYVal = ops.min;
//		}
//		if (ops.max != Double.NaN) {
//			if (ops.max!=maxYVal)
//				hasUserY = true;
//			maxYVal = ops.max;
//		}
//		if (ops.tickSpacing > 0) {
//			this.yTickSpacing = ops.tickSpacing;
//		}
//		
//		drawYGrid = ops.drawAxis;
//		recalculateBounds = true;
//		setFontSize(ops.fontSize);
//		parent.repaint();
//	}
	
	private boolean xAxisContains(int x, int y) {
		if (y >= graphAreaBottom && y < (bounds.y+bounds.height)*yFactor) {
			return true;
		}
	
		if (y >= (xAxisPos-4) && y < (xAxisPos+4)) {
			return true;
		}
		
		return false;
	}
	
	private boolean yAxisContains(int x, int y) {
		if (x > (graphAreaLeft-8) && x < (graphAreaLeft+2) ) {
			if (y > graphAreaTop && y < graphAreaBottom) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * We only respond to clicks that are over the axes, not anywhere in the graph area.
	 * This is confusing if the x axis does not lie on the bottom of the graph..
	 */
	public boolean contains(double x, double y) {
		int px = round(x*xFactor);
		int py = round(y*yFactor);
		
		return xAxisContains(px, py) || yAxisContains(px, py);
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
		
		if (g!=null)
			initializeBounds(g);
	}
	
	/**
	 * Overrides FigureElement.clicked to set separate x and y axis selection
	 */
	public void clicked(Point pos) { 
		setSelected(true);
		if (xAxisContains(pos.x, pos.y)) {
			isXSelected = true;
			isYSelected = false;
		}
		
		if (yAxisContains(pos.x, pos.y)) {
			isXSelected = false;
			isYSelected = true;
		}	
	}
	
	/**
	 * Called (by Figure) when this element is single clicked
	 */
	public void unClicked() { 
		setSelected(false);
		isXSelected = false;
		isYSelected = false;
	}
	
	public double getYMin() {
		return minYVal;
	}
	
	public double getYMax() {
		return maxYVal;
	}
	
	public double getXMax() {
		return maxXVal;
	}
	
	public double getXMin() {
		return minXVal;
	}
	
	public double getXTickSpacing() {
		return xTickSpacing;
	}
	
	public double getYTickSpacing() {
		return yTickSpacing;
	}
	
	
	
	/**
	 * Recalculate some of the values in pixel units, this happens whenever the size changes.
	 * @param g
	 */
	private void initializeBounds(Graphics g) {
		FontMetrics fm = g.getFontMetrics();
		fontHeight = fm.getHeight();
		
		System.out.println("Initializing axes bounds with xFactor : " + xFactor + " yFactor : " + yFactor + " bounds.x: " + bounds.x + " bounds.y: " + bounds.y);
		
		bottomSpace = fontHeight + xTickWidth*yFactor;
		leftSpace = fm.stringWidth(testLabel) + xTickWidth*xFactor;
		graphAreaTop = bounds.y*yFactor;
		graphAreaWidth = bounds.width*xFactor - leftSpace;
		graphAreaHeight = bounds.height*yFactor - bottomSpace;
		graphAreaBottom = (bounds.y+bounds.height)*yFactor - bottomSpace;
		graphAreaLeft = bounds.x*xFactor + leftSpace;
		
		
		positiveFrac = maxYVal/(maxYVal-minYVal);	//The portion of the graph above the y=0 line		
		zeroY =  graphAreaTop + positiveFrac*graphAreaHeight; //The pixel position where y=0
		
		//Place the x-axis in the correct position
		if (maxYVal<0)
			xAxisPos = bounds.y*yFactor;
		else
			xAxisPos = Math.min(zeroY, graphAreaBottom); //Where we draw the x axis
		
		//Always draw the y axis at graphAreaLeft. This could easily be changed, someday
		yAxisPos = graphAreaLeft; 
		if (maxXVal<0)  
			yAxisZero = graphAreaLeft;
		else {
			yAxisZero = Math.min(graphAreaLeft+graphAreaWidth, graphAreaLeft+graphAreaWidth*(1.0-maxXVal/(maxXVal-minXVal)));
		}
		
		recalculateBounds = false;
	}

	public double boundsYtoDataY(double boundsY) {
		double figureY = (boundsY-bounds.y)*yFactor+graphAreaTop;
		return figureYtoDataY(figureY);
	}
	
	public double boundsXtoDataX(double boundsX) {
		
		double figureX = boundsX*xFactor;
		double x = figureXtoDataX(figureX);
		return x;
	}
	
	/**
	 * Converts a y-value in the 'figure space' which is the number of pixels from the top
	 * of this AxesElement, into a y-value in the 'data space', which is defined by maxYVal and minYVal
	 * @param figureY
	 * @return The value in 'data' coordinates
	 */
	public double figureYtoDataY(double figureY) {
		double top = graphAreaTop;
		double bottom = graphAreaBottom;
		return (1.0-(figureY-top)/(bottom-top))*(maxYVal-minYVal)+minYVal;
	}
	
	/**
	 * Converts a y value in data coordinates to an y value in pixels
	 * @param A value in 'data space'
	 * @return A value in figure (pixel) space
	 */
	public double dataYtoFigureY(double dataY) {
		double top = graphAreaTop;
		double bottom = graphAreaBottom;
		return (1.0-(dataY - minYVal)/(maxYVal-minYVal))*(bottom-top)+top ;
	}
	
	/**
	 * Converts an x-value in the 'figure space' which is the number of pixels from the left side
	 * of this AxesElement, into a x-value in the 'data space', which is defined by maxXVal and minXVal
	 * @param figureX 
	 * @return The value in 'data' coordinates
	 */	
	public double figureXtoDataX(double figureX) {
		return ((figureX-graphAreaLeft)/(graphAreaWidth))*(maxXVal-minXVal)+minXVal;
	}
	
	
	/**
	 * Converts an xvalue in data coordinates to an x value in pixels
	 * @param dataX
	 * @return
	 */
	public double dataXtoFigureX(double dataX) {
		 double figureX = ( (dataX-minXVal)/(maxXVal-minXVal))*graphAreaWidth+graphAreaLeft;
		 return figureX;
	}
	
	/**
	 * Converts an xvalue in data coordinates to an x value in bounds space
	 * @param dataX
	 * @return
	 */
	public double dataXtoBoundsX(double dataX) {
		 double boundsX = (dataX-minXVal)/(maxXVal-minXVal)*graphAreaWidth/xFactor+graphAreaLeft/xFactor;
		 //System.out.println("Computing dXtobX, dataX: " + dataX + "\n minx: " + minXVal + " maxX: " + maxXVal + "\n xFactor : " + xFactor + " graph width: " + graphAreaWidth + "\n graph left: " + graphAreaLeft);
		 return boundsX;
	}

	/**
	 * Converts a y value in data coordinates to an y value in bounds (0..1) space
	 * @param A value in 'data space'
	 * @return A value in bounds (0..1) space
	 */
	public double dataYtoBoundsY(double dataY) {
		double top = graphAreaTop/yFactor;
		double bottom = graphAreaBottom/yFactor;
		return (1.0-(dataY - minYVal)/(maxYVal-minYVal))*(bottom-top)+top ;
	}
	
	public DataBounds getDataBounds() {
		DataBounds dBounds = new DataBounds(minXVal, maxXVal, minYVal, maxYVal);
		return dBounds;
	}

	public boolean showXGrid() {
		return drawXGrid;
	}
	
	public boolean showYGrid() {
		return drawYGrid;
	}
	
	public boolean isXSelected() {
		return isXSelected;
	}
	
	public boolean isYSelected() {
		return isYSelected;
	}
	
	/**
	 * Attempts to set a logical x-tick spacing value
	 */
	public void setRationalTicks() {
		double range = maxXVal - minXVal;
		double numXticks = 4;
		int log = (int)Math.floor( Math.log10(range));
		int pow = (int)Math.round( Math.pow(10.0, log-2));
		if (pow != 0)
			xTickSpacing = Math.round(range/numXticks*pow)/pow;
		else
			xTickSpacing = range/numXticks;
		//System.out.println("X range: " + range + " log: " + log + " pow: " + pow + " x spacing: " + xTickSpacing);
		
		double numYticks = 4;
		range = maxYVal - minYVal;
		log = (int)Math.floor( Math.log10(range));
		pow = (int)Math.round( Math.pow(10.0, log-2));
		if (pow != 0)
			yTickSpacing = Math.round(range/numYticks*pow)/pow;
		else
			yTickSpacing = range/numYticks;
		//System.out.println("Y range: " + range + " log: " + log + " pow: " + pow + " y spacing: " + yTickSpacing);
		
	}
	
	public void setSelected(boolean isSelected) {
		if(this.isSelected && isSelected ) {
			if ( isXSelected) {
				isXSelected = false;
				isYSelected = true;
			} 
			else if (isYSelected) {
				isYSelected = false;
				isXSelected = false;
			}
		}
		else if (!this.isSelected && isSelected) {
			this.isSelected = true;
			isXSelected = true;
		}
		else if(!isSelected) {
			this.isSelected = false;
			isXSelected = false;
			isYSelected = false;
		}
	}
	
	public void paint(Graphics2D g) {
		g.setColor(foregroundColor);
		g.setFont(xLabelFont);

		
		if (recalculateBounds) {
			initializeBounds(g);
		}
		
//		if (allowMouseDragSelection && mouseIsBeingDragged) {
//			int xStart = round(mouseDragStart.x*xFactor);
//			int xEnd = round(mouseDragEnd.x*xFactor);
//			int rectL = Math.min(xStart, xEnd);
//			int wid = Math.abs(xStart-xEnd);
//			g.setColor(selectionRegionColor);
//			g.fillRect(rectL, round(graphAreaTop), wid, round(graphAreaHeight));
//			g.setColor(Color.GRAY);
//			g.drawRect(rectL, round(graphAreaTop), wid, round(graphAreaHeight));	
//		}
		
		if (allowMouseDragSelection && isRangeSelected && leftMarkerPos != rightMarkerPos) {
			g.setColor(selectionRegionColor);
			g.fillRect(leftMarkerPos, round(graphAreaTop), rightMarkerPos-leftMarkerPos, round(graphAreaHeight));
			g.setColor(Color.gray);
			g.drawLine(leftMarkerPos, round(graphAreaTop), leftMarkerPos, round(graphAreaTop+graphAreaHeight));
			g.drawLine(rightMarkerPos, round(graphAreaTop), rightMarkerPos, round(graphAreaTop+graphAreaHeight));
			double dataRX = figureXtoDataX(rightMarkerPos);
			String drxStr = StringUtils.format(dataRX);
			double dataLX = figureXtoDataX(leftMarkerPos);
			String dlxStr = StringUtils.format(dataLX);
			g.setColor(Color.gray);
			g.setFont(mouseDragNumberFont);
			g.drawString(drxStr, rightMarkerPos, round(graphAreaTop+graphAreaHeight+10));
			g.drawString(dlxStr, leftMarkerPos-g.getFontMetrics().stringWidth(dlxStr), round(graphAreaTop+graphAreaHeight+10));
		}

		
		if (drawMousePosTick) {
			g.setColor(Color.LIGHT_GRAY);
			g.setStroke(normalStroke);
			//System.out.println("Drawing mouse pos. tick 1");
			g.drawLine(round(graphAreaLeft-yTickWidth*xFactor), mousePos.y, round(graphAreaLeft), mousePos.y);
			//System.out.println("Drawing mouse pos. tick 2");
			g.drawLine(mousePos.x, round(xAxisPos), mousePos.x, Math.max(round(xAxisPos+2),round(xAxisPos+xTickWidth*yFactor)));
		}
		
		if (isYSelected) {
			g.setStroke(highlightStroke);
			g.setColor(highlightColor);
			paintYAxis(g, false);
		}

		g.setStroke(normalStroke);
		g.setColor(Color.black);

		paintYAxis(g, true);
		
		if (isXSelected) {
			g.setStroke(highlightStroke);
			g.setColor(highlightColor);
			paintXAxis(g, false);
		}


		g.setStroke(normalStroke);
		g.setColor(Color.black);

		paintXAxis(g, true);

	}
	
	/**
	 * Whether or not dragging the mouse over this element causes the selection region to appear
	 * @return
	 */
	public boolean isAllowMouseDragSelection() {
		return allowMouseDragSelection;
	}


	/**
	 * Sets whether or not mouse dragging causes the selection region to appear
	 * @param allowMouseDragSelection
	 */
	public void setAllowMouseDragSelection(boolean allowMouseDragSelection) {
		this.allowMouseDragSelection = allowMouseDragSelection;
	}
	
	/**
	 * Paint the main y-axis line and tick marks
	 * @param g
	 * @param drawTicks If we should draw the tick marks
	 */
	protected void paintYAxis(Graphics2D g, boolean drawTicks) {
		Color origColor = g.getColor();
		
		// Y - axis
		g.drawLine(round(graphAreaLeft), round(graphAreaTop), round(graphAreaLeft), round(graphAreaBottom));
		
		if (yTickSpacing>0) {	
			//Positive ticks and labels
			double tickStep = dataYtoFigureY(0)-dataYtoFigureY(yTickSpacing); //yTickSpacing in pixels 
			int positiveYTicks = (int)Math.floor((double)graphAreaHeight*(maxYVal/(maxYVal-minYVal))/tickStep);
			double 	yLabelStep = figureYtoDataY(xAxisPos)-figureYtoDataY(xAxisPos+tickStep); //really just positive y labels here
			
			int i=0;
			int tickY = round(xAxisPos-i*tickStep);
			while(drawTicks && tickStep > 0 && tickY>=bounds.y && positiveYTicks>0) {
				//Major tick
				g.drawLine(round(graphAreaLeft-yTickWidth*xFactor), tickY, round(graphAreaLeft), tickY);
				if (drawYGrid) {
					g.setStroke(normalStroke);
					g.setColor(yGridColor);
					g.drawLine(round(graphAreaLeft)+1, tickY, round(graphAreaLeft+graphAreaWidth), tickY);
				}
				g.setColor(origColor);
				
				//Minor tick
				if (round(xAxisPos-i*tickStep + tickStep/2.0) < graphAreaBottom )
					g.drawLine(round(graphAreaLeft-yTickWidth/2.0*xFactor), round(xAxisPos-i*tickStep + tickStep/2.0), round(graphAreaLeft), round(xAxisPos-i*tickStep+tickStep/2.0));
				
				if (minYVal>0)	{ 
					paintYLabel(g, round(graphAreaLeft-yTickWidth*xFactor), round(xAxisPos-i*tickStep), i*yLabelStep+minYVal);
				}
				else { 
					paintYLabel(g, round(graphAreaLeft-yTickWidth*xFactor), round(xAxisPos-i*tickStep), i*yLabelStep);
				}
				i++;
				tickY = round(xAxisPos-i*tickStep);
			}

			
			//Negative Y ticks and labels
			if (drawTicks && minYVal<0) {
				i=0;
				tickY = round(xAxisPos+i*tickStep);
				while(tickY<=graphAreaBottom) {
					//Major tick
					g.drawLine(round(graphAreaLeft-yTickWidth*xFactor), tickY, round(graphAreaLeft), tickY);
					if (drawYGrid && tickY != xAxisPos) {
						g.setColor(yGridColor);
						g.drawLine(round(graphAreaLeft)+1, tickY, round(graphAreaLeft+graphAreaWidth), tickY);
					}
					g.setColor(origColor);	
					//Minor tick
					if (drawMinorYTicks && round(xAxisPos+i*tickStep - tickStep/2.0) < graphAreaBottom )
						g.drawLine(round(graphAreaLeft-yTickWidth/2.0*xFactor), round(xAxisPos+i*tickStep-tickStep/2.0), round(graphAreaLeft), round(xAxisPos+i*tickStep-tickStep/2.0));

					if (maxYVal<0) {
						paintYLabel(g, round(graphAreaLeft-yTickWidth*xFactor), round(tickY), -1.0*i*yLabelStep+maxYVal );
					}
					else {
						paintYLabel(g, round(graphAreaLeft-yTickWidth*xFactor), round(tickY), -1.0*i*yLabelStep );

					}
						
					i++;
					tickY = round(xAxisPos+i*tickStep);
				}
				
				//Make sure we draw at least one at the bottom boundary
				if (i==1) {
					tickY = round(graphAreaBottom);
					g.drawLine(round(graphAreaLeft-yTickWidth*xFactor), tickY, round(graphAreaLeft), tickY);
					paintYLabel(g, round(graphAreaLeft-yTickWidth*xFactor), round(tickY), figureYtoDataY(graphAreaBottom));
				}
			}//negative y ticks & labels
			
		}// y tick & label drawing		
	}
	
	protected void paintXAxis(Graphics2D g, boolean drawTicks) {
		
		//	X-axis			
		g.drawLine(round(graphAreaLeft), round(xAxisPos), round(graphAreaLeft+graphAreaWidth), round(xAxisPos));
				
		if (xTickSpacing>0) {
			
			//positive x labels & ticks
			double tickStep = dataXtoFigureX(xTickSpacing)-dataXtoFigureX(0); //xTickSpacing in pixels
			double xLabelStep;
			if (xLabelList!=null)
				xLabelStep = 1;
			else
				xLabelStep = figureXtoDataX(yAxisZero+tickStep)-figureXtoDataX(yAxisZero);
			
			double minorTickOffset = tickStep / 2.0;
			int i=0;
			double tickX;
			if (yAxisZero>=graphAreaLeft && yAxisZero < (graphAreaLeft+graphAreaWidth)) {
				tickX = yAxisZero;
			}
			else {
				tickX = graphAreaLeft;
			}
			
			while(tickStep > 0 && tickX<(graphAreaLeft+graphAreaWidth) && (drawTicks)) {
				int iTickX = round(tickX);
				g.drawLine(iTickX, round(xAxisPos), iTickX, Math.max(round(xAxisPos+2),round(xAxisPos+xTickWidth*yFactor)));
				if (drawXGrid && tickX>(graphAreaLeft+1)) {
					g.setColor(yGridColor);
					g.drawLine(iTickX, round(graphAreaTop), iTickX, round(graphAreaBottom));
				}
			
				//Minor tick
				if (drawMinorXTicks &&  (tickX-minorTickOffset) > graphAreaLeft) {
					g.drawLine(round(tickX-minorTickOffset), round(xAxisPos), round(tickX-minorTickOffset), Math.max(round(xAxisPos+2),round(xAxisPos+xTickWidth*yFactor/2.0)));
				}
				
				//If we're using custom x labels, we change the label indexing value...
				if (xLabelList!=null) {
					
					paintXLabel(g, round(tickX), round(graphAreaBottom+xTickWidth*yFactor), i*xLabelStep);
				}
				else {
					double val;
					if (tickX==yAxisZero)
						val = 0;
					else
						val = figureXtoDataX(tickX);
					paintXLabel(g, round(tickX), round(graphAreaBottom+xTickWidth*yFactor), val);
				}
				i++;
				tickX += tickStep;
			}
			
			//Paint rightmost label, rounding errors cause it to not be painted consistently in 
			//loop above
			int iTickX = round(graphAreaLeft+graphAreaWidth);
			g.drawLine(iTickX, round(xAxisPos), iTickX, Math.max(round(xAxisPos+2),round(xAxisPos+xTickWidth*yFactor)));
			if (xLabelList!=null) {	
				paintXLabel(g, round(tickX), round(graphAreaBottom+xTickWidth*yFactor), i*xLabelStep);
			}
			else {
				double val;
				if (tickX==yAxisZero)
					val = 0;
				else
					val = figureXtoDataX(tickX);
				paintXLabel(g, round(tickX), round(graphAreaBottom+xTickWidth*yFactor), val);
			}
			
			if (minXVal<0) {
				if (maxXVal>0) //represses drawing two zeros which may not overlap completely
					i=1;
				else
					i=0;
				tickX = yAxisZero-i*tickStep;
				while(tickX>=graphAreaLeft) {
					iTickX = round(tickX);
					g.drawLine(iTickX, round(xAxisPos), iTickX, Math.max(round(xAxisPos+2),round(xAxisPos+xTickWidth*yFactor)));
					
					//Minor tick
					if (round(yAxisZero+i*tickStep-tickStep/2.0) < graphAreaLeft+graphAreaWidth )
						g.drawLine(round(yAxisZero-i*tickStep+tickStep/2.0), round(xAxisPos), round(yAxisZero-i*tickStep+tickStep/2.0), round(xAxisPos+xTickWidth*yFactor/2.0));
					
					if (maxXVal<0) {
						paintXLabel(g, round(tickX), round(graphAreaBottom+xTickWidth*yFactor), figureXtoDataX(tickX) /*-1.0*i*xLabelStep+minXVal */);
					}
					else {
						paintXLabel(g, round(tickX), round(graphAreaBottom+xTickWidth*yFactor), figureXtoDataX(tickX));
					}

					i++;
					tickX = yAxisZero-i*tickStep;
				}

			}

		}		
		
	}


	private String[] toScientificNotation(double val) {
		int exp = 1;
		
		if ( Math.abs(val) > 10000 && val != 0) {
			while (Math.abs(val)>=10) {
				val = val/10.0;
				exp++;
			}
			exp--;
		}
		else {
			if ( Math.abs(val) < 0.001 && val != 0) {
				while (Math.abs(val)<1) {
					val *= 10.0;
					exp++;
				}
				exp--;
				exp *= -1;			
			}
			
		}


		String mantissaLabel = mantissaFormatter.format(val);
		String expLabel = mantissaFormatter.format(exp);
		String[] arr = {mantissaLabel, expLabel}; 
		return arr;
	}
	
	private void paintYLabel(Graphics2D g, double xPos, double yPos, double val) {
		
		if (val != 0 &&  (Math.abs(val) > 10000 || Math.abs(val)<0.001) ) {

			String[] labels = toScientificNotation(val);
			String mantissaLabel = labels[0];
			String expLabel = labels[1];
			g.setFont(xLabelFont);
			FontMetrics fm = g.getFontMetrics();
			mantissaLabel = mantissaLabel + "x10";
			Rectangle2D mantissaRect = fm.getStringBounds(mantissaLabel, 0, mantissaLabel.length(), g);
			
			g.setFont(exponentFont);
			fm = g.getFontMetrics();
			Rectangle2D expRect = fm.getStringBounds(expLabel, 0, expLabel.length(), g);

			//Fill rectangle behind label so overlapping labels don't look horrible, just bad
			g.setColor(parent.getBackground());
			mantissaRect.setRect(round(xPos-mantissaRect.getWidth()-2), yPos-2, mantissaRect.getWidth(), mantissaRect.getHeight());
			g.fill(mantissaRect);
			g.setColor(Color.black);
			
			g.setFont(xLabelFont);
			g.drawString(mantissaLabel, round(xPos-mantissaRect.getWidth()-expRect.getWidth()), round(yPos+mantissaRect.getHeight()/2.0));
			
			g.setFont(exponentFont);
			g.drawString(expLabel, round(xPos-expRect.getWidth()), round(yPos-expRect.getHeight()/5.0));
			return;
		}
		else {
			g.setFont(xLabelFont);
			FontMetrics fm = g.getFontMetrics();
			String label = StringUtils.format(val); //labelFormatter.format(val);
			Rectangle2D rect = fm.getStringBounds(label, 0, label.length(), g);
			rect.setRect(round(xPos-rect.getWidth()), yPos-rect.getHeight()*0.67, rect.getWidth(), rect.getHeight());
			g.setColor(parent.getBackground());
			g.fill(rect);
			g.setColor(Color.black);
			g.drawString(label, round(xPos-rect.getWidth()), round(yPos+rect.getHeight()/3.0));
		} //number didn't need to be converted to scientific notation

	} //paintYLabel
	

	
	private void paintXLabel(Graphics2D g, double xPos, double yPos, double val) {
		
		//If a list of x labels has been supplied we use those. We cast 'val' to an integer to look up
		//the index of the list to use. If the index is beyond the end of the list, we draw nothing. 
		if (xLabelList != null) {
			g.setFont(xLabelFont);
			FontMetrics fm = g.getFontMetrics();
			int index = (int)Math.round(val);
			if (index < xLabelList.size()) {
				Rectangle2D strBounds = fm.getStringBounds( xLabelList.get(index), 0, xLabelList.get(index).length(), g );
				g.drawString(xLabelList.get(index), round(xPos-strBounds.getWidth()/2.0), round(yPos+strBounds.getHeight()));
			}
			
			return;
		}
		
		if (val != 0 && ( Math.abs(val) > 10000 || Math.abs(val)<0.001)) {
			String[] labels = toScientificNotation(val);
			String mantissaLabel = labels[0];
			String expLabel = labels[1];
			
			g.setFont(xLabelFont);
			FontMetrics fm = g.getFontMetrics();
			mantissaLabel = mantissaLabel + "x10";
			Rectangle2D mantissaRect = fm.getStringBounds(mantissaLabel, 0, mantissaLabel.length(), g);
			
			g.setFont(exponentFont);
			fm = g.getFontMetrics();
			Rectangle2D expRect = fm.getStringBounds(expLabel, 0, expLabel.length(), g);
			
			//Fill rectangle behind label so overlapping labels don't look horrible, just bad
			g.setColor(parent.getBackground());
			mantissaRect.setRect(round(xPos-mantissaRect.getWidth()/2.0-2), yPos, mantissaRect.getWidth(), mantissaRect.getHeight());
			g.fill(mantissaRect);
			g.setColor(Color.black);
			
			int xLeft = round(xPos-(mantissaRect.getWidth()+expRect.getWidth())/2.0);
			int top = round(yPos+mantissaRect.getHeight()/2.0);
			
			g.setFont(xLabelFont);
			g.drawString(mantissaLabel, xLeft, round(yPos+mantissaRect.getHeight()));
			
			g.setFont(exponentFont);
			g.drawString(expLabel, round(xPos-(mantissaRect.getWidth()+expRect.getWidth())/2.0+mantissaRect.getWidth()), top);
			return;
		}
		else {

			//val is between 0.001 and 10000, does not need to be set in scientific notation
			g.setFont(xLabelFont);
			FontMetrics fm = g.getFontMetrics();
			String label = StringUtils.format(val); // labelFormatter.format(val);
			Rectangle2D rect = fm.getStringBounds(label, 0, label.length(), g);
			rect.setRect(round(xPos-rect.getWidth()/2.0), yPos, rect.getWidth(), rect.getHeight());
			g.setColor(parent.getBackground());
			g.fill(rect);
			g.setColor(Color.black);
			g.drawString(label, round(xPos-rect.getWidth()/2.0), round(yPos+rect.getHeight()));
		}
		
	} //paintXLabel
	
	public class DataBounds {
		public double xMin;
		public double xMax;
		public double yMin;
		public double yMax;
		
		public DataBounds(double xMin, double xMax, double yMin, double yMax) {
			this.xMin = xMin;
			this.yMin = yMin;
			this.yMax = yMax;
			this.xMax = xMax;
		}
	}

	public Rectangle getGraphAreaBounds() {
		Rectangle gaBounds = new Rectangle();
		gaBounds.x = round(graphAreaLeft);
		gaBounds.y = round(graphAreaTop);
		gaBounds.width = round(graphAreaWidth);
		gaBounds.height = round(graphAreaHeight);
		return gaBounds;
	}

	/**
	 * Obtain the font size used for drawing axes labels
	 * @return
	 */
	public int getFontSize() {
		return fontSize;
	}


//	public void setYMax(double max) {
//		if (max>minYVal) 
//			maxYVal = max;
//		else
//			throw new IllegalArgumentException("Cannot set max Y val to be less than min Y val");
//		recalculateBounds = true;
//	}


//	public void setXMin(double xmin) {
//		if (xmin<maxXVal) 
//			minXVal = xmin;
//		else
//			throw new IllegalArgumentException("Cannot set min X val to be greater than max X val");
//		recalculateBounds = true;
//	}
//
//
//	public void setXMax(double xmax) {
//		if (xmax>minXVal) 
//			maxXVal = xmax;
//		else
//			throw new IllegalArgumentException("Cannot set max X val to be less than min X val");
//		recalculateBounds = true;		
//	}


	
}
