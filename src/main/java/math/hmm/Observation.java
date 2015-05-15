package math.hmm;

public class Observation {

	final Double value;
	final int position;
	
	public Observation(double value, int position) {
		this.value = value;
		this.position = position;
	}
	
	
	public String toString() {
		return this.position + ", " + this.value;
	}
}
