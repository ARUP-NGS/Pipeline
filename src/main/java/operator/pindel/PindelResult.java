package operator.pindel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import util.Interval;

/**
 * A single structural variation detected by Pindel, typically parsed using PindelParser
 * from a Pindel output directory
 * @author brendan
 *
 */
public class PindelResult {
	

	// First row of data, given 1 line for each field, including comments for
	// unsaved data
	private int index;
	private int finalIndex;
	private String varType;
	private int svLength;
	// Always an NT field comes next, so not saved
	private int ntLength; //
	private String ntSeq;
	// Text "ChrID" always found here
	private String ChrID;
	// Text BP always found here
	private int bpStart;
	private int bpEnd;
	// Always BP_range
	private int bpRangeStart;
	private int bpRangeEnd;
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

	private List<String> featureAnnotations = new ArrayList<String>();
	
	private double meanDepth; //Must be computed externally - pindel doesn't set this by itself.
	

	public PindelResult(final String[] entryLines) {
		// System.out.println(entryLines[0]);
		String[] bits = entryLines[0].split("\t");
		String[] temp;

		index = Integer.parseInt(bits[0]);
		finalIndex = index;

		temp = bits[1].split(" ");
		varType = temp[0];
		svLength = Integer.parseInt(temp[1]);

		temp = bits[2].split(" ");
		// Always an NT field comes next, so not saved
		// System.out.println("bad line is "+bits[2] + "in " + index);
		ntLength = Integer.parseInt(temp[1]); //
		ntSeq = temp[2];

		temp = bits[3].split(" ");
		// Text "ChrID" always found here
		ChrID = temp[1].toUpperCase().replace("CHR", "");

		temp = bits[4].split(" ");
		// Text BP always found here
		bpStart = Integer.parseInt(temp[1]);
		bpEnd = Integer.parseInt(bits[5]);

		temp = bits[6].split(" ");
		// Always BP_range
		bpRangeStart = Integer.parseInt(temp[1]);
		bpRangeEnd = Integer.parseInt(bits[7]);

		temp = bits[8].split(" ");
		// Always Supports
		supportReads = Integer.parseInt(temp[1]);
		uniqReads = Integer.parseInt(bits[9]);

		temp = bits[10].split(" ");
		// +
		upReads = Integer.parseInt(temp[1]);
		upUniqReads = Integer.parseInt(bits[11]);
		

		temp = bits[12].split(" ");
		// -
		downReads = Integer.parseInt(temp[1]);
		downUniqReads = Integer.parseInt(bits[13]);
		

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

	public void setSupportReads(int newSupport) {
		this.supportReads = newSupport;
	}
	
	public void addFeatureAnnotation(String anno) {
		featureAnnotations.add(anno);
	}
	
	public List<String> getAllAnnotations() {
		return Collections.unmodifiableList(featureAnnotations);
	}
	
	public String toShortString() {
		String msg = varType + " " + ChrID + ":" + bpStart + "-" + bpEnd + " (support " + supportReads + ")";
//		for(String feat : featureAnnotations) {
//			msg = msg + "\t" + feat;
//		}
		
		return msg;
	}
	
	
	
	public String toString() {
		String total = "";
		int count = 0;
		String firstLine = index + "\t" + varType + " " + svLength + "\t"
				+ "NT" + " " + ntLength + " " + ntSeq + "\t" + "ChrID" + " "
				+ ChrID + "\t" + "BP" + " " + bpStart + "\t" + bpEnd + "\t"
				+ "BP_range" + " " + bpRangeStart + "\t" + bpRangeEnd + "\t"
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
		return svLength;
	}

	public int getIndex() {
		return index;
	}

	public String getChromo() {
		return ChrID;
	}

	public int getRangeStart() {
		return bpRangeStart;
	}

	public int getRangeEnd() {
		return bpRangeEnd;
	}

	public int getSupportReads() {
		return supportReads;
	}
	
	public double getMeanDepth() {
		return meanDepth;
	}

	public void setMeanDepth(double meanDepth) {
		this.meanDepth = meanDepth;
	}
	
	public int getSize() {
		return svLength;
	}
	
	public String getSequence() {
		return ntSeq;
	}
	
	public boolean sameHit(PindelResult next, int mergeDistance) {
		if (this.varType.equals(next.varType)) {
			if (ChrID.equals(next.getChromo())) {
				Interval thisInt = new Interval(bpRangeStart-mergeDistance, bpRangeStart+svLength+mergeDistance);
				Interval otherInt = new Interval(next.getRangeStart()-1, next.getRangeStart()+next.getSize()+1);
				
				if (thisInt.intersects(otherInt) && next.getSequence().equals(this.getSequence())) {
					return true;
				}
			}
		}
		
		return false;
	}

	public void add(PindelResult next) {		
		finalIndex = next.getIndex();
		if (svLength < next.getSVLength()) {
			svLength = next.getSVLength();
		}
		
		bpRangeStart = Math.min(bpRangeStart, next.getRangeStart());
		bpRangeEnd = Math.max(bpRangeEnd, next.getRangeEnd());
		
		supportReads += next.getSupportReads();
	}

	public void printSummary() {
		System.out.println(index + "\t" + finalIndex + "\t" + svLength + "\t"
				+ ChrID + "\t" + bpRangeStart + "\t" + bpRangeEnd + "\t"
				+ supportReads);
	}
}