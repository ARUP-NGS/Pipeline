package buffer;

import java.io.File;

public class FastQFile extends FileBuffer {

	
	public FastQFile() {
	}
	
	public FastQFile(File file) {
		super(file);
	}

	@Override
	public String getTypeStr() {
		return "FastQ";
	}

}
