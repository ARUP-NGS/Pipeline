package math.hmm;

public class Observation implements Comparable<Observation> {

	public final Double value;
	public final int position;
	public final String chr;
	
	public Observation(double value, String chr, int position) {
		this.value = value;
		this.position = position;
		this.chr = chr;
	}
	
	
	public String toString() {
		return this.position + ", " + this.value;
	}


	@Override
	public int compareTo(Observation a) {
		return Integer.compare(this.position, a.position);
	}
	
}