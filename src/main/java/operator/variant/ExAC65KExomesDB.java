package operator.variant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.types.CommandlineJava.SysProperties;
import org.broad.tribble.readers.TabixReader;

import com.google.common.base.Joiner;

/**
 * Stores information about variants that have previously been observed at ARUP, right now 
 * this expects things to be in the .csv type flatfile produced by the CompareVarFreqs class.
 * ... and now *MUST* be tabix-compressed and indexed
 * @author brendan
 *
 */
public class ExAC65KExomesDB {

	private File dbFile;
	private Map<Integer, String> headerToks = new HashMap<Integer, String>();
	private TabixReader reader = null;
	
	public ExAC65KExomesDB(File dbFile) throws IOException {
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
							//System.out.println("Hey, I found one! Let's see if I can pull the information I need.");
							
							String sampleINFO = toks[7];
							//System.out.println("I found sampleINFO. This information is: " + sampleINFO);
							String[] InfoPairs= sampleINFO.split(";");
							LinkedHashMap<String, String> CountsFreqs = new LinkedHashMap<String, String>();
							for(String pair: InfoPairs){
								CountsFreqs.put(pair.split("=")[0], pair.split("=")[1]);
							}
							/*
							for(String key: CountsFreqs.keySet()){
								System.out.println("I am currently iterating through keys: " + key);
							}
							*/
							ArrayList<String> ReturnList = new ArrayList<String>();
							
							// Getting general frequencies and zygosities
							double AlleleFrequency = Double.parseDouble(CountsFreqs.get("AF"));
							ReturnList.add(String.valueOf(AlleleFrequency));
							double NumCalledAlleles = Double.parseDouble(CountsFreqs.get("AN"));
							int AlleleCount = Integer.parseInt(CountsFreqs.get("AC"));
							double HomFreq = Double.parseDouble(CountsFreqs.get("AC_Hom"))/AlleleCount;
							ReturnList.add(String.valueOf(HomFreq));
							double HetFreq = Double.parseDouble(CountsFreqs.get("AC_Het"))/AlleleCount;
							ReturnList.add(String.valueOf(HetFreq));
							
							
							//System.out.println("Now attempting to get ethnicity-specific frequencies.");
							// Getting ethnicity-specific frequencies and zygosities
							double NumCalledAllelesAfr = Double.parseDouble(CountsFreqs.get("AN_AFR"));
							double AlleleFreqAfr = (double)Integer.parseInt(CountsFreqs.get("AC_AFR"))/NumCalledAllelesAfr;
							ReturnList.add(String.valueOf(AlleleFreqAfr));
							double HomFreqAfr = (double)Integer.parseInt(CountsFreqs.get("Hom_AFR"))/NumCalledAllelesAfr;
							ReturnList.add(String.valueOf(HomFreqAfr));
							//System.out.println("HomFreqAfr = " + String.valueOf(HomFreqAfr));
							double HetFreqAfr = ((double)Integer.parseInt(CountsFreqs.get("AC_AFR"))-(double)Integer.parseInt(CountsFreqs.get("Hom_AFR"))*2)/(NumCalledAllelesAfr);
							ReturnList.add(String.valueOf(HetFreqAfr));
							
							double NumCalledAllelesAmr = Double.parseDouble(CountsFreqs.get("AN_AMR"));
							double AlleleFreqAmr = (double)Integer.parseInt(CountsFreqs.get("AC_AMR"))/NumCalledAllelesAmr;
							ReturnList.add(String.valueOf(AlleleFreqAmr));
							double HomFreqAmr = (double)Integer.parseInt(CountsFreqs.get("Hom_AMR"))/NumCalledAllelesAmr;
							ReturnList.add(String.valueOf(HomFreqAmr));
							double HetFreqAmr = ((double)Integer.parseInt(CountsFreqs.get("AC_AMR"))-(double)Integer.parseInt(CountsFreqs.get("Hom_AMR"))*2)/(NumCalledAllelesAmr);
							ReturnList.add(String.valueOf(HetFreqAmr));
							
							double NumCalledAllelesEas = Double.parseDouble(CountsFreqs.get("AN_EAS"));
							double AlleleFreqEas = (double)Integer.parseInt(CountsFreqs.get("AC_EAS"))/NumCalledAllelesEas;
							ReturnList.add(String.valueOf(AlleleFreqEas));
							double HomFreqEas = (double)Integer.parseInt(CountsFreqs.get("Hom_EAS"))/NumCalledAllelesEas;
							ReturnList.add(String.valueOf(HomFreqEas));
							double HetFreqEas = ((double)Integer.parseInt(CountsFreqs.get("AC_EAS"))-(double)Integer.parseInt(CountsFreqs.get("Hom_EAS"))*2)/(NumCalledAllelesEas);
							ReturnList.add(String.valueOf(HetFreqEas));
							
							//System.out.println("Starting Finnish parsing.");
							double NumCalledAllelesFin = Double.parseDouble(CountsFreqs.get("AN_FIN"));
							double AlleleFreqFin = (double)Integer.parseInt(CountsFreqs.get("AC_FIN"))/NumCalledAllelesFin;
							ReturnList.add(String.valueOf(AlleleFreqFin));
							double HomFreqFin = (double)Integer.parseInt(CountsFreqs.get("Hom_FIN"))/NumCalledAllelesFin;
							ReturnList.add(String.valueOf(HomFreqFin));
							double HetFreqFin = ((double)Integer.parseInt(CountsFreqs.get("AC_FIN"))-(double)Integer.parseInt(CountsFreqs.get("Hom_FIN"))*2)/(NumCalledAllelesFin);
							ReturnList.add(String.valueOf(HetFreqFin));
							
							double NumCalledAllelesNfe = Double.parseDouble(CountsFreqs.get("AN_NFE"));
							double AlleleFreqNfe = (double)Integer.parseInt(CountsFreqs.get("AC_NFE"))/NumCalledAllelesNfe;
							ReturnList.add(String.valueOf(AlleleFreqNfe));
							double HomFreqNfe = (double)Integer.parseInt(CountsFreqs.get("Hom_NFE"))/NumCalledAllelesNfe;
							ReturnList.add(String.valueOf(HomFreqNfe));
							double HetFreqNfe = ((double)Integer.parseInt(CountsFreqs.get("AC_NFE"))-(double)Integer.parseInt(CountsFreqs.get("Hom_NFE"))*2)/(NumCalledAllelesNfe);
							ReturnList.add(String.valueOf(HetFreqNfe));
							
							double NumCalledAllelesSas = Double.parseDouble(CountsFreqs.get("AN_SAS"));
							double AlleleFreqSas = (double)Integer.parseInt(CountsFreqs.get("AC_SAS"))/NumCalledAllelesSas;
							ReturnList.add(String.valueOf(AlleleFreqSas));
							double HomFreqSas = (double)Integer.parseInt(CountsFreqs.get("Hom_SAS"))/NumCalledAllelesSas;
							ReturnList.add(String.valueOf(HomFreqSas));
							double HetFreqSas = ((double)Integer.parseInt(CountsFreqs.get("AC_SAS"))-(double)Integer.parseInt(CountsFreqs.get("Hom_SAS"))*2)/(NumCalledAllelesSas);
							ReturnList.add(String.valueOf(HetFreqSas));
							
							String overallStr[] = ReturnList.toArray(new String[ReturnList.size()]);
							//System.out.println("Returning string " + Joiner.on("\t").join(overallStr));
							return overallStr;
	
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
	
	
//	public static void main(String[] args) throws IOException {
//		ARUPDB db = new ARUPDB(new File("/home/brendan/resources/arup_db_20121220.csv.gz"));
//		
//		System.out.println( db.getInfoForPostion("17", 22259905));
//	}
}
