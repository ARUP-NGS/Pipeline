package test;

import gui.figure.FigureFactory;
import gui.figure.series.XYSeriesFigure;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestFigure {

	public static void main(String[] args) {
		
		FigureFactory figFactory = new FigureFactory();
		
		List<Point2D> rawCov =new ArrayList<Point2D>();
		
		for(int i=0; i<100; i++) {
			rawCov.add( new Point2D.Double(i, Math.exp(-i/50.0)+Math.random()/3));
		}
		
		
		List<Point2D> finalCov =new ArrayList<Point2D>();
		
		for(int i=0; i<100; i++) {
			finalCov.add( new Point2D.Double(i, Math.exp(-i/50.0)+Math.random()/2));
		}
		
		List<List<Point2D>> data = new ArrayList<List<Point2D>>();
		data.add(rawCov);
		data.add(finalCov);
		
		List<String> names = new ArrayList<String>();
		names.add("Raw coverage");
		names.add("Final coverage");
		
		List<Color> colors = new ArrayList<Color>();
		colors.add(Color.blue);
		colors.add(Color.green);
		
		String figStr =  "testcoverage1.png";
		String figFullPath = figStr;
		File destFile = new File(figFullPath);
		XYSeriesFigure fig = figFactory.createFigure("Coverage", "Proportion of bases", data, names, colors); 
		try {
			figFactory.saveFigure(new Dimension(500, 500), fig, destFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		List<Point2D> rawCov1 =new ArrayList<Point2D>();
		
		for(int i=0; i<30; i++) {
			rawCov1.add( new Point2D.Double(i, Math.exp(-i/50.0)+Math.random()/3));
		}
		
		
		List<Point2D> finalCov1 =new ArrayList<Point2D>();
		
		for(int i=0; i<30; i++) {
			finalCov1.add( new Point2D.Double(i, Math.exp(-i/50.0)+Math.random()/2));
		}
		
		figStr =  "testcoverage2.png";
		figFullPath = figStr;
		destFile = new File(figFullPath);
		XYSeriesFigure fig1 = figFactory.createFigure("Schmoverage", "Proportion of bases", data, names, colors); 
		try {
			figFactory.saveFigure(new Dimension(500, 500), fig1, destFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
