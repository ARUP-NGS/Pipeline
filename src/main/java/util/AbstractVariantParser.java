package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import buffer.variant.VariantLineReader;

public abstract class AbstractVariantParser implements VariantLineReader {

	protected BufferedReader reader = null;
	protected String currentLine = null;
	
	public AbstractVariantParser(InputStream stream) throws IOException {
		reader = new BufferedReader(new InputStreamReader(stream));
		currentLine = reader.readLine();
	}
	
	public AbstractVariantParser(BufferedReader reader) throws IOException {
		this.reader = reader;
		currentLine = reader.readLine();
	}
	
	public AbstractVariantParser(File file) throws IOException {
		this.reader = new BufferedReader(new FileReader(file));
		currentLine = reader.readLine();
	}
	
	@Override
	public boolean advanceLine() throws IOException {
		currentLine = reader.readLine();
		return currentLine != null;
	}


}
