package buffer;

import java.io.File;

/**
 * A file type that cannot / has not been guessed by file type guesser
 * @author brendan
 *
 */
public class UnknownFileBuffer extends FileBuffer {

	public UnknownFileBuffer(File file) {
		super(file);
	}
	
	@Override
	public String getTypeStr() {
		return "Unknown type";
	}

}
