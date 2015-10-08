package buffer;

import java.io.File;

/**
 * A BEDGRAPH file is just like a BED file but has a few extra columns. 
 * @author brendan
 *
 */
public class BEDGraphFile extends BEDFile {

	public BEDGraphFile() {
		super();
	}

	public BEDGraphFile(File file) {
		super(file);
		if (! file.getName().endsWith("bedgraph")) {
			throw new IllegalArgumentException("BEDGraph files must have the suffix .bedgraph");
		}
	}
	
	@Override
	public String getTypeStr() {
		return "BEDGraphFile";
	}
	
}
