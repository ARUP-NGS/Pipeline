package operator.pindel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PindelParser {
	private BufferedReader in;
	private int minCompositeSupported=5;
	private ArrayList<PindelResult> resultsList;

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

	public void filter(int i) {
		Iterator<PindelResult> iter = resultsList.iterator();
		PindelResult temp;
		while (iter.hasNext()) {
			temp = iter.next();
			// System.out.println("Checking result " + temp.getIndex());
			if (temp.getSVLength() < 15) {
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

	public void makeVCF(final String prefix, final String reference,
			final String pindelAddress) {
		String call = pindelAddress + "pindel2vcf -P out2 -r " + reference
				+ "-R sampleRef -d 20101123 -v PINDEL.vcf";
	}

	public void combineResults() {
		ArrayList<PindelResult> subset = new ArrayList<PindelResult>();
		PindelResult current = null;
		for (PindelResult next : resultsList) {
			if (current == null) {
				current = next;
			} else {
				if (current.sameHit(next)) {
					current.add(next);
				} else {
					if(current.getSupportReads()>minCompositeSupported)
					subset.add(current);
					current = next;
				}
			}
		}
		System.out
				.println("index\tfinalIndex\tSVLength\tChrID\tBPRangeStart\tBPRangeEnd\tsupportReads");
		for (PindelResult temp : subset) {
			temp.printSummary();
		}
	}
	
	public List<PindelResult> getResults() {
		return resultsList;
	}
}
