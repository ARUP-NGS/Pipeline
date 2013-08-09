package fpClassifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import math.Histogram;

/**
 * Stores a list of variant frequencies by contig and site
 * @author brendan
 *
 */
public class FreqStore {

	Map<String, List<FreqPos>> freqData = new HashMap<String, List<FreqPos>>();
	private PosComparator posComparer = new PosComparator();
	private FreqPos qPos = new FreqPos(); //Used as query object for lookups
	
	public void readFromFile(File storeData) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(storeData));
		String line = reader.readLine();
		List<Double> freqs = new ArrayList<Double>();
		while (line != null) {
			if (line.startsWith("#") || line.trim().length()==0) {
				line = reader.readLine();
				continue;
			}
			
			String[] toks = line.split("\t");
			String contig = toks[0];
			Integer pos = Integer.parseInt(toks[1]);
			freqs.clear();
			FreqPos fp = new FreqPos();
			fp.pos = pos;
			for(int i=6; i<toks.length; i++) {
				Double freq = Double.parseDouble(toks[i]);
				if (freq > 0)
					freqs.add(freq);
			}
			
			double[] freqArray = new double[freqs.size()];
			for(int i=0; i<freqs.size(); i++)
				freqArray[i] = freqs.get(i);
			fp.freqs = freqArray;
			
			
			List<FreqPos> contigFPs = freqData.get(contig);
			if (contigFPs == null) {
				contigFPs = new ArrayList<FreqPos>(256);
				freqData.put(contig, contigFPs);
			}
			contigFPs.add(fp);
			
			line = reader.readLine();
		}
		
		//Now sort all contig lists 
		for(String contig : freqData.keySet()) {
			List<FreqPos> freqList = freqData.get(contig);
			Collections.sort(freqList, posComparer);
		}
		
	}
	
	/**
	 * Write the distribution of allele frequencies in the FreqDB to stdout
	 */
	public void emitFreqDistro() {
		Histogram hist = new Histogram(0, 1.0, 40);
		for(String contig : freqData.keySet()) {
			for(FreqPos fp : freqData.get(contig)) {
				for(int i=0; i<fp.freqs.length; i++)
					if (fp.freqs[i] > 0)
						hist.addValue(fp.freqs[i]);
			}
		}
		
		System.out.println(hist.toString());
	}
	
	public int getCount(String contig, int pos) {
		FreqPos fp = getFreqPos(contig, pos);
		if (fp == null)
			return 0;
		else
			return fp.freqs.length;		
	}
	
	public double[] getFreqs(String contig, int pos) {
		FreqPos fp = getFreqPos(contig, pos);
		if (fp == null)
			return null;
		else
			return fp.freqs;
	}
	
	public boolean hasEntry(String contig, int pos) {
		return getFreqPos(contig, pos)!=null;
	}
	
	/**
	 * Find the FreqPos object on the given contig at the given position, returns
	 * null if there is no object there
	 * @param contig
	 * @param pos
	 * @return
	 */
	public FreqPos getFreqPos(String contig, int pos) {
		List<FreqPos> freqs = freqData.get(contig);
		if (freqs == null)
			return null;
		else {
			qPos.pos = pos;
			int index = Collections.binarySearch(freqs, qPos, posComparer);
			if (index > -1) {
				return freqs.get(index);
			}
			else {
				return null;
			}
		}
	}
	
	class FreqPos {
		int pos;
		double[] freqs;
	}
	
	class PosComparator implements Comparator<FreqPos> {

		@Override
		public int compare(FreqPos fp0, FreqPos fp1) {
			return fp1.pos - fp0.pos;
		}
		
	}
}
