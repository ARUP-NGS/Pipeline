package operator.variant;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.broad.tribble.readers.TabixReader;

/**
 * Retrieves frequency from MitoMap database.
 * @author daniel
 *
 */
public class MitoMapFreqDB {

	private File dbFile;
	private Map<Integer, String> headerToks = new HashMap<Integer, String>();
	private TabixReader reader = null;
	
	public MitoMapFreqDB(File dbFile) throws IOException {
		if (! dbFile.exists()) {
			throw new IOException("File " + dbFile.getAbsolutePath() + " does not exist");
		}
		this.dbFile = dbFile;
		
		reader = new TabixReader(dbFile.getAbsolutePath());
		
		//Read header
		String header = reader.readLine();
		String[] toks = header.split("\t");
		for(int i=3; i<toks.length; i++) {
			String headerDesc = toks[i];
			if (toks[i].contains("[")) {
				headerDesc = toks[i].substring(0, toks[i].indexOf("["));
			}
			headerToks.put(i, headerDesc);
		}
		
	}

	
	public String[] getInfoForPosition(String contig, int pos, String alt) throws IOException {
		String queryStr = contig + ":" + pos + "-" + (pos);
		try {
			TabixReader.Iterator iter = reader.query(queryStr);
			if(iter != null) {
				String str = iter.next();
					while(str != null) {
						String[] toks = str.split("\t");
						Integer qPos = Integer.parseInt(toks[1]);
						String ALT = toks[3];
						
						if (qPos == pos && ALT.equals(alt)) {
							//Found one..							
							String overallAF = toks[5]; 
							String Allele = toks[4];
							
							return new String[]{overallAF, Allele};
	
						}
						if (qPos > pos) {
							//# break;
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
	
	
//	public static void main(String[] args) throws IOException {
//		ARUPDB db = new ARUPDB(new File("/home/brendan/resources/arup_db_20121220.csv.gz"));
//		
//		System.out.println( db.getInfoForPostion("17", 22259905));
//	}
}
