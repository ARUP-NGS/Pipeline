package operator.variant;

/**
 * Retrieves COSMIC IDs from COSMIC coding vcf
 * MUST be tabix-compressed and indexed
 * @author daniel
 * Borrowing from David F's HaloplexDB
 */

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.broad.tribble.readers.TabixReader;

import pipeline.Pipeline;

public class COSMICCodingDB {
	private File dbFile;
	private TabixReader reader = null;
    public COSMICCodingDB(File file) throws IOException {
		if (! file.exists()) {
			throw new IOException("File " + dbFile.getAbsolutePath() + " does not exist");
		}
		dbFile = file;
		
		reader = new TabixReader(dbFile.getAbsolutePath());
	}
    
    public String[] getInfoForPosition(String contig, int pos) throws IOException {
		String queryStr = contig + ":" + pos + "-" + (pos);
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);

		try {
			TabixReader.Iterator iter = reader.query(queryStr);
			if(iter != null) {
					String str = iter.next();
					while(str != null) {
						String[] toks = str.split("\t");
						Integer VarPos = Integer.parseInt(toks[1]);
						if(pos == VarPos) {
							String cosmicID = toks[2];
							String cosmicCount = toks[7].split(";")[4];
							return new String[]{cosmicID,cosmicCount};
						}
						else {
							logger.info("Failed to make it match. " + pos + " is the position for our input variant rec, while the position in the VCF is " + VarPos );
						}
						str = iter.next();
					}
			}
			else {
			}
		}
		catch (RuntimeException rex) {
			logger.info("Runtime Exception is happening - watch out!");
			//Bad contigs will cause an array out-of-bounds exception to be thrown by
			//the tabix reader. There's not much we can do about this since the methods
			//are private... right now we just ignore it and skip this variant
		}
		
		
		
		return null;
	}
}