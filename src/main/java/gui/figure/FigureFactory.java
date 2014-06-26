package gui.figure;

import gui.figure.heatMapFigure.HeatMapFigure;
import gui.figure.series.XYSeries;
import gui.figure.series.XYSeriesElement;
import gui.figure.series.XYSeriesFigure;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.CellRendererPane;

public class FigureFactory {

	
	/**
	 * Layout all components in the given container and all components in child
	 * containers
	 * @param c
	 */
	public static void layoutComponent(Component c) {
		synchronized (c.getTreeLock()) {
			c.doLayout();
			if (c instanceof Container)
				for (Component child : ((Container) c).getComponents())
					layoutComponent(child);
		}
	}
	
	/**
	 * Layout all components of the figure and render it onto a BufferedImage 
	 * @param size
	 * @param fig
	 * @return
	 */
	public static BufferedImage getFigureImage(final Dimension size, final Figure fig) {
		fig.setSize(size);
		fig.doLayout();
		layoutComponent(fig);
		
		BufferedImage img = new BufferedImage((int)size.getWidth()-1, (int)size.getHeight()-1,
                BufferedImage.TYPE_INT_RGB);
		
		final CellRendererPane crp = new CellRendererPane();
        crp.add(fig);
        crp.setSize(size);
        crp.validate();
        
        final Graphics g = img.createGraphics();
        
        g.setColor(fig.getBackground());
        g.fillRect(1, 1, fig.getBounds().width, fig.getBounds().height);
        
        crp.paintComponent(g, fig, fig, 0, 0, (int)size.getWidth(), (int)size.getHeight(), true);			
        
        return img; 
	}
	
	/**
	 * Save the figure as a .png image to the given destination. Overwrites existing files 
	 * @param size
	 * @param fig
	 * @param destinationFile
	 * @throws IOException
	 */
	public static void saveFigure(Dimension size, Figure fig, File destinationFile) throws IOException {
		BufferedImage image = getFigureImage(size, fig);
		ImageIO.write(image, "png", destinationFile);
	}

	
	public static HeatMapFigure createFigure(String xLabel, String yLabel,
			double[][] data) {
				
		HeatMapFigure fig = new HeatMapFigure();
		fig.setPreferredSize(new Dimension(500, 500));
		fig.setData(data);
		
		//Laboriously find maximum value
		double max = 0;
		for(int i=0; i<data.length; i++) {
			for(int j=0; j<data[0].length; j++) {
				if (data[i][j] > max) 
					max = data[i][j];
			}
		}
		fig.setHeatMax(max);
		
		fig.setXAxisLabel(xLabel);
		fig.setYAxisLabel(yLabel);
		fig.setXMax(data.length);
		fig.setYMax(data[0].length);
		
		return fig;
	}
	
	public static XYSeriesFigure createFigure(String xLabel, String yLabel,
			List< List<Point2D>> data, 
			List<String> seriesNames,
			List<Color> colors) {
		XYSeriesFigure fig = new XYSeriesFigure();
		Dimension size = new Dimension(500, 500);
		
		
		int count = 0;
		for(List<Point2D> seriesData : data) {
			XYSeriesElement el = fig.addDataSeries(new XYSeries(seriesData));
			el.setLineColor(colors.get(count));
			el.setName(seriesNames.get(count));
			el.setLineWidth(2f);
			count++;
		}
		
		fig.inferBoundsFromCurrentSeries();
		fig.setXLabel(xLabel);
		fig.setYLabel(yLabel);
		fig.setSize( size );
		return fig;
	}
	
	
	/**
	 * Create a simple figure with the given X and Y labels, the lists of series as X-Y points,
	 * and the names and colors of the given series.
	 * @param xLabel
	 * @param yLabel
	 * @param data
	 * @param name
	 * @param color
	 * @return
	 */
	public static XYSeriesFigure createFigure(String xLabel, String yLabel,
								List<Point2D> data, 
								String name,
								Color color) {
		List<List<Point2D>> dataList = new ArrayList<List<Point2D>>();
		dataList.add(data);
		List<String> names = new ArrayList<String>();
		List<Color> colors = new ArrayList<Color>();
		names.add(name);
		colors.add(color);
		return createFigure(xLabel, yLabel, dataList, names, colors);
	}
	
	
	public static class SeriesProperties {
		String name;
		Color color = Color.blue;
		float width = 2f;
		String style = XYSeriesElement.LINES;
	}
	
	public static void main(String[] args) {	
		List< List<Point2D> > data = new ArrayList< List<Point2D>>();
//		data.add(Arrays.asList(new Point2D[]{
//				new Point2D.Double(0, 0.0),
//				new Point2D.Double(1, 0.01),
//				new Point2D.Double(2, 0.01),
//				new Point2D.Double(3, 0.012),
//				new Point2D.Double(4, 0.02),
//				new Point2D.Double(5, 0.064),
//				new Point2D.Double(6, 0.02),
//				new Point2D.Double(7, 0.018),
//				new Point2D.Double(8, 0.017),
//				new Point2D.Double(9, 0.012),
//				new Point2D.Double(10, 0.005)
//		}));
//		
//		
//		List<String> names = new ArrayList<String>();
//		names.add("My data series");
//	//	names.add("Another data series");
//		
//		List<Color> colors = new ArrayList<Color>();
//		colors.add(Color.green);
//	//	colors.add(Color.magenta);
//		
//		XYSeriesFigure fig = createFigure("X axis label", "Time", data, names, colors);
//		
//		File destFile = new File("savedImage.png");
//		
//		try {
//			saveFigure(new Dimension(1000, 1000), fig, destFile);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		BAMFile bam = new BAMFile(new File("/home/brendan/oldhome/tinytest/test.final.bam"));
//		BAMMetrics metrics = BamMetrics.computeBAMMetrics(bam);
//		Histogram[] histos = metrics.readPosQualHistos;
//		
//		double[][] heats = new double[histos.length][histos[0].getBinCount()];
//		for(int i=0; i<histos.length; i++) {
//			Histogram posHist = histos[i];
//			if (posHist != null)
//				System.arraycopy(posHist.getRawCounts(), 0, heats[i], 0, posHist.getRawCounts().length);
//		}
//		HeatMapFigure readPosFig = FigureFactory.createFigure("Read position", "Quality", heats);
//		try {
//			FigureFactory.saveFigure(new Dimension(500, 500), readPosFig, new File("fancynewfigure.png"));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		
		
	}
}
