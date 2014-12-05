package buffer.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import util.vcfParser.VCFParser.GTType;

public class IonTorrentXLSParser extends CSVLineReader {

	private boolean headerHasBeenRead = false;

	
	protected void readHeader() throws IOException {
		//Try to get source file from attributes
		if (sourceFile == null) {
			String sourceFilePath = this.getAttribute("filename");
			if (sourceFilePath == null) {
				throw new IllegalArgumentException("No file specified");
			}
			sourceFile = new File(this.getProjectHome() + "/" + sourceFilePath);
		}
		
		reader = new BufferedReader(new FileReader(sourceFile));
		currentLine = reader.readLine(); //Skip first line
		currentLine = reader.readLine();
		
			String[] rawToks = currentLine.split("\t");
			List<String> tokList = new ArrayList<String>();
			for(int i=0; i<rawToks.length; i++)
				if (rawToks[i].trim().length()>0) {
					tokList.add(rawToks[i].trim());
				}
			headerToks = tokList.toArray(new String[]{});
			for(int i=0; i<tokList.size(); i++)
				headerMap.put(tokList.get(i).trim(), i);
			currentLine = reader.readLine();
			
		headerHasBeenRead = true;
	}
		
	@Override
	public VariantRec toVariantRec()  {
		if (! headerHasBeenRead) {
			try {
				readHeader();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		if (currentLine == null)
			return null;
		
		String[] toks = currentLine.split("\t");
		VariantRec rec = null;
		
		if (toks.length < 18) {
			System.err.println("ERROR: could not parse variant from file : " + sourceFile.getName() + " line : \n " + currentLine + " (at least 8 columns required)");
			return null;
		}
		
		try {
			
			String contig = toks[13];
			Integer start = Integer.parseInt(toks[14]);
			String ref = toks[15];
			String alt = toks[16];
			
			Double qual = 1.0;
			Double depth = Double.parseDouble(toks[4]);
			
			
			//boolean isHet = false;


			if (alt.length() != ref.length()) {
				//Remove initial characters if they are equal and add one to start position
				if (alt.charAt(0) == ref.charAt(0)) {
					alt = alt.substring(1);
					ref = ref.substring(1);
					if (alt.length()==0)
						alt = "-";
					if (ref.length()==0)
						ref = "-";
					start++;
				}

			}

			rec = new VariantRec(contig, start, start+ref.length(), ref, alt, qual, GTType.UNKNOWN);
			rec.addProperty(VariantRec.DEPTH, depth);

			/**
			 * 			0	gene
			1	exon.number
			2	cdot
			3	pdot
			4	Coverage
			5	Var.Freq
			6	variant.type
			7	exon.function
			8	HotSpot.ID
			9	pop.freq
			10	exomes5400.frequency
			11	rsnum
			12	hgmd.hit
			13	chrom
			14	pos
			15	ref
			16	alt
			17	nm.number

			 */
			
			rec.addProperty(VariantRec.VAR_DEPTH, Double.parseDouble(toks[5])/100.0 * depth );
			rec.addAnnotation(VariantRec.VARIANT_TYPE, toks[6]);
			rec.addAnnotation(VariantRec.EXON_FUNCTION, toks[7]);
			rec.addAnnotation(VariantRec.EXON_NUMBER, toks[1]);
			rec.addAnnotation(VariantRec.GENE_NAME, toks[0]);
			rec.addAnnotation(VariantRec.NM_NUMBER, toks[17]);
			rec.addAnnotation(VariantRec.HOTSPOT_ID, toks[8]);
			rec.addAnnotation(VariantRec.RSNUM, toks[11]);
			rec.addAnnotation(VariantRec.HGMD_HIT, toks[12]);
			try {
				rec.addProperty(VariantRec.POP_FREQUENCY, Double.parseDouble(toks[9]));
			}
			catch (NumberFormatException nfe) {
				rec.addProperty(VariantRec.POP_FREQUENCY, 0.0);
			}
			
			try {
				rec.addProperty(VariantRec.EXOMES_FREQ, Double.parseDouble(toks[10]));
			}
			catch (NumberFormatException nfe) {
				rec.addProperty(VariantRec.POP_FREQUENCY, 0.0);
			}
			rec.addProperty(VariantRec.EXOMES_FREQ, Double.parseDouble(toks[10]));
			


			if (sourceFile != null)
				rec.addAnnotation(VariantRec.SOURCE, sourceFile.getName());

		}
		catch (NumberFormatException nfe) {
			System.err.println("ERROR: could not parse variant from file : " + sourceFile.getName() + " line : \n " + currentLine);

		}
		return rec;
	}



}
