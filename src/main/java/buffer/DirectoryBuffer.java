package buffer;

import java.io.File;

import org.w3c.dom.NodeList;

/**
 * Simple container for a directory or folder "File"
 * @author dnix
 *
 */
public class DirectoryBuffer extends FileBuffer {

	public DirectoryBuffer() {
	}
	
	public DirectoryBuffer(File file) {
		super(file);
	}

	@Override
	public String getTypeStr() {
		return "Directory buffer";
	}


}
