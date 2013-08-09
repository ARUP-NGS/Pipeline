package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import math.Histogram;

import util.flatFilesReader.DBNSFPReader;

public class ValConverter {

	public static Double parse(String str) {
		try {
			Double val = Double.parseDouble(str);
			return val;
		}
		catch (NumberFormatException nfe) {
			return Double.NaN;
		}
	}
	
	public static void main(String[] args) {
		
		Histogram gerpHist = new Histogram(-10, 10, 20);
		Histogram siftHist = new Histogram(0, 1, 20);
		Histogram ppHist = new Histogram(0, 1, 20);
		Histogram mtHist = new Histogram(0, 1, 20);
		Histogram phylopHist = new Histogram(-10, 10, 20);
		Histogram siphyHist = new Histogram(0, 10, 20);
		Histogram lrtHist = new Histogram(0, 1, 20);
		
		double gerpMean = 3.053;
		double gerpStdev = 3.106;
		
		double siftMean = 0.226;
		double siftSTtdev = 0.2923;
		
		double ppMean = 0.584;
		double ppStdev = 0.4323;
		
		double mtMean = 0.5604;
		double mtStdev = 0.4318;
		
		double phylopMean = 1.2932;
		double phylopStdev = 1.1921;
		
		double siphyMean = 11.1355;
		double siphyStdev = 5.1848;
		
		double lrtMean = 0.08391;
		double lrtStdev = 0.20298;
		
		for(int i=1; i<24; i++) {
			String contig = "" + i;
			if (i==23)
				contig = "X";
			if (i==24)
				contig = "Y";
			try {
				System.err.println("Parsing contig : "+ contig);
				BufferedReader reader = new BufferedReader(new FileReader("/home/brendan/resources/dbNSFP2.0/dbNSFP2.0b2_variant.chr" + contig));
				//BufferedWriter writer = new BufferedWriter(new FileWriter("/home/brendan/resources/dbNSFP2.0/dbNSFP2.0b2_variant-trans.chr" + contig));
				
				String line = reader.readLine();
				line = reader.readLine();
				while(line != null) {
					String[] toks = line.split("\t");

					Double gerp = parse( toks[DBNSFPReader.GERP] );
					Double sift = parse( toks[DBNSFPReader.SIFT] );
					Double pp = parse( toks[DBNSFPReader.PP] );
					Double mt = parse( toks[DBNSFPReader.MT] );
					Double phylop = parse( toks[DBNSFPReader.PHYLOP] );
					Double siphy = parse( toks[DBNSFPReader.SIPHY] );
					Double lrt = parse( toks[DBNSFPReader.LRT] );

					if (!Double.isNaN(gerp))
						gerpHist.addValue(gerp);

					if (!Double.isNaN(sift))
						siftHist.addValue(sift);

					if (!Double.isNaN(pp))
						ppHist.addValue(pp);

					if (!Double.isNaN(mt))
						mtHist.addValue(mt);

					if (!Double.isNaN(phylop))
						phylopHist.addValue(phylop);

					if (!Double.isNaN(siphy))
						siphyHist.addValue(siphy);

					if (!Double.isNaN(lrt))
						lrtHist.addValue(lrt);

					
					line = reader.readLine();

				}// while reading lines
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("Gerp : " + gerpHist.toString() );
		System.out.println("sift : " + siftHist.toString());
		System.out.println("pp : " + ppHist.toString());
		System.out.println("mt : " + mtHist.toString());
		System.out.println("phylop : " + phylopHist.toString());
		System.out.println("siphy : " + siphyHist.toString());
		System.out.println("lrt : " + lrtHist.toString());
	
		
		
		
	}
	
}
