package util;

import java.io.File;
import java.io.IOException;

import buffer.variant.VariantLineReader;
import buffer.variant.VariantRec;

public class TZTLineReader extends AbstractVariantParser {

	

	public TZTLineReader(File file) throws IOException {
		super(file);
	}

	@Override
	public VariantRec toVariantRec() {
		if (currentLine == null)
			return null;
		String[] toks = currentLine.split("\t");
				
		String contig = toks[1].replace("chr", "");
		int startPos = Integer.parseInt(toks[2]);
		int endPos = Integer.parseInt(toks[3]);
		String ref = "-";
		String alt = toks[4];
		if (alt.contains("/"))
			alt = alt.substring(0, alt.indexOf("/"));
		
		
		VariantRec rec = new VariantRec(contig, startPos, endPos, ref, alt);
		return rec;
	}

	
	public static void main(String[] args) throws IOException {
		
		File inFile = new File(args[0]);
		VariantLineReader reader = new TZTLineReader(inFile);
		VariantRec var = reader.toVariantRec();
		System.out.println(VariantRec.getSimpleHeader());
		while(var != null) {
			System.out.println(var.toSimpleString());
			reader.advanceLine();
			var = reader.toVariantRec();
		}
	}

	@Override
	public String getCurrentLine() throws IOException {
		return currentLine;
	}

	@Override
	public String getHeader() throws IOException {
		return null;
	}
}
