package gui.figure.heatMapFigure;

import gui.figure.Figure;
import gui.figure.TextElement;
import gui.figure.VerticalTextElement;

/**
 * A type of figure that dispays a HeatMapElement and a color key/legend, aka a colorbar.
 * 
 * @author brendan
 *
 */
public class HeatMapFigure extends Figure {

//Distances from the edges of the figure to the main element, in "figure" (0..1) coords
	protected double leftPadding = 0.1;
	protected double rightPadding = 0.15;
	protected double topPadding = 0.05;
	protected double bottomPadding = 0.1;
	
	protected HeatMapElement heatMapEl = null;
	protected ColorBarElement colorBar = null;
	protected TextElement xAxisLabel = null;
	protected VerticalTextElement yAxisLabel = null;
	private boolean showColorBar = true; //right now we always show the color bar
	
	
	public HeatMapFigure() {
		heatMapEl = new HeatMapElement(this);
		heatMapEl.setBounds(leftPadding, topPadding, 1.0-leftPadding-rightPadding, 1.0-topPadding-bottomPadding);
		heatMapEl.setCanConfigure(false);
		heatMapEl.setMobile(false);
		addElement(heatMapEl);
		
		colorBar = new ColorBarElement(this);
		colorBar.setBounds(0.88, 0.1, 0.05, 0.7);
		if (showColorBar)
			addElement(colorBar);
		
		xAxisLabel = new TextElement("X Axis", this);
		xAxisLabel.setPosition(0.45, 0.95);
		xAxisLabel.setMobile(true);
		addElement(xAxisLabel);
		
		yAxisLabel = new VerticalTextElement("Y Axis", this);
		yAxisLabel.setPosition(0.02, 0.3);
		yAxisLabel.setMobile(true);
		addElement(yAxisLabel);
	}
	
	
	public void setData(double[][] heats) {
		heatMapEl.setData(heats);
		colorBar.setColors(heatMapEl.getColors());
		repaint();
	}
	
	/**
	 * Sets the temperature that corresponds to the 'hot' color
	 * @param max
	 */
	public void setHeatMax(double max) {
		heatMapEl.setHeatMax(max);
		colorBar.setMaxValue(max);
		colorBar.setColors(heatMapEl.getColors());
		repaint();
	}

	/**
	 * Set the temp that corresponds to the 'cold' color
	 * @param min
	 */
	public void setHeatMin(double min) {
		heatMapEl.setHeatMin(min);
		colorBar.setMinValue(min);
		colorBar.setColors(heatMapEl.getColors());
		repaint();
	}
	
	public void setYMax(double yMax) {
		heatMapEl.setYMax(yMax);
	}
	
	public void setXMax(double xMax) {
		heatMapEl.setXMax(xMax);
	}
	
	public void setYMin(double yMin) {
		heatMapEl.setYMin(yMin);
	}
	
	public void setXMin(double xMin) {
		heatMapEl.setXMin(xMin);
	}
	
	/**
	 * Set the text of the x-axis (horizontal) label
	 * @param label
	 */
	public void setXAxisLabel(String label) {
		xAxisLabel.setText(label);
		repaint();
	}
	
	/**
	 * Set the text of the y-axis (vertical) label
	 * @param label
	 */
	public void setYAxisLabel(String label) {
		yAxisLabel.setText(label);
		repaint();
	}


	public double[][] getData() {
		return heatMapEl.getData();
	}
}
