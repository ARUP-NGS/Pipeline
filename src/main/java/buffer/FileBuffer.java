package buffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;

/**
 * Base class of buffers that refer to a file. 
 * @author brendan
 *
 */
public abstract class FileBuffer extends PipelineObject {

	public static final String FILENAME_ATTR = "filename";
	public static final String BINARY_ATTR = "binary";
	public static final String CONTIG_ATTR = "contig";
	
	protected File file;
	protected Map<String, String> properties = new HashMap<String, String>();
	
	public FileBuffer() {
		//Blank on purpose, we just need to make sure there's a no-arg constructor
	}
	
	public FileBuffer(File file) {
		this.file = file;
		setAttribute(FILENAME_ATTR, file.getName());
	}
	
	public void setAttribute(String key, String value) {
		properties.put(key, value);
	}
	

	public String getAttribute(String key) {
		return properties.get(key);
	}
	
	public Collection<String> getAttributeKeys() {
		return properties.keySet();
	}
	
	/**
	 * Sets the contig attribute for this file buffer
	 * @param contig
	 */
	public void setContig(String contig) {
		setAttribute(CONTIG_ATTR, contig);
	}
	
	/**
	 * Get the contig associated with this file, may be null if no contig has been set
	 * @return
	 */
	public String getContig() {
		return properties.get(CONTIG_ATTR);
	}
	
	/**
	 * Returns true if the binary flag has been set for this FileBuffer, indicating that reads/writes
	 * should be done in binary form
	 * @return
	 */
	public boolean isBinary() {
		String binStr = properties.get(BINARY_ATTR);
		if (binStr == null) {
			return false;
		}
		else {
			Boolean binary = Boolean.parseBoolean(binStr);
			return binary;
		}
	}
	
	public FileInputStream getInputStream() throws FileNotFoundException {
		if (file == null)
			return null;
		else
			return new FileInputStream(file);
	}
	
	public FileOutputStream getOutputStream() throws FileNotFoundException {
		if (file == null)
			return null;
		else 
			return new FileOutputStream(file);
	}
	
	public void initialize(NodeList children) throws IllegalStateException {
		String filename = properties.get(FILENAME_ATTR);
		if (filename == null || filename.length()==0) {
			Logger.getLogger(Pipeline.primaryLoggerName).warn("No filename attribute found for FileBuffer object " + getObjectLabel() + ", assuming filename will be set at runtime.");
		}
		
		if (filename != null) {
			//If the input file path does not start with '/' and the PROJECT_HOME property has been set,
			//then we append PROJECT_HOME to the file path
			String pathMod = "";
			if (! filename.startsWith("/")) {
				String projHome = properties.get(Pipeline.PROJECT_HOME);
				if (projHome != null) {
					pathMod = projHome;
				}
			}


			file = new File(pathMod + filename);
		}
	}
	
	
	/**
	 * Set the file associated with this buffer. This also sets the FILENAME property to the absolute path
	 * of the given file
	 * @param file
	 */
	public void setFile(File file) {
		properties.put(FILENAME_ATTR, file.getAbsolutePath());
		this.file = file;
	}
	
	
	/**
	 * Obtain the file object associated with this buffer
	 * @return
	 */
	public File getFile() {
		return file;
	}
	
	/**
	 * Return the name of the file
	 * @return
	 */
	public String getFilename() {
		if (file == null)
			return null;
		return file.getName();
	}
	
	/**
	 * Return the absolute path of the file
	 * @return
	 */
	public String getAbsolutePath() {
		if (file==null)
			return null;
		return file.getAbsolutePath();
	}
	
	/**
	 * Obtain the unique type string associated with this file type
	 * @return
	 */
	public abstract String getTypeStr();
}
