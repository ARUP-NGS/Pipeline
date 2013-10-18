package operator.variant;

/**
 * Stores information about variants that we believe to be false positives from a HaloPlex 
 * run due to comparison with the 1000 genomes project this expects things to be in the 
 * .csv type flatfile produced by researchers. MUST be tabix-compressed and indexed
 * @author dave f, stealing a lot from ARUPDB,java by brendan
 */

import java.io.File;
import java.io.IOException;

import org.broad.tribble.readers.TabixReader;

public class HaloplexDB {
	private File dbFile;
	private TabixReader reader = null;
    public HaloplexDB(File file) throws IOException {
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
						Integer qPos = Integer.parseInt(toks[1]);
						
						
						if (qPos == pos) {
							//Found one..
							
							String sampleTotalStr = toks[4];
							String hetsFoundStr = toks[5];
							String homsFoundStr = toks[6];
							
							double totalSamples = Double.parseDouble(sampleTotalStr);
							double overallHets = Double.parseDouble(hetsFoundStr);
							double overallHoms = Double.parseDouble(homsFoundStr);
							double overallAF = (overallHets + 2.0*overallHoms)/(double)(2.0*totalSamples); 
						
							String details = "Samples: " + (int)totalSamples + " Hets: " + (int)overallHets + " Homs: " + (int)overallHoms;
							
							return new String[]{Double.toString(overallAF), details};
	
						}
						if (qPos > pos) {
							break;
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