package buffer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;

/**
 * Represents a generic item to put into a review directory, and works only inside the  ReviewDirGenerator operator
 * The idea is to specify XML that looks like this:
 *  
 *  <SomeReviewSubdir class="buffer.ReviewSubDir" dir.name="log" manifest.key="log.info">
 *  	<MyThingToPutInTheSubdir />
 *  <SomeReviewSubdir/>
 *   
 * @author brendan
 *
 */
public class ReviewDirSubDir extends PipelineObject {

	public static final String DIR_NAME = "dir.name";
	public static final String MANIFEST_KEY = "manifest.key";
	public static final String COPY = "copy";
	
	protected String dirName = null; //relative path of subdir, e.g. var/ bam/, etc.
	protected String manifestKey = null; //key to put in to sampleManifest. Optional, no entry if omitted
	protected boolean copy = true; //If true, copy the file instead of moving it

	protected Map<String, String> properties = new HashMap<String, String>();
	private FileBuffer subdirFile = null;
	
	/**
	 * Get a reference to the FileBuffer object provided to this subdir 
	 * @return
	 */
	public FileBuffer getSubdirFile() {
		return subdirFile;
	}
	
	/**
	 * Indicates if the file should be copied into the subdir or just moved into the subdir. 
	 * @return
	 */
	public boolean copy() {
		return copy;
	}
	
	public String getDirName() {
		return dirName;
	}
	
	public String getManifestKey() {
		return manifestKey;
	}
	
	/**
	 * Returns the value associated with the manifest key
	 * @return
	 */
	public String getManifestValue() {
		if (subdirFile == null) {
			return "null";
		}
		
		return dirName + "/" + subdirFile.getFilename();
	}
	
	public boolean getIncludeInManifest() {
		return manifestKey != null;
	}
	
	@Override
	public void setAttribute(String key, String value) {
		properties.put(key, value);
	}
		
	@Override
	public String getAttribute(String key) {
		return properties.get(key);
	}
	
	public Collection<String> getAttributeKeys() {
		return properties.keySet();
	}

	@Override
	public void initialize(NodeList children) {
		
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				
				if (obj instanceof FileBuffer) {
					if (subdirFile != null) {
						throw new IllegalArgumentException("Only one input filebuffer and be specified.");
					}
					
					subdirFile = (FileBuffer)obj;
				}
				else {
					throw new IllegalArgumentException("Found non-FileBuffer object in input list for Operator " + getObjectLabel());
				}
			}
		}
		
		if (subdirFile == null) {
			throw new IllegalArgumentException("No filebuffers found");
		}
		
		dirName = properties.get(DIR_NAME);
		if (dirName == null) {
			throw new IllegalArgumentException("Subdir name must be specified");
		}
		
		manifestKey = properties.get(MANIFEST_KEY);
		String copyStr = properties.get(COPY);
		if (copyStr != null) {
			copy =Boolean.parseBoolean(copyStr);
		}
		
	}

}
