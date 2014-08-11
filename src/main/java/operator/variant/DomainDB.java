package operator.variant;

/**
 * Retrieves domain information for variants from Pfam/SCOP
 * MUST be tabix-compressed and indexed
 * @author daniel
 * Borrowing from David F's HaloplexDB
 */

import java.io.File;
import java.io.IOException;

import org.broad.tribble.readers.TabixReader;

public class DomainDB {
	private File dbFile;
	private TabixReader reader = null;
    public DomainDB(File file) throws IOException {
		if (! file.exists()) {
			throw new IOException("File " + dbFile.getAbsolutePath() + " does not exist");
		}
		dbFile = file;
		
		reader = new TabixReader(dbFile.getAbsolutePath());
	}
    
    public String[] getInfoForPosition(String contig, int pos) throws IOException {
		String queryStr = contig + ":" + pos + "-" + (pos);
		
		try {
			TabixReader.Iterator iter = reader.query(queryStr);

			if(iter != null) {
					String str = iter.next();
					while(str != null) {
						String[] toks = str.split("\t");
						if(toks[0].equals(contig)) {
							str = iter.next();
							continue;
						}
						if(toks[8].equals("n/a")) {
							str = iter.next();
							continue;
						}
						Integer startPos = Integer.parseInt(toks[1]) + Integer.parseInt(toks[8]);
						Integer endPos = Integer.parseInt(toks[1]) + Integer.parseInt(toks[9]);
						
						if(pos >= startPos && pos <= endPos) {
							String domainName = toks[10];
							String pfamDesc = toks[6];
							String pfamID = toks[5];
							String pfamAC = toks[4];
							return new String[]{pfamAC,pfamID,pfamDesc,domainName};
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