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

import gui.figure.FigureElement;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;


/**
 * Draws a legend for a series figure. Configuration allows for adjusting the properties of
 * all the various data series.  
 * @author brendan
 *
 */
public class LegendElement extends FigureElement {

	SeriesFigure seriesParent;
	Font font;
	ArrayList<Integer> yPosn;
	
	Stroke normalStroke;
	Stroke highlightStroke;
	
	LegendConfigFrame configFrame;
	Color backgroundColor = new Color(0.99f, 0.99f, 0.99f, 0.85f);
	
	public LegendElement(SeriesFigure fig) {
		super(fig);
		this.seriesParent = fig;
		font = new Font("Sans", Font.PLAIN, 11);
		yPosn = new ArrayList<Integer>();
		normalStroke = new BasicStroke(1.0f);
		highlightStroke = new BasicStroke(2.0f);
	}
	
	public void paint(Graphics2D g) {
		
		//Don't paint anything if there no are active series
		if (seriesParent.getSeriesElements().size()==0) {
			return;
		}
		
		
		g.setColor(backgroundColor);
		g.setFont(font);
		int markerSpace = 20;
		int pos = 4;
		int maxWidth = 0;
		int totHeight = 0;
		FontMetrics fm = g.getFontMetrics();
		yPosn.clear();
		//Calculate the positions
		g.setColor(Color.green);
		int count = 0;
		for(SeriesElement el : seriesParent.getSeriesElements()) {
			if (el.includeInLegend() && el.getSeries().size()>0) {
				String seriesName = el.getName();
				if (seriesName==null) {
					seriesName = "Unknown series";
				}
				Rectangle2D rect = fm.getStringBounds(seriesName, 0, seriesName.length(), g);
				yPosn.add(round(bounds.y*yFactor+pos+rect.getHeight()) );

				pos += rect.getHeight()+5;
				if (round(rect.getWidth())>maxWidth)
					maxWidth = round(rect.getWidth());
				totHeight += rect.getHeight();
				count++;
			}
		}
		
		//No elements have size > 0, so abort drawing the legend
		if (count == 0)
			return;
		
		g.setClip(0, 0, round(xFactor), round(yFactor));
		bounds.width = (maxWidth+markerSpace+10)/xFactor;
		bounds.x = Math.min((double)parent.getWidth()/(double)xFactor-bounds.width-0.01, bounds.x);
		bounds.height = (yPosn.get( yPosn.size()-1)+5)/yFactor-bounds.y;
		
		if (isSelected()) {
			g.setColor(highlightColor);
			g.fillRect(round(bounds.x*xFactor-2.0), round(bounds.y*yFactor-2.0), round(bounds.width*xFactor+5.0), round(bounds.height*yFactor+5.0));
			g.setColor(Color.white);
			g.fillRect(round(bounds.x*xFactor), round(bounds.y*yFactor), round(bounds.width*xFactor), round(bounds.height*yFactor));			
		}
		
		//Draw the background
		g.setColor(backgroundColor);
		g.fillRect(round(bounds.x*xFactor), round(bounds.y*yFactor), round(bounds.width*xFactor), round(bounds.height*yFactor));
		
		
		int i = 0;
		for(SeriesElement el : seriesParent.getSeriesElements()) {
			if (el.includeInLegend() && el.getSeries().size()>0) {
				String seriesName = el.getName();
				if (seriesName==null) {
					seriesName = "Unknown series";
				}

				g.setColor(el.getLineColor());
				if (el.getType() == XYSeriesElement.LINES)
					g.drawLine(round(bounds.x*xFactor+4), yPosn.get(i)-5, round(bounds.x*xFactor+markerSpace-4), yPosn.get(i)-5);

				if (el.getType() == XYSeriesElement.POINTS_AND_LINES) {
					g.drawLine(round(bounds.x*xFactor+4), yPosn.get(i)-5, round(bounds.x*xFactor+markerSpace-4), yPosn.get(i)-5);
					el.drawMarker(g, round(bounds.x*xFactor+10), yPosn.get(i)-5);
				}

				if (el.getType() == XYSeriesElement.POINTS) {
					//g.drawLine(round(bounds.x*xFactor+4), yPosn.get(i)-5, round(bounds.x*xFactor+markerSpace-4), yPosn.get(i)-5);
					el.drawMarker(g, round(bounds.x*xFactor+10), yPosn.get(i)-5);
				}

				if (el.getType() == XYSeriesElement.BOXES) {
					g.setColor(el.getLineColor());
					g.fillRect(round(bounds.x*xFactor+3), yPosn.get(i)-6, round(markerSpace/2.0-3), 6);
					g.fillRect(round(bounds.x*xFactor+1+markerSpace/2.0), yPosn.get(i)-10, round(markerSpace/2.0-4), 10);
				}

				g.setColor(Color.black);
				g.drawString(seriesName, round(bounds.x*xFactor) + markerSpace, yPosn.get(i));
				i++;
			}
		}
		

		g.setColor(Color.black);
		g.setStroke(normalStroke);
		g.drawRect(round(bounds.x*xFactor), round(bounds.y*yFactor), round(bounds.width*xFactor), round(bounds.height*yFactor));
		
		g.setStroke(normalStroke);
	}
	
	public void popupConfigureTool(java.awt.Point pos) {
		if (configFrame==null)
			configFrame = new LegendConfigFrame((XYSeriesFigure)parent, this);
		
		configFrame.display( seriesParent.getSeriesElements() );
	}

	public void setFont(Font newFont) {
		this.font = newFont;
	}

}
