package buffer;

import java.io.File;

import org.w3c.dom.NodeList;

/**
 * Sample / practice buffer
 * @author brendan
 *
 */
public class TextBuffer extends FileBuffer {

	public TextBuffer() {
	}
	
	public TextBuffer(File file) {
		super(file);
	}

	@Override
	public String getTypeStr() {
		return "Text buffer";
	}


}
