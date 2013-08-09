package operator.writer;

import gene.Gene;

import java.io.PrintStream;

import operator.variant.MedDirWriter;
import buffer.variant.VariantRec;

/**
 * Variant writer for 
 * @author brendan
 *
 */
public class AmpliseqWriter extends MedDirWriter {

	public final static String[] defaultKeys = new String[]{
		VariantRec.GENE_NAME,
		VariantRec.EXON_NUMBER,
		VariantRec.CDOT,
		VariantRec.PDOT,
		VariantRec.VARIANT_TYPE, 
		VariantRec.EXON_FUNCTION,
		VariantRec.POP_FREQUENCY,
		VariantRec.EXOMES_FREQ,
		VariantRec.RSNUM, 
		VariantRec.HGMD_HIT};

	public AmpliseqWriter() {
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
		StringBuilder builder = new StringBuilder();
		String[] keys = getKeys();
		builder.append(keys[0]);
		for(int i=1; i<keys.length; i++) {
			builder.append("\t" + keys[i]);
		}
		builder.append("\tchrom\tpos\tref\talt\tnm.number");
		outputStream.println(builder.toString());
	}
	
	private String[] getKeys(){
		return defaultKeys;
	}
	
	/**
	 * For each variant in the pool this method is called once, giving this writer a chance
	 * to emit variant info the outputstream provided
	 */
	public void writeVariant(VariantRec rec, PrintStream outputStream) {

		StringBuilder builder = new StringBuilder();

//		Gene g = rec.getGene();
//		if (g == null && genes != null) {
//			String geneName = rec.getAnnotation(VariantRec.GENE_NAME);
//			if (geneName != null)
//				g = genes.getGeneByName(geneName);
//		}
		
		String[] keys = getKeys();
		String refAllele = rec.getRef();
		String chrom = rec.getContig();
		Integer pos = rec.getStart();
		String val;

		String[] altAlleles = rec.getAllAlts();
		for(String alt : altAlleles){
			builder.setLength(0);
			builder.append( createGeneHyperlink(rec.getAnnotation(VariantRec.GENE_NAME)) );
//			builder.append("," + chrom + "," + pos + "," + refAllele + "," + alt);

			for(int i=1; i<keys.length; i++) {
				val = rec.getPropertyOrAnnotation(keys[i]).trim();
				if(val == null){
					val = "-";
				}
				builder.append("\t" + val);
			}
			builder.append("\t" + chrom + "\t" + pos + "\t" + refAllele + "\t" + alt + "\t" + rec.getAnnotation(VariantRec.NM_NUMBER));
			//write string to stream
			outputStream.println( builder.toString() );
		}
	}
}
