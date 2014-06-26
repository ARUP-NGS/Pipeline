package buffer.variant;

import java.io.File;
import java.io.IOException;

import util.VCFLineParser;

public class TinyVCFParser extends VCFLineParser {

	public TinyVCFParser(File source) throws IOException {
		super(source);
	}
	
	/**
	 * Convert current line into a variant record
	 * @param stripChr If true, strip 'chr' from contig name, if false do not alter contig name
	 * @return A new variant record containing the information in this vcf line
	 */
	public VariantRec toVariantRec(boolean stripChr) {
		if (! isPrimed()) {
			try {
				primeForReading();
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage());
			}
		}
		
		if (getCurrentLine() == null || getCurrentLine().trim().length()==0)
			return null;
		else {
			
			VariantRec rec = null;
			try {
				String contig = getContig();
				if (contig == null)
					return null;
				if (stripChr)
					contig = contig.replace("chr", "");
				//System.out.println(currentLine);
				String ref = getRef();
				String alt = getAlt();
				int start = getStart();
				int end = ref.length();

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

					if (ref.equals("-"))
						end = start;
					else
						end = start + ref.length();
				}

				rec = new VariantStub(contig, start, end,  ref, alt, isHetero() );
				
			
			}
			catch (Exception ex) {
				System.err.println("ERROR: could not parse variant from line : " + getCurrentLine() + "\n Exception: " + ex.getCause() + " " + ex.getMessage());
				
				return null;
			}
			return rec;
		}
	}
}
