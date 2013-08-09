package util.reviewDir;

/**
 * These are thrown when there's an error attempting to construct a ReviewDirInfo object
 * @author brendan
 *
 */
public class ReviewDirParseException extends Exception {
	
	public ReviewDirParseException(String message) {
		super(message);
	}

}
