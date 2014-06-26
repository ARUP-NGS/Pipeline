package buffer;

import java.io.File;

public class AnnovarInputFile extends FileBuffer {

	public AnnovarInputFile() {
		//blank on purpose
	}
	
	public AnnovarInputFile(File file) {
		super(file);
	}
	
	
	@Override
	public String getTypeStr() {
		return "AnnovarInput";
	}

}
