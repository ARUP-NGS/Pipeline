package operator.examples;

import java.io.IOException;
import java.io.PrintStream;

import operator.variant.VariantPoolWriter;
import buffer.variant.VariantRec;

public class ExampleWriter extends VariantPoolWriter {

	@Override
	/**
	 * Write an informative header...
	 */
	public void writeHeader(PrintStream outputStream) {
		outputStream.println("# Here's my header, with some comments");
		outputStream.println("# If this is a csv file, then include some column headers...");
		outputStream.println("chr,pos,random");
	}

	@Override
	/**
	 * Write out each variant to the output stream...
	 */
	public void writeVariant(VariantRec rec, PrintStream outputStream) {
		outputStream.println(rec.getContig() + ", " + rec.getStart() + ", " + rec.getProperty("random.property") );
	}

	@Override
	public void writeFooter(PrintStream outputStream) throws IOException {
		//Don't need to do anything here
	}

}
