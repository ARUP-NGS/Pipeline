package gene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Allows lookups of mito-specific annotations, specifially tRNAs
 * @author brendan
 *
 */
public class MitoAnnoLookupContainer extends BasicIntervalContainer {

	public void readIntervals(File mitogbk) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(mitogbk));
		String line = reader.readLine();
		
		boolean startLooking = false;
		while(line != null) {
			if (line.startsWith("FEATURES")) {
				startLooking = true;
			}
			if (line.startsWith("ORIGIN")) {
				startLooking = false;
			}
			
			if (line.trim().length()<5) {
				line = reader.readLine();
				continue;
			}
			
			if (startLooking) {
				
				
				String firstTok = line.substring(0,15).trim();
				
				//Just get tRNAs, that's all they want for now. 
				if (firstTok.equals("tRNA")) {
					MitoAnnoInfo annoInfo = parseInfo(reader, line);
					addInterval("MT", annoInfo.start, annoInfo.end, annoInfo);						
				}
				
				
			}
			
			line = reader.readLine();
		}
		
		reader.close();
	}
	
	private MitoAnnoInfo parseInfo(BufferedReader reader, String line) throws IOException {
		String firstTok = line.substring(3,15).trim();
		String secondTok = line.substring(15).trim();
		
		boolean dir = ! secondTok.contains("complement");
		 
		
		String[] posToks = secondTok.replace("complement(", "").replace(")", "").split("\\.\\.");
		Integer start = Integer.parseInt(posToks[0]);
		Integer end = Integer.parseInt(posToks[1]);
		
		line = reader.readLine();
		while(line != null && (line.substring(5, 15).length()==0) && !line.trim().startsWith("/gene") ) {
			line = reader.readLine();
		}
		
		if (! line.trim().startsWith("/gene")) {
			throw new IOException("Parsing error!");
		}
		
		MitoAnnoInfo annoInfo = new MitoAnnoInfo();
		annoInfo.featureType = firstTok;
		annoInfo.direction = dir;
		annoInfo.featureName = line.trim().replace("/gene=", "").replace("\"", "");
		annoInfo.start = start;
		annoInfo.end = end;
		
		return annoInfo;
	}
	
	public class MitoAnnoInfo {
		public int start;
		public int end;
		public boolean direction; //true = forward
		public String featureType;
		public String featureName;
	}
	
	
}
