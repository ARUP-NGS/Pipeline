package buffer;

import java.io.File;

import org.w3c.dom.NodeList;

public class ReferenceFile extends FileBuffer {

	public static final String TYPE = "Reference file";

	public void initialize(NodeList children) throws IllegalStateException {
		String filename = properties.get(FILENAME_ATTR);
		if (filename == null || filename.length()==0) {
			throw new IllegalStateException("Property '" + FILENAME_ATTR + "' required to create file buffer object");
		}
		file = new File(filename);
		
		if (! file.exists()) {
			throw new IllegalStateException("Reference file at path : " + file.getAbsolutePath() + " does not exist");
		}
	}
	
	@Override
	public String getTypeStr() {
		return TYPE;
	}


}
