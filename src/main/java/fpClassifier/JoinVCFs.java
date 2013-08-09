package fpClassifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import operator.variant.FPComputer;

import math.Histogram;

import buffer.CSVFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class JoinVCFs {

	public static VariantRec firstVariant(String contig, List<VariantPool> pools) {
		List< List<VariantRec>> vars = new ArrayList<List<VariantRec>>();
		for(VariantPool pool : pools) {
			List<VariantRec> varList = pool.getVariantsForContig(contig);
			vars.add(varList);
		}
		
		List<VariantRec> firstVars = vars.get(0);
		if (firstVars.size()==0) {
			return null;
		}
		VariantRec firstVariant = firstVars.get(0);
		for(List<VariantRec> varList : vars) {
			if (varList.size() == 0)
				continue;
			VariantRec testVar = varList.get(0);
			if (testVar.getStart() < firstVariant.getStart()) {
				firstVariant = testVar;
			}
		}
		
		return firstVariant;
	}
	
	public static VariantRec nextVariant(VariantRec thisVariant, List<VariantPool> pools) {
//		if (thisVariant.getStart() == 1049491) {
//			System.out.println("ouch");
//		}
		
		List<VariantRec> nextVars = new ArrayList<VariantRec>();
		for(VariantPool pool : pools) {
			VariantRec var = pool.nextVariant(thisVariant);
			nextVars.add(var);
		}
		
		//Now find which record comes next
		int startIndex = 0;
		VariantRec firstVar = nextVars.get(startIndex);
		while (firstVar == null && startIndex < nextVars.size()) {
			firstVar = nextVars.get(startIndex);
			startIndex++;
		}
		
		//All next vars are null, we're done
		if (firstVar == null)
			return null;
		
		for(int i=startIndex; i<nextVars.size(); i++) {
			VariantRec var = nextVars.get(i);
			if (var != null && var.getStart() < firstVar.getStart()) {
				firstVar = var;
			}
			
		}
		
		return firstVar;
	}
	
	

	private static void writeLineForVar(VariantRec var, List<VariantPool> pools, Histogram freqHist, Histogram countHist) {
		StringBuilder strBFirst = new StringBuilder();
		StringBuilder strBSecond = new StringBuilder();
		strBFirst.append(var.getContig() + "\t" + var.getStart() + "\t" + (var.getStart()+1) + "\t" + var.getRef() + "\t" + var.getAlt());
		
		int count = 0;
		List<Double> freqs = new ArrayList<Double>();
		
		for(VariantPool pool : pools) {
			FPComputer.computeFPForPool(pool);
			
			VariantRec qVar = pool.findRecordNoWarn(var.getContig(), var.getStart());
			if (qVar == null)
				strBSecond.append("\t0.0");
			else {
				//strBSecond.append("\t" + qVar.getProperty(VariantRec.DEPTH) + "\t" + qVar.getProperty(VariantRec.VAR_DEPTH));
				double varDepth = qVar.getProperty(VariantRec.VAR_DEPTH);
				double depth = qVar.getProperty(VariantRec.DEPTH);
				double freq = qVar.getProperty(VariantRec.VAR_DEPTH) / qVar.getProperty(VariantRec.DEPTH);
				
				
				Double fpScore = qVar.getProperty(VariantRec.FALSEPOS_PROB);
				Double fsScore = qVar.getProperty(VariantRec.FS_SCORE);
				String fpStr = "NA";
				if (fpScore != null)
					fpStr = "" + fpScore;
				String fsStr = "NA";
				if (fsScore != null)
					fsStr = "" + fsScore;
				
				//strBSecond.append("\t" + qVar.getProperty(VariantRec.DEPTH) + "\t" +  qVar.getProperty(VariantRec.VAR_DEPTH));
				strBSecond.append("\t" + varDepth + "," + depth + "," + fpStr + "," + fsStr);
				if (freqHist != null && qVar.getProperty(VariantRec.DEPTH) > 3) {
					freqs.add(freq);
					count++;
				}
			}
		}
		
		countHist.addValue(count);
		
		if (count > 3) {
			System.out.print(strBFirst);
			System.out.println("\t" + count + strBSecond);
			for(Double freq : freqs)
				freqHist.addValue(freq);
			
		}
	}
	
	public static void main(String[] args) {
		
//		args = new String[1];
//		args[0] = "/media/MORE_DATA/detect_fp/novel_csvs/NA12878.novel.csv";
//		args[1] = "/media/MORE_DATA/detect_fp/annotated_files/HHT15.novel.csv";
		
		StringBuilder header = new StringBuilder("#contig\tstart\tend\tref\talt\tcount");
		
		List<VariantPool> pools = new ArrayList<VariantPool>();
		for(int i=0; i<args.length; i++) {
			CSVFile csvFile = new CSVFile(new File(args[i]));
			//header.append("\t" + args[i] +".depth\t" + args[i] + ".vardepth");
			header.append("\t" + args[i] +".varFreq");
			try {
				
				VariantPool pool = new VariantPool(csvFile);
				pools.add(pool);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		Histogram afHist = new Histogram(0, 1.0, 50);
		Histogram countHist = new Histogram(0, 25, 25);
		
		System.out.println(header);
		for(String contig : pools.get(0).getContigs()) {
			VariantRec var = firstVariant(contig, pools);
			while (var != null) {
				writeLineForVar(var, pools, afHist, countHist);
				var = nextVariant(var, pools);
			}
		}
		
		System.err.println("Histogram of frequencies:\n" + afHist.toString());
		
		System.err.println("Total number of variants across all samples:" + countHist.getCount());
		System.err.println("Histogram of counts:\n" + countHist.toString());
		
		for(int i=0; i<afHist.getBinCount(); i++) {
			System.err.print( afHist.getFreq(i) + ",");
		}
		System.err.println();
	}
	
	
}
