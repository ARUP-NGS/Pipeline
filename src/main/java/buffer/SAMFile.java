package buffer;

import java.io.File;

public class SAMFile extends FileBuffer {

	public SAMFile() {
	}
	
	public SAMFile(File file) {
		super(file);
	}

	@Override
	public String getTypeStr() {
		return "SAMFile";
	}

}
