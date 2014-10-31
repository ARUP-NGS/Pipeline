package operator.pindel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PindelParser {
	private BufferedReader in;
	private int minCompositeSupported=5;
	private List<PindelResult> resultsList;

	/**
	 * @param filename A Pindel output file
	 * @throws FileNotFoundException
	 */
	public PindelParser(File filename) throws FileNotFoundException {
		resultsList = new ArrayList<PindelResult>();
		String line;
		ArrayList<String> nextResult = new ArrayList<String>();
		in = new BufferedReader(new FileReader(filename));
		try {
			line = in.readLine(); // skip over pound signs
			while ((line = in.readLine()) != null) {
				while (line.charAt(0) != '#') {
					nextResult.add(line);
					if ((line = in.readLine()) == null) {
						break;
					}
				}
				String[] converted = new String[nextResult.size()];
				converted = nextResult.toArray(converted);
				PindelResult tempResult = new PindelResult(converted);
				resultsList.add(tempResult);
				nextResult.clear();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void filter(int threshold) {
		Iterator<PindelResult> iter = resultsList.iterator();
		
		
		while (iter.hasNext()) {
			PindelResult temp = iter.next();
			if (temp.getSVLength() < threshold) {
				iter.remove();
			}
		}
	}

	public Object getLength() {
		return resultsList.size();
	}

	public String printPINDEL() {
		String result = "";
		for (PindelResult currentResult : resultsList) {
			result = result
					.concat("####################################################################################################\n");
			result = result.concat(currentResult.toString());
		}
		return result;
	}

	public void makePindelFile(File outFile) {
		try {
			PrintWriter writer = new PrintWriter(outFile);
			writer.print(printPINDEL());
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	

	public void combineResults(int mergeDistance) {		
		//Collect all hits by ntSeq first, 
		Map<String, List<PindelResult>> hitMap = new HashMap<String, List<PindelResult>>();
		for (PindelResult next : resultsList) {
			String seq = next.getSequence();
			List<PindelResult> hitResults = hitMap.get(seq);
			if (hitResults == null) {
				hitResults = new ArrayList<PindelResult>();
				hitMap.put(seq, hitResults);
			}
			hitResults.add(next);
		}
		
		//Now, for each set of results that has the same sequence see if we can merge them... 
		for(String seq : hitMap.keySet()) {
			
			//Find result with maximum support
			PindelResult maxSupport = hitMap.get(seq).get(0);
			for (PindelResult next :  hitMap.get(seq)) {
				if (next.getSupportReads() > maxSupport.getSupportReads()) {
					maxSupport = next;
				}
			}	
			
			List<PindelResult> mergeables = new ArrayList<PindelResult>();
			int totalSupport = 0;
			for (PindelResult next :  hitMap.get(seq)) {
				if ((next != maxSupport) && next.sameHit(maxSupport, mergeDistance)) {
					mergeables.add(next);
					totalSupport += next.getSupportReads();
				}
			}
			
			//This is the 'definitive' version, we use the breakpoints from it, and 
			//add the support from all the overlapping sameHits to it
			maxSupport.setSupportReads(maxSupport.getSupportReads() + totalSupport);
			
			//Now remove all the hits we merged into maxSupport
			for(PindelResult merged : mergeables) {
				resultsList.remove(merged);
			}
			
		}
		
//		for (PindelResult next : resultsList) {
//			if (current == null) {
//				current = next;
//			} else {
//				if (current.sameHit(next, mergeDistance)) {
//					current.add(next);
//				} else {
//					if(current.getSupportReads()>minCompositeSupported) {
//						combinedResults.add(current);
//					}
//					current = next;
//				}
//			}
//		}
		
		
	}
	
	public List<PindelResult> getResults() {
		return resultsList;
	}
}
