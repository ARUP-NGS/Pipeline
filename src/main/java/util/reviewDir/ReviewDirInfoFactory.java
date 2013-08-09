package util.reviewDir;


/**
 * These objects are capable of constructing a ReviewDirInfoObject from a directory
 * @author brendan
 *
 */
public interface ReviewDirInfoFactory {

	/**
	 * Attempt to read information about the review directory at the given path
	 * @param pathToReviewDir
	 * @return
	 */
	public ReviewDirInfo constructInfo(String pathToReviewDir) throws ReviewDirParseException;
	
}
