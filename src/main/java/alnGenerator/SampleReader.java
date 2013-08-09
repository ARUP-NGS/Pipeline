package alnGenerator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * SampleReaders provide ordered access to the variants described in a VCF file. They're
 * meant to be used like iterators, but are associated with a VCFReader that stores additional
 * info regarding the VCF file
 * @author brendan
 *
 */
public class SampleReader {

	private VCFReader vcfReader;
	private BufferedReader reader;
	private String sampleName;
	private int phase;
	private int column;
	private String currentLine = null;
	
	public SampleReader(VCFReader vcfReader, String sampleName, int phase) throws IOException {
		this.vcfReader = vcfReader;
		this.sampleName = sampleName;
		if (phase != 0 && phase != 1) {
			throw new IllegalArgumentException("Invalid phase, must be zero or one (got " + phase + ")");
		}
		this.phase = phase;
		reader = new BufferedReader(new FileReader(vcfReader.getSourceFile()));
		column = vcfReader.getColumnForSample(sampleName);
		advanceToFirstVariant();
	}
	
	public int getPhase() {
		return phase;
	}

	public String getSampleName() {
		return sampleName;
	}
	
	/**
	 * Advance current line to the first non-comment line in the vcf file
	 * @throws IOException
	 */
	private void advanceToFirstVariant() throws IOException {
		currentLine = reader.readLine();
		while(currentLine != null && currentLine.startsWith("#"))
			currentLine = reader.readLine();
	}
	
	/**
	 * Advances the current line until it contains the first variant in the given
	 * contig after or equal to the given position
	 * @param contig
	 * @param pos
	 * @throws IOException 
	 */
	public void advanceTo(String contig, int pos) throws IOException {
		String[] toks = currentLine.split("\t");
		String curContig = toks[0];
		
		while (currentLine != null && (! curContig.equals(contig))) {
			currentLine = reader.readLine();
			if (currentLine == null) {
				throw new IllegalArgumentException("Could not find contig " + contig);
			}	
			toks = currentLine.split("\t");
			curContig = toks[0]; 
		}
		if (currentLine == null) {
			throw new IllegalArgumentException("Could not find contig " + contig);
		}
		
		String posStr = toks[1];
		Integer curPos = Integer.parseInt(posStr);
		while(currentLine != null && curPos < pos) {
			currentLine = reader.readLine();
			if (currentLine == null) {
				throw new IllegalArgumentException("Could not find position " + pos + " in contig " + curContig);
			}
			toks = currentLine.split("\t");
			curContig = toks[0];
			if (! curContig.equals(contig))
				throw new IllegalArgumentException("Could not find position " + pos + " in contig " + curContig);
			curPos = Integer.parseInt(toks[1]);
		}
		
	}
	
	public Variant getVariant() {
		if (currentLine == null) 
			return null;
		
		String[] toks = currentLine.split("\t");
		if (toks.length < 8)
			throw new IllegalArgumentException("Fewer than 8 columns, this file does not seem to be formatted correctly");
		if (toks.length < column)
			throw new IllegalArgumentException("Not enough columns at this line, only " + toks.length);
		
		String contig = toks[0].trim();
		Integer pos = Integer.parseInt( toks[1].trim() );
		String ref = toks[3];
		String altStr = toks[4];
		Double quality = Double.parseDouble(toks[5]);
		String[] altAlleles = altStr.split(",");
		
		String formatVals = toks[column];
		String[] formatToks = formatVals.split(":");
		
		String GTStr = formatToks[ vcfReader.getFormatCol("GT")];
		char gt0 = GTStr.charAt(0);
		char gt1 = GTStr.charAt(2);
		String alt0;
		String alt1;
		Integer gt0Col = Integer.parseInt(gt0 + "");
		Integer gt1Col = Integer.parseInt(gt1 + "");
		
		String primaryAlt = altAlleles[0];
		if (primaryAlt.length() != ref.length()) {
			//Remove initial characters if they are equal and add one to start position
			if (primaryAlt.charAt(0) == ref.charAt(0)) {
				primaryAlt = primaryAlt.substring(1);
				ref = ref.substring(1);
				if (primaryAlt.length()==0)
					primaryAlt = ProtoSequence.GAP;
				altAlleles[0] = primaryAlt;
				if (ref.length()==0)
					ref = ProtoSequence.GAP;
				pos++;
			}
			if (altAlleles.length > 1) {
				throw new IllegalArgumentException("OK, so there are multiple alts at this site, and one of them needs a reassigned position because it's in GaTK-style indel annotation. We can't handle this currently because there are really multiple variants at this position");
			}
		}
		
		if (gt0Col == 0)
			alt0 = ref;
		else
			alt0 = altAlleles[gt0Col-1];
		if (gt1Col == 0)
			alt1 = ref;
		else
			alt1 = altAlleles[gt1Col-1];
		
		
		String depthStr = null;
		Integer depthCol = vcfReader.getFormatCol("AD");
		Integer depth = 1;
		if (depthCol != null) { 
			depthStr = formatToks[ depthCol ];
			String[] depthVals = depthStr.split(",");
			int totDepth = 0;
			for(int i=0; i<depthVals.length; i++) {
				totDepth += Integer.parseInt( depthVals[i] );
			}
			depth = totDepth;
		}
		
		Variant var = new Variant(contig, pos, ref, alt0, alt1, quality, depth);
		return var;
	}
	
	/**
	 * Advance the current line to the next variant
	 * @return True if there is another variant to be read
	 * @throws IOException 
	 */
	public boolean advance() throws IOException {
		currentLine = reader.readLine();
		return currentLine != null;
	}
}
