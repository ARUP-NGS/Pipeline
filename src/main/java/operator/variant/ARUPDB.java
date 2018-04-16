package operator.variant;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.broad.tribble.readers.TabixReader;

/**
 * Stores information about variants that have previously been observed at ARUP, right now 
 * this expects things to be in the .csv type flatfile produced by the CompareVarFreqs class.
 * ... and now *MUST* be tabix-compressed and indexed
 * @author brendan
 *
 */
public class ARUPDB {

	private File dbFile;
	private Map<Integer, String> headerToks = new HashMap<Integer, String>();
	private TabixReader reader = null;
	
	public ARUPDB(File dbFile) throws IOException {
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

	
	public QueryResult getInfoForVariant(String contig, int pos, String ref, String alt) throws IOException {
		String queryStr = contig + ":" + pos + "-" + (pos);
		
		try {
			TabixReader.Iterator iter = reader.query(queryStr);
			if(iter != null) {
					String str = iter.next();
					while(str != null) {
						String[] toks = str.split("\t");
						Integer qPos = Integer.parseInt(toks[1]);
						String qRef = toks[2];
						String qAlt = toks[3];
						String overall = toks[4];
						if (qPos == pos && qRef.equals(ref) && qAlt.equals(alt) && overall.equals("overall")) {
							//Found one..
							String sampleTotalStr = toks[5];
							String hetsFoundStr = toks[6];
							String homsFoundStr = toks[7];
							
							double totalSamples = Double.parseDouble(sampleTotalStr);
							double overallHets = Double.parseDouble(hetsFoundStr);
							double overallHoms = Double.parseDouble(homsFoundStr);
							double overallAF = (overallHets + 2.0*overallHoms)/(double)(2.0*totalSamples); 
							
							
							//Create fancier details string here...
							String details = "Samples: " + (int)totalSamples + " Hets: " + (int)overallHets + " Homs: " + (int)overallHoms;
							
							QueryResult result = new QueryResult();
							result.overallFreq = overallAF;
							result.totHets = overallHets;
							result.totHoms = overallHoms;
							result.totSamples = totalSamples;
							result.details = details;
							return result;
							
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
	
	class QueryResult {
		public Double overallFreq;
		public Double totSamples;
		public Double totHets;
		public Double totHoms;
		public String details;
	}
	
	
//	public static void main(String[] args) throws IOException {
//		ARUPDB db = new ARUPDB(new File("/home/brendan/resources/arup_db_20121220.csv.gz"));
//		
//		System.out.println( db.getInfoForPostion("17", 22259905));
//	}
}
