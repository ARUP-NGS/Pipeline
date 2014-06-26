package util.reviewDir;


/**
 * These objects are capable of constructing a ReviewDirInfoObject from a directory
 * @author brendan
 *
 */
public interface ManifestReader {

	/**
	 * Attempt to read the manifest from  the review dir at the given path
	 * @param pathToReviewDir
	 * @return
	 */
	public SampleManifest readManifest(String pathToReviewDir) throws ManifestParseException;
	
}
