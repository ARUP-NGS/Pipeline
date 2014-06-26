package buffer;

import java.io.File;

import org.w3c.dom.NodeList;

/**
 * This file buffer is always associated with the 'pipeinstancelog'
 * @author brendan
 *
 */
public class InstanceLogFile extends FileBuffer {

	public InstanceLogFile() {
		file = null;
		
	}
	
	public InstanceLogFile(File file) {
		throw new IllegalStateException("Cannot specify a new file for the InstanceLogFile");
	}
	
	@Override
	public String getTypeStr() {
		return "instance.log";
	}
	
	/**
	 * Obtain the file object associated with this buffer
	 * @return
	 */
	public File getFile() {
		if (file == null)
			setFile( new File( getPipelineOwner().getInstanceLogPath() ));
		return file;
	}
	
	/**
	 * Return the name of the file
	 * @return
	 */
	public String getFilename() {
		if (file == null)
			setFile( new File( getPipelineOwner().getInstanceLogPath() ));
		return file.getName();
	}
	
	/**
	 * Return the absolute path of the file
	 * @return
	 */
	public String getAbsolutePath() {
		if (file == null)
			setFile( new File( getPipelineOwner().getInstanceLogPath() ));
		return file.getAbsolutePath();
	}

	public void initialize(NodeList children) throws IllegalStateException {
		String filename = properties.get(FILENAME_ATTR);
		if (filename != null) {
			throw new IllegalStateException("Cannot specify filename for the Pipeline Instance log file");
		}
		
	}
}
