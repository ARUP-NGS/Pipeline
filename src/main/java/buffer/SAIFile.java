package buffer;

import java.io.File;

public class SAIFile extends FileBuffer {

	public SAIFile() {
	}
	
	public SAIFile(File file) {
		super(file);
	}

	public boolean isBinary() {
		return true;
	}
	
	@Override
	public String getTypeStr() {
		return "SAIFile";
	}

}
