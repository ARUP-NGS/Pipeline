package alnGenerator;

/**
 * A single variant obtained from a line in a vcf file and associated with a single sample
 * @author brendan
 *
 */
public class Variant {

	String contig;
	int pos;
	String ref;
	String alt0;
	String alt1;
	Double quality;
	Integer depth;
	
	public Variant(String contig, int pos, String ref, String alt0, String alt1, double quality, int depth) {
		this.contig = contig;
		this.pos = pos;
		this.ref = ref;
		this.alt0 = alt0;
		this.alt1 = alt1;
		this.quality = quality;
		this.depth = depth;
	}
	
	public String getContig() {
		return contig;
	}
	
	public int getPos() {
		return pos;
	}
	
	public String getRef() {
		return ref;
	}
	
	public String getAlt0() {
		return alt0;
	}
	
	public String getAlt1() {
		return alt1;
	}
	
	public double getQuality() {
		return quality;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public String toString() {
		return contig + "\t" + pos + "\t" + ref + "\t" + alt0 + "," + alt1 + "\t" + quality + "\t" + depth;
	}
}
