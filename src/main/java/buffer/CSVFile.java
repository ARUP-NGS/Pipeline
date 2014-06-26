package buffer;

import java.io.File;

public class CSVFile extends FileBuffer {
	
	public CSVFile() {
	}
	
	public CSVFile(File file) {
		super(file);
	}

	@Override
	public String getTypeStr() {
		return "CSVFile";
	}

	
}
