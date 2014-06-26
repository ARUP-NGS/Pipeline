package gui.figure.heatMapFigure;

import gui.figure.Figure;
import gui.figure.FigureElement;
import gui.figure.StringUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
/**
 * A type of figure that dispays a HeatMapElement and, perhaps, a color bar.
 * 
 * @author brendan
 *
 */
public class HeatMapElement extends FigureElement {
	
	
	protected int xAxisHeight = 5;
	protected int yAxisWidth = 5; 
	private double xMin = 0;
	private double yMin = 0;
	private double xMax = 1000;
	private double yMax = 100;
	
	protected double[][] heats = null;
	
	private Font labelFont = new Font("Sans",Font.PLAIN, 11);
	private Color gridColor = Color.LIGHT_GRAY;
	private Color labelColor = Color.black;
	
	protected Color coldColor = Color.blue;
	protected Color hotColor = Color.red;
	
	protected double coldTemp = 0.0; //The temperature that corresponds to the cold color
	protected double hotTemp = 1.0; //Temp that corresponds to the hot color
	protected int colorBinCount = 20;
	protected Color[] colors = new Color[colorBinCount];
	
	private DecimalFormat intFormatter = new DecimalFormat("0"); //Used to format axis labels	
	private DecimalFormat bigFormatter = new DecimalFormat("#.#"); //Used to format axis labels
	private DecimalFormat smallFormatter = new DecimalFormat("0.0##"); //Used to format axis labels
	
	public HeatMapElement(Figure parentFig) {
		super(parentFig);
	}
	
	public HeatMapElement(Figure parentFig, double[][] heats) {
		this(parentFig);
		this.heats = heats;
		createColorArray();
	}

	private void createColorArray() {
		colors = new Color[colorBinCount];
		double dr = (double)(hotColor.getRed()  - coldColor.getRed())/(double)colors.length;
		double dg = (double)(hotColor.getGreen()  - coldColor.getGreen())/(double)colors.length;
		double db = (double)(hotColor.getBlue()  - coldColor.getBlue())/(double)colors.length;
		
		for(int i=0; i<colors.length; i++) {
			//colors[i] = new Color( (int)Math.round(coldColor.getRed() + dr*i), (int)Math.round(coldColor.getGreen() + dg*i),  (int)Math.round(coldColor.getBlue() + db*i), 150);
			float x = (float) i / (float)(colors.length-1);
			colors[i] = new Color( Color.HSBtoRGB(0.5f+x/2.0f, 1f, 0.8f));
		}
		//System.out.println("Recreating colors, last color is : " + colors[colors.length-1]);
	}

	public void setData(double[][] heats) {
		this.heats = heats;
	}
	
	public int getNumRows() {
		if (heats == null)
			return 0;
		return heats.length;
	}
	
	public int getNumCols() {
		if (heats == null)
			return 0;
		return heats[0].length;
	}
	
	@Override
	public void paint(Graphics2D g) {
		if (heats == null) {
			g.drawString("Heats is null", (int)Math.floor(xFactor/2), (int)Math.round(yFactor/2));
			return;
		}


		drawGrid(g);
		
		for(int row=0; row<getNumRows(); row++) {
			for(int col=0; col<getNumCols(); col++) {
				if (heats[row][col]>0)
				drawBox(g, row, col, heats[row][col]);
			}
		}
	}
	
	
	public void setYMax(double yMax) {
		this.yMax = yMax;
	}
	
	public void setXMax(double xMax) {
		this.xMax = xMax;
	}
	
	public void setYMin(double yMin) {
		this.yMin = yMin;
	}
	
	public void setXMin(double xMin) {
		this.xMin = xMin;
	}
	
	/**
	 * Draw the background grid
	 * @param g
	 */
	private void drawGrid(Graphics2D g) {
		int left = (int)Math.round(bounds.x*xFactor);
		int top = (int)Math.round(bounds.y*yFactor);
		int width = (int)Math.round(bounds.width*xFactor+1);
		int height = (int)Math.round(bounds.height*yFactor+1);
		
		g.setFont(labelFont);
		
		g.setColor(Color.white);
		g.fillRect(left, top, width, height);
		
		g.setColor(Color.LIGHT_GRAY);
		double xStep = width / 4.0;
		double yStep = height / 4.0;
		
		//Vertical grid lines and x-labels
		int count = 0;
		for(double i=left; i<=left+width; i+=xStep) {
			g.setColor(gridColor);
			g.drawLine((int)Math.round(i), top,(int)Math.round(i), top+height);
			
			g.setColor(labelColor);
			double val = xMin + (xMax - xMin)/4.0* count;
			if (i==left)
				val = xMin;
			if (i==left+width)
				val = xMax;
			String str = StringUtils.format(val,3);
			
			int strWidth = g.getFontMetrics().stringWidth(str);
			g.drawString(str, (int)Math.round(i - strWidth/2), (int)Math.round((bounds.y+bounds.height)*yFactor+12));
			count++;
		}
		
		//Horizontal grid lines and y-labels
		for(double i=top; i<=top+height; i+=yStep) {
			g.setColor(gridColor);
			g.drawLine(left, (int)Math.round(i), left+width, (int)Math.round(i));
			
			g.setColor(labelColor);
			double val = pixelYToDataY(i);
			if (i==top)
				val = yMax;
			if (i==top+height)
				val = yMin;
			
			String str = format(val);
			int strWidth = g.getFontMetrics().stringWidth(str);
			g.drawString(str, left-strWidth-2, (int)Math.round(i+4));
		}
		
		
	}

	private String format(double val) {
		if (val==0)
			return "0";
		
		String str;
		if (val<10)
			str = smallFormatter.format(val);
		else {
			if (val > 99)
				str = intFormatter.format(Math.round(val));
			else
				str = bigFormatter.format(Math.round(val));
		}
		
		return str;
	}
	
	public double pixelXToDataX(double px) {
		return figXToDataX(px/xFactor);
	}
	
	
	/**
	 * Convert a x-value if figure coords (0..1) to one in "data" coords, which 
	 * are specified by the user and defined in ymin..ymax
	 * @param y
	 * @return
	 */
	public double figXToDataX(double x) {
		x -= bounds.x;
		x /= bounds.width;
		return x*(xMax-xMin);
	}
	
	/**
	 * Convert a pixel value (which is unbounded) into one in data coords. This doesn't do
	 * any error checking to see if the value is in bounds, or anything else
	 * @param py
	 * @return
	 */
	public double pixelYToDataY(double py) {
		return figYToDataY(py/yFactor);
	}
	
	/**
	 * Convert a y-value if figure coords (0..1) to one in "data" coords, which 
	 * are specified by the user and defined in ymin..ymax
	 * @param y
	 * @return
	 */
	public double figYToDataY(double y) {
		y -= bounds.y;
		y /= bounds.height;
		return (1-y)*(yMax-yMin);
	}
	
	
	private void drawBox(Graphics2D g, int row, int col, double heat) {
		
		double boxWidth = bounds.width/(double)getNumRows() * xFactor;
		double boxHeight = bounds.height/(double)getNumCols() * yFactor;
		int boxTop = (int)Math.round( (bounds.height+bounds.y)*yFactor +bounds.y*yFactor-boxHeight- (bounds.y*yFactor + boxHeight * col));
		int boxLeft = (int)Math.round(bounds.x*xFactor + boxWidth * row);
		
		g.setColor(colorForHeat(heat));
		g.fillRect(boxLeft, boxTop, (int)Math.round(boxWidth), (int)Math.round(boxHeight));
	}

	private Color colorForHeat(double heat) {
		double val = (double)(heat-coldTemp)/(double)(hotTemp - coldTemp);
		int bin = (int)Math.round(Math.max(val*colors.length, 0.0));
		if (bin > colors.length-1)
			bin = colors.length-1;
		return colors[bin];
	}

	/**
	 * Sets the temperature that corresponds to the 'hot' color
	 * @param max
	 */
	public void setHeatMax(double max) {
		this.hotTemp = max;
		createColorArray();
	}

	public void setHeatMin(double min) {
		this.coldTemp = min;
		createColorArray();
	}
	
	public void setColdColor(Color coldColor) {
		this.coldColor = coldColor;
		createColorArray();
	}
	
	public void setHotColor(Color hotColor) {
		this.hotColor = hotColor;
		createColorArray();
	}

	public Color[] getColors() {	
		return colors;
	}

	public double[][] getData() {
		return heats;
	}
}

