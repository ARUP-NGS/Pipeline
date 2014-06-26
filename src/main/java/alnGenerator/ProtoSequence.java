package alnGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * A mutable sequence that is used to temporarily store sequence data for use
 * with the AlignmentGenerator
 * @author brendan
 *
 */
public class ProtoSequence {

	public static final String GAP = "-";
	
	//Creates a mapping from reference position to index in the sequence
	//It basically just contains a list of pairs of numbers that describe what the reference
	//pos is at actual sites, so we can easily look up a real index given a reference pos
	List<Pair> refMap = new ArrayList<Pair>();
	StringBuilder seq;
	String sampleName = null;
	
	public ProtoSequence(String str) {
		seq = new StringBuilder(str);
		Pair first = new Pair(1, 0); //Since the StringBuider is zero-indexed and the variants are 1-sequenced
		refMap.add(first);
	}
	
	public ProtoSequence(String str, int refStartPos) {
		seq = new StringBuilder(str);
		Pair first = new Pair(refStartPos, 0); //Since the StringBuider is zero-indexed and the variants are 1-sequenced
		refMap.add(first);
	}
	
	public void setSampleName(String name) {
		this.sampleName = name;
	}
	
	/**
	 * Returns the lowest index in this sequence that corresponds to the given reference
	 * position 
	 * @param refPos
	 * @return
	 */
	public int seqIndexForRefPos(int refPos) {
		Pair prev = refMap.get(0);
		for(Pair p : refMap) {
			
			//Sanity check, this can maybe be removed sometime
			if (prev.refPos > p.refPos || prev.actualIndex > p.actualIndex) {
				throw new IllegalStateException("Ref-map list is corrupted...");
			}
			
			if (prev.refPos <= refPos && p.refPos > refPos) {
				return prev.actualIndex + (refPos - prev.refPos);
			}
			prev = p;
		}
		return prev.actualIndex + (refPos - prev.refPos);
	}
	
	public char getBaseForRef(int refPos) {
		return seq.charAt( seqIndexForRefPos(refPos) );
	}
	
	public void applyVariant(Variant var, int phase) {
		String alt = null;
		if (phase==0) {
			alt = var.getAlt0();
		}
		if (phase==1)
			alt = var.getAlt1();
		if (alt.equals( var.getRef() )) {
			System.out.println("Alt equals ref at this site, ignoring (alt:" + alt  + " ref:" + var.getRef() +")");
			return; //no variant, do nothing
		}
		
		//Altering wont change mapping
		if (alt.length()==1) {
			int refPos = var.getPos(); //This number is in reference coords
			int seqPos = seqIndexForRefPos(refPos); //Ref coords converted to indices for string
			System.out.println("Replacing base at site " + seqPos + " with " + alt);
			seq.replace(seqPos, seqPos+1, alt);
		}
		else {
			System.out.println("WARNING: skipping insertion " + alt + " at pos: " + var.getPos());
		}
			
	}
	
	public String toString() {
		return ">" + sampleName + "\n" + seq.toString();
	}
	
	
	class Pair {
		int refPos;
		int actualIndex;
		
		public Pair(int ref, int actual) {
			this.refPos = ref;
			this.actualIndex = actual;
		}
	}
}
