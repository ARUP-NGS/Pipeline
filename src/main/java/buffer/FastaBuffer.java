package buffer;

import java.io.File;

public class FastaBuffer extends FileBuffer {

	
	public FastaBuffer() {
	}
	
	public FastaBuffer(File file) {
		super(file);
	}

	@Override
	public String getTypeStr() {
		return "Fasta file";
	}

}
