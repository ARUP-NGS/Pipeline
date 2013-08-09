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

import gui.figure.ElementList;
import gui.figure.FigureElement;
import gui.figure.TextElement;
import gui.figure.VerticalTextElement;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;

/**
 * A versatile figure that displays multiple  "xy" data series, where the data are in the form of an XYSeries.
 * The XYSeries are wrapped in XYSeriesElement objects that can be drawn. Legends and both X and Y labels
 * are supported. 
 * 
 * @author brendan
 *
 */
public class XYSeriesFigure extends SeriesFigure {

	TextElement xLabelElement; //Element describing x-label
	VerticalTextElement yLabelElement; //Element describing y-label
	AxesElement axes; //Element drawing the axes
	LegendElement legend; //Element drawing the leged
	DataPosElement dataPosElement; //Element showing the mouse position in data coordinates 
	
	Color[] colorList = new Color[6];
	
	boolean drawLegend = true;

	public XYSeriesFigure() {
		setLayout(null); 

		seriesElements = new ArrayList<SeriesElement>();
		xLabelElement = new TextElement("Position in sequence", this);
		xLabelElement.setPosition(0.525, 0.925);
		xLabelElement.setCanConfigure(true);
		xLabelElement.setFontSize(13);
		xLabelElement.setMobile(true);
		yLabelElement = new VerticalTextElement("     Value     ", this);
		yLabelElement.setPosition(0.05, 0.4);
		yLabelElement.setFontSize(12);
		yLabelElement.setCanConfigure(true);
		yLabelElement.setMobile(true);
		axes = new AxesElement(this);
		axes.setBounds(0.065, 0.05, 0.85, 0.85);
		axes.setCanConfigure(true);
		addMouseListeningElement(axes);
		//Note that is is probably necessary to have the axes element as first in this list
		//Since calls to its setScale will come before those to the SeriesElements, which is 
		//required for the proper calculations in the SeriesElements
		addElement(axes);
		axes.setZPosition(-10);
		addElement(xLabelElement);
		addElement(yLabelElement);
		
		legend = new LegendElement(this);
		legend.setBounds(0.8, 0.02, 0.15, 0.1);
		legend.setZPosition(10);
		legend.setMobile(true);
		legend.setCanConfigure(true);
		addElement(legend);
		
		dataPosElement = new DataPosElement(axes, this);
		addMouseListeningElement(dataPosElement);
		addElement(dataPosElement);
		
		colorList[0] = Color.blue;
		colorList[1] = Color.red;
		colorList[2] = Color.GREEN;
		colorList[3] = Color.cyan;
		colorList[4] = Color.magenta;
		colorList[5] = Color.black;
	}
	
	
	/**
	 * Informs the axes element of what its data bounds should be
	 */
	public void inferBoundsFromCurrentSeries() {
		inferXBoundsFromSeries();
		inferYBoundsFromSeries();
	}

	/**
	 * Attempt to infer what sensible y-boundaries are given the data in the series
	 */
	public void inferYBoundsFromSeries() {
		if (seriesElements.size()==0) {
			axes.setDataBounds(0, 1, 0, 1);
		}
		else {

			double ymin = seriesElements.get(0).getMinY();
			if (ymin > 0)
				ymin = 0;
			double ymax = seriesElements.get(0).getMaxY();
			
			for(int i=1; i<seriesElements.size(); i++) {	
				double serMaxY = seriesElements.get(i).getMaxY(); 
				if ( (!Double.isNaN(serMaxY)) && serMaxY > ymax)
					ymax = serMaxY;
				
				double serMinY = seriesElements.get(i).getMinY(); 
				if ((!Double.isNaN(serMinY)) && serMinY < ymin)
					ymin = serMinY;
				
			}
			
			if (ymax > 0) 
				ymax = upperVal(ymax);
			
			
			if (ymax == ymin) {
				if (Math.abs(ymax)<1e-12) {
					ymin = 0;
					ymax = 0.001;
				}
				else {
					ymin /= 2.0;
					ymax *= 1.5;
				}
			}
			
			
			//System.out.println("Upperval max y is : " + ymax);
			axes.setDataBounds(axes.getXMin(), axes.getXMax(), axes.isAutoYMin() ? ymin : axes.getYMin(), axes.isAutoYMax() ? ymax : axes.getYMax());
			axes.setRationalTicks();
		}
	}
	
	/**
	 * Attempt to infer what sensible x-boundaries (in data coordinates) are for this figure, given the data in the series
	 */
	public void inferXBoundsFromSeries() {
		if (seriesElements.size()==0) {
			axes.setDataBounds(0, 1, 0, 1);
		}
		else {
			double xmin = seriesElements.get(0).getMinX();
			double xmax = seriesElements.get(0).getMaxX();
			
			for(int i=1; i<seriesElements.size(); i++) {
				double serMaxX = seriesElements.get(i).getMaxX(); 
				if (serMaxX > xmax)
					xmax = serMaxX;
				
				double serMinX = seriesElements.get(i).getMinX(); 
				if (serMinX < xmin)
					xmin = serMinX;
	
			}
			
			if (xmin>0 && (xmax-xmin)/xmin>10 ) {
				xmin = 0;
			}
			
	
			if (xmin == xmax) {
				if (Math.abs(xmin)<1e-12) {
					xmin = 0;
					xmax = 0.001;
				}
				else {
					if (xmin < 0)
						xmin /= -2;
					else
						xmin /= 2;
					if (xmin < 0)
						xmax *= -1.5;
					else
						xmax *= 1.5;
				}
			}
			
			axes.setDataBounds(axes.isAutoXMin() ? xmin : axes.getXMin(), axes.isAutoXMax() ? xmax : axes.getXMax(), axes.getYMin(), axes.getYMax());
			axes.setRationalTicks();
		}
	}
	

	
	
	/**
	 * Whether or not there's a currently selected range (delegated to axes element)
	 * @return
	 */
	public boolean isRangeSelected() {
		return axes.isRangeSelected();
	}
	
	/**
	 * Clears the currently selected range (delegates to axes element)
	 */
	public void clearRangeSelection() {
		axes.clearRangeSelection();
	}
	
	/**
	 * Returns, in data coordinates, the minimum and maximum of the selected range. If there's
	 * no selected range, returns 0,0 
	 * @return
	 */
	public double[] getRangeSelection() {
		return axes.getDataSelectionRange();
	}
	
	
	/**
	 * Recalculate box widths and offsets  everything - this should 
	 * be called after the addition or removal of every series which has
	 * mode == boxes to ensure that boxes aren't overlapping.
	 */
	public void placeBoxSeries() {
		int count = 0;
		for(SeriesElement series : getSeriesElements()) {
			if (series.getType().equals(  XYSeriesElement.BOXES)) {
				count++;
			}
		}
		
		int index = 0;
		for(SeriesElement series : getSeriesElements()) {
			if (series.getType().equals(  XYSeriesElement.BOXES)) {
				series.setBoxWidthAndOffset(count, (double)(index-(count-1.0)/2.0));
				index++;
			}
		}
		
		repaint();
	}
	
	/**
	 * Set whether or not to draw the legend element. Note that we always maintain the legend element, this
	 * just moves it into or out of the element list. 
	 * @param drawLegend
	 */
	public void setDrawLegend(boolean drawLegend) {
		if (this.drawLegend && drawLegend==false) {
			elements.remove(legend);
		}
		else {
			if (this.drawLegend==false && drawLegend) {
				elements.add(legend);
			}
		}
		this.drawLegend = drawLegend;
		
	}
	
	public AxesElement getAxes() {
		return axes;
	}
	
	/**
	 * Set the font used for the x and y axis labels (not the ticks)
	 * @param font
	 */
	public void setAxisLabelFont(Font font) {
		xLabelElement.setFont(font);
		yLabelElement.setFont(font);
		repaint();
	}
	
	public void setLegendFont(Font font) {
		legend.setFont(font);
		repaint();
	}
	
	/**
	 * Set the distance (in data units) between x ticks on the axes element
	 * @param spacing
	 */
	public void setXTickSpacing(double spacing) {
		axes.setXTickSpacing(spacing);
	}
	
	/**
	 * Add / remove the legend element from the figure 
	 * @param showLegend
	 */
	public void setShowLegend(boolean showLegend) {
		if (showLegend && !elements.contains(legend)) {
			addElement(legend);
		}
		if ( (!showLegend) ) {
			elements.remove(legend);
		}
	}
	
	/**
	 * Set the distance (in data units) between y ticks on the axes element
	 * @param spacing
	 */
	public void setYTickSpacing(double spacing) {
		axes.setYTickSpacing(spacing);
	}
	
	public void setRationalXTicks() {
		axes.setRationalTicks();
	}
	
	public double getXMax() {
		return axes.getXMax();
	}
	
	public double getYMax() {
		return axes.getYMax();
	}
	
	public double getXMin() {
		return axes.getXMin();
	}
	
	public double getYMin() {
		return axes.getYMin();
	}

	/**
	 * Return the number of series shown in this figure
	 * @return
	 */
	public int getSeriesCount() {
		return seriesElements.size();
	}
	
	public void setXLabel(String label) {		
		if (label == null)
			this.elements.remove(xLabelElement);
		else {
			if (! elements.contains(xLabelElement))
				addElement(xLabelElement);
			xLabelElement.setText(label);
		}
	}
	
	public void setYLabel(String label) {
		if (label == null)
			this.elements.remove(yLabelElement);
		else {
			if (! elements.contains(yLabelElement))
				addElement(yLabelElement);
			yLabelElement.setText(label);
		}
	}
	
	/**
	 * Add a new series to this this figure. A new XYSeriesElement is generated and added to both the list of
	 * FigureElements tracked by the Figure, as well as the list of SeriesElements maintained in SeriesFigure.
	 * @param newSeries
	 * @return newElement The newly created element
	 */
	public XYSeriesElement addDataSeries(XYSeries newSeries) {
		XYSeriesElement newElement = new XYSeriesElement(newSeries, axes, this);
		newElement.setLineColor( colorList[ seriesElements.size() % colorList.length] );
		seriesElements.add(newElement);
		addElement(newElement);
		//newElement.setCanConfigure(true);
		inferBoundsFromCurrentSeries();
		return newElement;
	}
	
	/**
	 * Add the given XYSeriesElement to the elements plotted in this figure
	 * @param el
	 */
	public void addSeriesElement(XYSeriesElement el) {
		if (!seriesElements.contains(el)) {
			seriesElements.add(el);
			addElement(el);
			if (el.getType().equals( SeriesElement.BOXES ))
				placeBoxSeries();
		}
		else {
			System.err.println("Series list already contain element, not adding it");
		}
	}
	
	/**
	 * Sets whether or not mouse dragging causes the selection region to appear in the axes
	 * @param allowMouseDragSelection
	 */
	public void setAllowMouseDragSelection(boolean allowMouseDragSelection) {
		axes.setAllowMouseDragSelection(allowMouseDragSelection);
	}
	
	
	public void componentResized(ComponentEvent arg0) {
		
		for(Object el : elements) {
			FigureElement element = (FigureElement)el;
			element.setScale(getWidth(), getHeight(), getGraphics());
		}
		
	}
	

	private double upperVal(double x) {
		double pow = 1;
		if (x==1)
			return 1;
				
		if (x>1.0) {
			while(x>1.0) {
				x /= 10.0;
				pow *= 10;
			}				
		}
		else {
			while(x<1.0) {
				x *= 10.0;
				pow /= 10;
			}
			//We want x to be between zero and 1
			x /= 10;
			pow *= 10;
		}
		
		if (x>0.5) 
			return 1.0*pow;
		if (x>0.25) 
			return 0.5*pow;
		if (x>0.15) 
			return 0.25*pow;
		if (x>0.1) 
			return 0.15*pow;
		
		return 0.1*pow;
	}

	
	/**
	 * Remove the series element associated with the given series (also removes element from parent Figure)
	 * @param series
	 */
	@Override public void removeSeries(AbstractSeries series) {
		SeriesElement toRemove = null;
		for(SeriesElement el : seriesElements) {
			if (el.getSeries() == series)
				toRemove = el;
		}
		if (toRemove != null) {
			seriesElements.remove(toRemove);
			elements.remove(toRemove);
			if (toRemove.getType().equals(  XYSeriesElement.BOXES)) {
				placeBoxSeries();
			}
			fireSeriesRemovedEvent(toRemove.getSeries());
		}
	}
	
	public double lowerVal(double x) {
		return -1.0*upperVal(-1.0*x);
	}

	public ElementList getElementList() {
		return elements;
	}

	public void setYMax(double max) {
		axes.setYMax(max);
	}
	


}
