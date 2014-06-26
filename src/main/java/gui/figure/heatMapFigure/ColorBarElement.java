package gui.figure.heatMapFigure;

import gui.figure.Figure;
import gui.figure.FigureElement;
import gui.figure.StringUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * A color key for heat maps, showing the value associated with various colors
 * @author brendan
 *
 */
public class ColorBarElement extends FigureElement {

	protected Color[] colors = null;
	protected double max = 1.0;
	protected double min = 0;
	protected Font font = new Font("Sans", Font.PLAIN, 10);
	
	public ColorBarElement(Figure parent) {
		super(parent);
	}

	public void setColors(Color[] newColors) {
		this.colors = newColors;
	}
	
	public void setMaxValue(double val) {
		this.max = val;
	}
	
	public void setMinValue(double min) {
		this.min = min;
	}

	@Override
	public void paint(Graphics2D g) {
		int left = (int)Math.round(bounds.x * xFactor);
		int top = (int)Math.round(bounds.y * yFactor);
		int width = (int)Math.round(bounds.width * xFactor);
		int height = (int)Math.round(bounds.height * yFactor);
		
		if (colors == null) {
			return;
		}
		
		double boxHeight = bounds.height*yFactor / colors.length;
		for(int i=0; i<colors.length; i++) {
			int boxTop = (int)Math.round( top + i*boxHeight);
			drawBox(g, left, boxTop, width, (int) Math.round(boxHeight), colors[colors.length-i-1]);
		}
		
		
		
		//Draw labels
		g.setFont(font);
		g.setColor(Color.black);
		g.drawString("" + StringUtils.format(max), left + width + 3, top + 10);
		g.drawString("" + StringUtils.format((max+min)/2), left + width + 3, top + (height/2) + 5);
		g.drawString("" + StringUtils.format(min), left + width + 3, top + height);
		
		g.setColor(Color.DARK_GRAY);
		g.drawRect(left, top, width, height);
	}
	
	private void drawBox(Graphics2D g, int x, int y, int width, int height, Color col) {
		g.setColor(col);
		g.fillRect(x, y, width, height);
	}
}
