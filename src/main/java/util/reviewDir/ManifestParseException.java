package util.reviewDir;

/**
 * These are thrown when there's an error attempting to construct a ReviewDirInfo object
 * @author brendan
 *
 */
public class ManifestParseException extends Exception {
	
	public ManifestParseException(String message) {
		super(message);
	}

}
