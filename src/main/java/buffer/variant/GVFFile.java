package buffer.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import pipeline.Pipeline;

import buffer.FileBuffer;
import buffer.IntervalsFile;

/**
 * A file ostensibly formatted in GVF
 * @author brendan
 *
 */
public class GVFFile extends IntervalsFile {

	public GVFFile() {
	}
	
	public GVFFile(File file) {
		super(file);
	}

	@Override
	public String getTypeStr() {
		return "GVFFile";
	}

	@Override
	public void buildIntervalsMap() throws IOException {
		throw new IllegalStateException("Not implemented yet");
	}

	
	
}
