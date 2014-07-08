package buffer;

import java.io.File;

public class JSONBuffer extends FileBuffer {

	
	public JSONBuffer() {
	}
	
	public JSONBuffer(File file) {
		super(file);
	}

	@Override
	public String getTypeStr() {
		return "JSON file";
	}

}
