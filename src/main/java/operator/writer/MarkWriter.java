package operator.writer;

import java.io.PrintStream;

import operator.variant.MedDirWriter;
import buffer.variant.VariantRec;

/**
 * Variant writer for 
 * @author brendan
 *
 */
public class MarkWriter extends MedDirWriter {

	
	public MarkWriter() {
		//If you want variants to come in in a certain order you can set a Comparator to define
		//the order. This must happen before writeVariant() is called (of course), so the
		//constructor is a good place for it
		
		// this.setComparator(new SomeNewComparator());
	}
	
	@Override
	/**
	 * The header is written once to the outputstream before any variants are written
	 */
	public void writeHeader(PrintStream outputStream) {
		//Write header for file
	}
	
	/**
	 * For each variant in the pool this method is called once, giving this writer a chance
	 * to emit variant info the outputstream provided
	 */
	public void writeVariant(VariantRec rec, PrintStream outputStream) {
		
		//Nice to build a string, then write the string at the end of the method...
		StringBuilder builder = new StringBuilder();
		
		
		String name = rec.getAnnotation(VariantRec.GENE_NAME);
		String geneHyperlinkText = createGeneHyperlink(name); //Make a link to NCBI's gene page for this gene 
		
		builder.append( geneHyperlinkText );
		
		//A few pieces of information about variants are hard-coded into the VariantRec class
		//such as ref and alt alleles, and contig and position
		String refAllele = rec.getRef();
		String altAllele = rec.getAlt();
		String contig = rec.getContig();
		Integer pos = rec.getStart();
		
		
		builder.append("\t" + contig + "\t" + pos + "\t" + refAllele + "\t" + altAllele);
		
		
		
		
		//write string to stream
		outputStream.println( builder.toString() );
	}
}
