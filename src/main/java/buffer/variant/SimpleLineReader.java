package buffer.variant;

import java.io.File;
import java.io.IOException;

/**
 * Just reads contig and start position - that's it
 * @author brendan
 *
 */
public class SimpleLineReader extends CSVLineReader {

	public SimpleLineReader(File csvFile) throws IOException {
		super(csvFile);
	}

	
	@Override
	public VariantRec toVariantRec() {
		if (currentLine == null)
			return null;
		
		String[] toks = currentLine.split("\t");
		
		String contig = getContig(toks);
		Integer start = Integer.parseInt(toks[1]);
		String ref = toks[3];
		String alt = toks[4];
		
		VariantRec rec = new VariantRec(contig, start, start+ref.length(), ref, alt, 100.0, true);

			
		return rec;
	}
}
