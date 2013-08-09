package buffer.variant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import util.VCFLineParser;

/**
 * A somewhat more flexible and generic vcf-parsing line reader
 * @author brendan
 *
 */
public class GenericVCFParser extends VCFLineParser {

	public GenericVCFParser(File file) throws IOException {
		super(file);
	}

	public GenericVCFParser(InputStream inputStream) throws IOException {
		super(inputStream);
	}
	
	@Override
	public VariantRec toVariantRec() {
		if (getCurrentLine() == null || getCurrentLine().trim().length()==0)
			return null;

		VariantRec rec = null;
		try {
			String contig = getContig();
			if (contig == null)
				return null;

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

			String qualStr = lineToks[5];
			Double quality = 1e6;
			try {
				quality = Double.parseDouble(qualStr);
			}
			catch (NumberFormatException nfe) {
				//no worries, quality will be at default
			}
			
			Boolean hetero = isHetero();
								
			rec = new VariantRec(contig, start, end,  ref, alt, quality, hetero);
			Integer depth = getDepth();
			if (depth != null)
				rec.addProperty(VariantRec.DEPTH, new Double(depth));
			else 
				rec.addProperty(VariantRec.DEPTH, null);

		}
		catch (Exception ex) {
			System.err.println("ERROR: could not parse variant from line : " + getCurrentLine() + "\n Exception: " + ex.getMessage());
			return null;
		}
		return rec;
	}
	
}
