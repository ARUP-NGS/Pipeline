package operator.pindel;

public class PindelResult {
	// Object starts with 100 pound symbols, but we won't save those, just use
	// them as a delimeter

	// First row of data, given 1 line for each field, including comments for
	// unsaved data
	private int index;
	private int finalIndex;
	private String varType;
	private int SVLength;
	// Always an NT field comes next, so not saved
	private int NTLength; //
	private String NTSeq;
	// Text "ChrID" always found here
	private String ChrID;
	// Text BP always found here
	private int BPStart;
	private int BPEnd;
	// Always BP_range
	private int BPRangeStart;
	private int BPRangeEnd;
	// Always Supports
	private int supportReads;
	private int uniqReads;
	// +
	private int upReads;
	private int upUniqReads;
	// -
	private int downReads;
	private int downUniqReads;
	// S1
	private int simpleScore;
	// SUM_MS
	private int sumMS;
	private int samplesScanned; // This value is the same for all records in an
								// output file
	// NumSupSamples
	private int numSupSamples;
	private String[] sampleNames;
	private int[] sampleCounts;
	private String[] readLines;

	public PindelResult(final String[] entryLines) {
		// System.out.println(entryLines[0]);
		String[] bits = entryLines[0].split("\t");
		String[] temp;

		index = Integer.parseInt(bits[0]);
		finalIndex = index;

		temp = bits[1].split(" ");
		varType = temp[0];
		SVLength = Integer.parseInt(temp[1]);

		temp = bits[2].split(" ");
		// Always an NT field comes next, so not saved
		// System.out.println("bad line is "+bits[2] + "in " + index);
		NTLength = Integer.parseInt(temp[1]); //
		NTSeq = temp[2];

		temp = bits[3].split(" ");
		// Text "ChrID" always found here
		ChrID = temp[1];

		temp = bits[4].split(" ");
		// Text BP always found here
		BPStart = Integer.parseInt(temp[1]);
		BPEnd = Integer.parseInt(bits[5]);

		temp = bits[6].split(" ");
		// Always BP_range
		BPRangeStart = Integer.parseInt(temp[1]);
		BPRangeEnd = Integer.parseInt(bits[7]);

		temp = bits[8].split(" ");
		// Always Supports
		supportReads = Integer.parseInt(temp[1]);
		uniqReads = Integer.parseInt(bits[9]);

		temp = bits[10].split(" ");
		// +
		upReads = Integer.parseInt(temp[1]);
		upUniqReads = Integer.parseInt(bits[11]);
		;

		temp = bits[12].split(" ");
		// -
		downReads = Integer.parseInt(temp[1]);
		downUniqReads = Integer.parseInt(bits[13]);
		;

		temp = bits[14].split(" ");
		// S1
		simpleScore = Integer.parseInt(temp[1]);

		temp = bits[15].split(" ");
		// SUM_MS
		sumMS = Integer.parseInt(temp[1]);
		samplesScanned = Integer.parseInt(bits[16]); // This value is the same
														// for all records

		// skipping 17, useless
		// NumSupSamples
		numSupSamples = Integer.parseInt(bits[18]);
		sampleNames = new String[bits.length - 18];
		sampleCounts = new int[(bits.length - 18) * 6];
		for (int loopIndex = 19; loopIndex < bits.length; loopIndex++) {
			temp = bits[loopIndex].split(" ");
			sampleNames[loopIndex - 19] = temp[0];
			sampleCounts[(loopIndex - 19) * 6] = Integer.parseInt(temp[1]);
			sampleCounts[(loopIndex - 19) * 6 + 1] = Integer.parseInt(temp[2]);
			sampleCounts[(loopIndex - 19) * 6 + 2] = Integer.parseInt(temp[3]);
			sampleCounts[(loopIndex - 19) * 6 + 3] = Integer.parseInt(temp[4]);
			sampleCounts[(loopIndex - 19) * 6 + 4] = Integer.parseInt(temp[5]);
			sampleCounts[(loopIndex - 19) * 6 + 5] = Integer.parseInt(temp[6]);
		}
		readLines = new String[entryLines.length - 1];
		for (int lines = 1; lines < entryLines.length; lines++) {
			readLines[lines - 1] = entryLines[lines];
		}
		// System.out.println("Result created, index " + index);
	}

	public String toString() {
		String total = "";
		int count = 0;
		String firstLine = index + "\t" + varType + " " + SVLength + "\t"
				+ "NT" + " " + NTLength + " " + NTSeq + "\t" + "ChrID" + " "
				+ ChrID + "\t" + "BP" + " " + BPStart + "\t" + BPEnd + "\t"
				+ "BP_range" + " " + BPRangeStart + "\t" + BPRangeEnd + "\t"
				+ "Supports" + " " + supportReads + "\t" + uniqReads + "\t"
				+ "+" + " " + upReads + "\t" + upUniqReads + "\t" + "-" + " "
				+ downReads + "\t" + downUniqReads + "\t" + "S1" + " "
				+ simpleScore + "\t" + "SUM_MS" + " " + sumMS + "\t"
				+ samplesScanned + "\t" + "NumSupSamples" + " " + numSupSamples
				+ "\t" + numSupSamples;
		for (String name : sampleNames) {
			if (name == null)
				break;
			firstLine = firstLine.concat("\t");
			firstLine = firstLine.concat(name + "");
			for (int start = count * 6; start < ((count + 1) * 6); start++) {
				firstLine = firstLine.concat(" "
						+ Integer.toString(sampleCounts[start]));
			}

			count++;
		}
		firstLine = firstLine.concat("\n");
		total = total.concat(firstLine);
		for (String line : readLines) {
			// System.out.println("adding line "+line);
			total = total.concat(line + "\n");
		}
		return total;
	}

	public int getSVLength() {
		return SVLength;
	}

	public int getIndex() {
		return index;
	}

	public String getChromo() {
		return ChrID;
	}

	public int getRangeStart() {
		return BPRangeStart;
	}

	public int getRangeEnd() {
		return BPRangeEnd;
	}

	public int getSupportReads() {
		return supportReads;
	}

	public boolean sameHit(PindelResult next) {
		System.out.println("In samehit");
		if (finalIndex == next.getIndex() - 1) {
			System.out.println("Index match");
			if (ChrID.equals(next.getChromo())) {
				System.out.println("Chromosome match");
				if (Math.abs(BPRangeStart - next.getRangeStart()) < 100) {
					System.out.println("Same found at " + index);
					return true;
				}
			}
		}
		System.out.println(finalIndex + " " + next.getIndex());
		return false;
	}

	public void add(PindelResult next) {
		finalIndex = next.getIndex();
		if (SVLength < next.getSVLength()) {
			SVLength = next.getSVLength();
		}

		if (BPRangeStart < next.getRangeStart()) {
			BPRangeStart = next.getRangeStart();
		}

		if (BPRangeEnd < next.getRangeEnd()) {
			BPRangeEnd = next.getRangeEnd();
		}
		supportReads += next.getSupportReads();
	}

	public void printSummary() {
		System.out.println(index + "\t" + finalIndex + "\t" + SVLength + "\t"
				+ ChrID + "\t" + BPRangeStart + "\t" + BPRangeEnd + "\t"
				+ supportReads);
	}
}