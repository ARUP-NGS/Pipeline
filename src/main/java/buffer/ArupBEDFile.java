package buffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import util.Interval;


public class ArupBEDFile extends BEDFile {

	public ArupBEDFile() {
	}
	
	public ArupBEDFile(File file) {
		super(file);
	}

	@Override
	public String getTypeStr() {
		return "ArupBEDFile";
	}
	
	/**
	 * Construct/initialize a map which allows us to easily look up which sites are in
	 * the intervals described by this BED file. If arg is true, strip chr from all contig labels
	 * @throws IOException, IllegalArgumentException 
	 */
	@Override
	public void buildIntervalsMap(boolean stripChr) throws IOException, IllegalArgumentException {
		BufferedReader reader = new BufferedReader(new FileReader(getAbsolutePath()));
		String line = reader.readLine();
		intervals = new HashMap<String, List<Interval>>();
		while (line != null) {
			if (line.startsWith("#")) {
				line = reader.readLine();
				continue;
			}
			if (line.length()>0) {
				String[] toks = line.split("\\s");
				String contig = toks[0];
				if (stripChr)
					contig = contig.replace("chr", "");
				
				if (toks.length < 3) {
					throw new IllegalArgumentException("ARUP Bed file line is malformed\n" 
							+ line + "\n" + "in bed file " + this.file.getName());
				}
				Integer begin;
				Integer end;
				try {
				toks[1] = toks[1].trim();
				toks[2] = toks[2].trim();
					begin = Integer.parseInt(toks[1])+1;
					end = Integer.parseInt(toks[2])+1;
				} catch (Exception e) {
					// REPLACES: Logger.getLogger(Pipeline.primaryLoggerName).warning("Skipping invalid line in bed file: " + line);
					e.printStackTrace();
					throw new IllegalArgumentException("Bed file appears to be malformed\n" + e);
				}
				if (toks.length < 4) {
					throw new IllegalArgumentException("ARUP Bed file has no 4th column for transcript ids on line \n" 
							+ line + "\n" + "in bed file " + this.file.getName());
				}

				
				
				String[] untrimmedTrs = toks[3].split("\\|");
				String[] transcripts = new String[untrimmedTrs.length];
				for (int i = 0; i < untrimmedTrs.length; i++) {
					transcripts[i] = untrimmedTrs[i].trim();
					if (transcripts[i].length() < 1) {
					throw new IllegalArgumentException("ARUP BED file transcript id appears to be blank in 4th column for line \n" 
							+ line + "\n" + "in bed file " + this.file.getName());
					}
				}

				String[] untrimmedGenes = toks[4].split("\\|");
				String[] genes = new String[untrimmedGenes.length];
				for (int i = 0; i < untrimmedGenes.length; i++) {
					genes[i] = untrimmedGenes[i].trim();
					if (genes[i].length() < 1) {
					throw new IllegalArgumentException("ARUP BED file gene name appears to be blank in 5th column for line \n" 
							+ line + "\n" + "in bed file " + this.file.getName());
					}
				}

				String[] untrimmedExons = toks[5].split("\\|");
				String[] exons = new String[untrimmedExons.length];
				for (int i = 0; i < untrimmedExons.length; i++) {
					if (untrimmedExons[i].trim().length() < 1) {
					throw new IllegalArgumentException("ARUP BED file gene name appears to be blank in 5th column for line \n" 
							+ line + "\n" + "in bed file " + this.file.getName());
					}
					else exons[i] = untrimmedExons[i].trim();
				}

				ARUPBedIntervalInfo intervalInfo = new ARUPBedIntervalInfo(genes, transcripts, exons);
				
				Interval interval = new Interval(begin, end, intervalInfo);

				List<Interval> contigIntervals = intervals.get(contig);
				if (contigIntervals == null) {
					contigIntervals = new ArrayList<Interval>(1024);
					intervals.put(contig, contigIntervals);
					//System.out.println("BED file adding contig: " + contig);
				}
				contigIntervals.add(interval);
			}
			line = reader.readLine();
		}
		
		reader.close();
		sortAllContigs();
	}
	
	/**
	 * Stores some information parsed from a single line of an ARUP BED file, including gene name, transcript list, and exon number 
	 * @author brendan
	 *
	 */
	public class ARUPBedIntervalInfo {

		public final String[] genes;
		public final String[] transcripts;
		public final String[] exons;
		
		public ARUPBedIntervalInfo(String[] genes, String[] transcripts, String[] exons) {
			this.genes = genes;
			this.transcripts = transcripts;
			this.exons = exons;
		}
	}
	
	
	/**
	 * Examines the given file and returns true if this looks like an ARUPBedFile
	 * @param file
	 * @return
	 * @throws IOException 
	 */
	public static boolean checkFormat(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		int maxLinesToTest = 5;
		int linesTested = 0;
		while(line != null && line.startsWith("#")) {
			line = reader.readLine();
		}
		
		boolean ok = true;
		while(line != null && linesTested < maxLinesToTest) {
			String[] toks = line.split("\t");
			if (toks.length < 7) {
				ok = false;
				break;
			}
			
			String tx = toks[4];
			String gene = toks[5];
			String exonNum = toks[6];
			
			/*
			if (tx.length()>0 && !(tx.startsWith("NM_") || tx.startsWith("NR_") || tx.startsWith("XM_"))) {
				ok = false;
				break;
			}
			
			if (exonNum.length()>0) {
				try {
					int ex = Integer.parseInt(exonNum);
				} catch (NumberFormatException nfe) {
					ok = false;
					break;
				}
			}
			*/
			
			// Use less restrictive versions of above till ArupBedFile format finalized
			if (tx.length()==0) {
				ok = false;
				break;
			}
					
			if (exonNum.length()==0) {
				ok = false;
				break;
			}

			linesTested++;
			line = reader.readLine();
		}
		
		
		reader.close();
		return ok;
	}
}
