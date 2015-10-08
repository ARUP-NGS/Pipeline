package util.bamWindow;

import util.Interval;

/**
 * Represents the 'template' dna that was sequenced to produce one or more mapped reads.
 * Right now, all we keep track of is the beginning and end position - which makes this thing
 * exactly an Interval. We might add more info in the future.  
 * @author brendan
 *
 */
public class MappedTemplate extends Interval {

	MappedRead firstRead;
	
	
	public MappedTemplate(int begin, int end, MappedRead read) {
		super(begin, end);
		this.firstRead = read;
	}
	
	public String toString() {
		return super.toString() + ": " + firstRead.getRecord().toString();
	}
	
	
}
