package operator.pindel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class PindelFolderFilter {
	private enum outFiles {
		_D, _LI, _TD, _SI,
		// _INV
	}
	
	private PindelParser parser;

	public PindelFolderFilter(String prefix, int threshold, String reference,
			String pindelAddress) throws IOException {
		

		File inv = new File(prefix + "_INV");
		File inv2 = new File(prefix + "2_INV");
		copyFileUsingStreams(inv, inv2); // INV has a different format that
												// we aren't processing right
												// now, so we simply copy the
												// file
		

		for (outFiles currentFile : outFiles.values()) {
			File thisFile = new File(prefix + currentFile);
			File filteredFile = new File(prefix + "2" + currentFile);
			if (thisFile.exists()) {
				if (thisFile.length() > 0) {
					System.out.println("processing " + prefix + currentFile);
					parser = new PindelParser(thisFile);
					parser.filter(threshold);
					System.out.println(parser.printPINDEL());
					parser.makePindelFile(filteredFile);
					parser.makeVCF(prefix + "2", reference, pindelAddress);
					parser.combineResults();
				} else {
					System.out.println("file size 0 " + prefix + currentFile);
				}
			} else {
				System.out.println("failed to find " + prefix + currentFile);
			}
		}
	}

	public List<PindelResult> getPindelResults() {
		return parser.getResults();
	}
	
	private static void copyFileUsingStreams(File source, File dest)
			throws IOException {
		InputStream input = null;
		OutputStream output = null;
		try {
			input = new FileInputStream(source);
			output = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buf)) > 0) {
				output.write(buf, 0, bytesRead);
			}
		} finally {
			input.close();
			output.close();
		}
	}
}