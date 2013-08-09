package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import math.Histogram;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.optimization.direct.CMAESOptimizer;

import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;

public class Optimizer implements MultivariateFunction {

	//static double[][] practiceA = new double[][]{{0.8, 0.022, 0.5}, {0.82, 0.1, 0.4}, {0.77, 0.05, 0.2}, {0.95, 0.01, 0.3}};
	//static double[][] practiceB = new double[][]{{0.1, 0.89, 0.4},{0.1, 0.915, 0.5},  {0.01, 0.8, 0.2},  {0.11, 0.9, 0.33}};
	
	List<double[]> practiceA;
	List<double[]> practiceB;
	
	Histogram histogramA = null;
	Histogram histogramB = null;
	

	RandomEngine rng = new MersenneTwister();
	Normal sanity = new Normal(0, 50, rng); //Just used to calculate normal pdf
	
	/**
	 * Return the probability that a variant with the given scores is in group A
	 * @param vec
	 * @return
	 */
	public static double sum(double[] vec, double[] weights) {
		double sum = 0;
		for(int i=0; i<vec.length; i++) {
			sum += vec[i]*weights[i];
		}
		
		//ALL TWO-WAY INTERACTIONS!
//		int index = vec.length;
//		for(int i=0; i<vec.length; i++) {
//			for(int j=i+1; j<vec.length; j++) {
//				sum += weights[index] * vec[i] * vec[j];
//				index++;
//			}
//		}

//		sum += weights[11] * vec[interactor1] * vec[interactor2];

		return sum;
	}
	
	public void loadDataA(File file) throws IOException {
		practiceA = new ArrayList<double[]>(1024);
		loadData(file, practiceA);
		System.out.println("Loaded " + practiceA.size() + " variants from " + file.getName());
	}

	public void loadDataB(File file) throws IOException {
		practiceB = new ArrayList<double[]>(1024);
		loadData(file, practiceB);
		System.out.println("Loaded " + practiceB.size() + " variants from " + file.getName());
	}
	
	private void loadData(File file, List<double[]> vals) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		
		double gerpMean = 3.053;
		double gerpStdev = 3.106;
		
		double siftMean = 0.226;
		double siftStdev = 0.2923;
		
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
		
		while(line != null) {
			String[] toks= line.split("\t");
			try {
				Double sift = Double.parseDouble(toks[33]);
				Double pp = Double.parseDouble(toks[34]);
				Double phylop = Double.parseDouble(toks[35]);
				Double mt = Double.parseDouble(toks[36]);
				Double gerp = Double.parseDouble(toks[37]);
				Double lrt = Double.parseDouble(toks[38]);
				Double siphy = Double.parseDouble(toks[39]);
				double[] arr = new double[7];
				arr[0] = (sift-siftMean)/siftStdev;
				arr[1] = (pp-ppMean)/ppStdev;
				arr[2] = (phylop-phylopMean)/phylopStdev;
				arr[3] = (mt-mtMean)/mtStdev;
				arr[4] = (gerp-gerpMean)/gerpStdev;
				arr[5] = (lrt-lrtMean)/lrtStdev;
				arr[6] = (siphy-siphyMean)/siphyStdev;
				vals.add(arr);
			}
			catch (NumberFormatException nfe) {
				//dont stress it
			}
			
			line = reader.readLine();
		}
	}
	
	public double classify(double[] weights) {
		Histogram histoA = new Histogram(-50, 50, 50);
		Histogram histoB = new Histogram(-50, 50, 50);
		
		double penalty = 0;
		
		for(int i=0; i<practiceA.size(); i++) {
			double val = sum(practiceA.get(i), weights);
			
			histoA.addValue(val);
		}
		
		for(int i=0; i<practiceB.size(); i++) {
			double val = sum(practiceB.get(i), weights);
			histoB.addValue(val);
		}
		
		for(int i=0; i<weights.length; i++) {
			penalty += 1.0 / (sanity.pdf(weights[i])+1);
		}
		
		double meanA = histoA.getMean();
		double meanB = histoB.getMean();
		double stdEvA = histoA.getStdev();
		double stdEvB = histoB.getStdev();
		
		double res = (stdEvA + stdEvB+0.1)/(Math.abs(meanA - meanB) + 0.1) + penalty ;
		//double res = computeHistoOverlap(histoA, histoB);
		
		return res;
	}

	
	private double computeHistoOverlap(Histogram histoA, Histogram histoB) {
		double sum = 0;
		for(int i=0; i<histoA.getBinCount(); i++) {
			sum += histoA.getFreq(i)*histoB.getFreq(i);
		}
		
		sum += histoA.getLessThanMin() / histoA.getCount() *  histoB.getLessThanMin() / histoB.getCount(); 
		sum += histoA.getMoreThanMax() / histoA.getCount() *  histoB.getMoreThanMax() / histoB.getCount();
		
		return sum /(double) histoA.getBinCount();
	}

	@Override
	public double value(double[] point) {
		return classify(point);
	}
	
	public void emit(double[] w) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("/home/brendan/oldhome/hgmd_test/hgmdvals.csv"));
		System.out.println("Disease variants");
		for(int i=0; i<practiceA.size(); i++) {
			writer.write(sum(practiceA.get(i), w) + "\n");
		}
		writer.close();
		
		writer = new BufferedWriter(new FileWriter("/home/brendan/oldhome/hgmd_test/esp5400.csv"));
		for(int i=0; i<practiceB.size(); i++) {
			writer.write(sum(practiceB.get(i), w) + "\n");
		}
		writer.close();
	}
	
	public void generateHistograms(double[] weights) {
		histogramA = new Histogram(-50, 50, 50);
		histogramB = new Histogram(-50, 50, 50);
		
		
		for(int i=0; i<practiceA.size(); i++) {
			double val = sum(practiceA.get(i), weights);
			histogramA.addValue(val);
		}
		
		for(int i=0; i<practiceB.size(); i++) {
			double val = sum(practiceB.get(i), weights);
			histogramB.addValue(val);
		}
	}

	public Histogram getHistoA(double[] w) {
		generateHistograms(w);
		return histogramA;
	}
	
	public Histogram getHistoB(double[] w) {
		generateHistograms(w);
		return histogramB;
	}
	
	/**
	 * The relative probability that the given variant came from A
	 * @param histA
	 * @param histB
	 * @param values
	 * @param weights
	 * @return
	 */
	public double testA(double[] values, double[] weights) {
		
		double val = sum(values, weights);
		int bin = histogramA.getBin(val);
		
		double probA = histogramA.getFreq(bin);
		double probB = histogramB.getFreq(bin);
		return probA / (probA + probB);
	}
	
	
	
	public static void main(String[] args) {
		CMAESOptimizer optim = new CMAESOptimizer();
		//SimplexOptimizer optim = new SimplexOptimizer();
		//optim.setSimplex(new NelderMeadSimplex(12));
		
		double[] bestWeights = null;
		double bestVal = 1000.0;
		
		//double[] startVals = new double[21];
		double[] startVals = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
		
		DecimalFormat formatter = new DecimalFormat("#0.0#");
		
		RandomEngine rng = new MersenneTwister();
		Uniform uni = new Uniform(rng);
		
		try {
			Optimizer func = new Optimizer();
			//func.loadDataA(new File("/home/brendan/hgmd_test/hgmd.analysis.csv"));
			//func.loadDataB(new File("/home/brendan/hgmd_test/highfreqs.0.1.analysis.csv"));
	
			func.loadDataA(new File("/home/brendan/oldhome/hgmd_test/hgmd.dmonly.nonans.csv")); //Original training set
			func.loadDataB(new File("/home/brendan/oldhome/hgmd_test/tgk.nonans.csv"));
			
			
			
			func.emit(new double[]{-2.22,4.98,1.52,11.29,-0.485,-0.22,2.68}); //best so far, basically
			//func.emit(new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}); 

			System.exit(0);
			
			for(int i=0; i<1000; i++) {
				PointValuePair res = optim.optimize(100000, func, GoalType.MINIMIZE, startVals);
				double[] sol = res.getPoint();
				System.out.print(i + ":\t");
				for(int j=0; j<sol.length; j++) {
					System.out.print(sol[j] + "\t");
				}
				System.out.println(" sum : " + res.getValue());
				
				if ((!res.getValue().isInfinite()) && res.getValue() < bestVal) {
					bestWeights = res.getPoint();
					bestVal = res.getValue();
				}
				System.out.print("Best so far: \t");
				for(int j=0; j<bestWeights.length; j++) {
					System.out.print(formatter.format(bestWeights[j]) + "\t");
					
				}
				System.out.println(" sum : " + bestVal);
				Histogram a = func.getHistoA(bestWeights);
				Histogram b = func.getHistoB(bestWeights);
				System.out.println("Mean of A: "+ formatter.format(a.getMean()) + " stdev: " + formatter.format(a.getStdev()));
				System.out.println("Mean of B: "+ formatter.format(b.getMean()) + " stdev: " + formatter.format(b.getStdev()));
				
				System.arraycopy(bestWeights, 0, startVals, 0, bestWeights.length);
				for(int j=0; j<startVals.length; j++) {
					double mul = 5;
					if (i%10==0)
						mul = 25;
					startVals[j] += mul*(uni.nextDouble()-0.5);
				}
			}
			
			
			System.out.println("Best found : " );
			for(int j=0; j<bestWeights.length; j++) {
				System.out.println(bestWeights[j]);
			}
			System.out.println("Value : " + bestVal);
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	
	
}

