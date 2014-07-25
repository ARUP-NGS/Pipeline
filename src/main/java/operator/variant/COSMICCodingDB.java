package operator.variant;

/**
 * Retrieves COSMIC IDs from COSMIC coding vcf
 * MUST be tabix-compressed and indexed
 * @author daniel
 * Borrowing from David F's HaloplexDB
 */

import java.io.File;
import java.io.IOException;

import org.broad.tribble.readers.TabixReader;

public class COSMICCodingDB {
	private File dbFile;
	private TabixReader reader = null;
    public COSMICCodingDB(File file) throws IOException {
		if (! dbFile.exists()) {
			throw new IOException("File " + dbFile.getAbsolutePath() + " does not exist");
		}
		dbFile = file;
		
		reader = new TabixReader(dbFile.getAbsolutePath());
	}
    
    public String[] getInfoForPostion(String contig, int pos) throws IOException {
		String queryStr = contig + ":" + pos + "-" + (pos);
		
		try {
			TabixReader.Iterator iter = reader.query(queryStr);

			if(iter != null) {
					String str = iter.next();
					while(str != null) {
						String[] toks = str.split("\t");
						if(toks[0]!=contig) {
							str = iter.next();
							continue;
						}
						Integer VarPos = Integer.parseInt(toks[1]);
						
						if(pos == VarPos) {
							String cosmicID = toks[2];
							String cDot = toks[7].split(";")[2].split("CDS=")[-1];
							String pDot = toks[7].split(";")[3].split("AA=")[-1];
							String cosmicCount = toks[7].split(";")[4].split("CNT=")[-1];
							return new String[]{cosmicID,cDot,pDot,cosmicCount};
						}
						str = iter.next();
					}
			}
		}
		catch (RuntimeException rex) {
			//Bad contigs will cause an array out-of-bounds exception to be thrown by
			//the tabix reader. There's not much we can do about this since the methods
			//are private... right now we just ignore it and skip this variant
		}
		
		
		
		return null;
	}
}