package operator.writer;

import gene.Gene;

import java.io.PrintStream;

import buffer.variant.VariantRec;

/**
 * Write basic information about variants and HGMD hits to a file
 * @author brendan
 *
 */
public class HGMDHitsEmitter extends MedDirTrioWriter {

	public final static String[] theseKeys = new String[]{
		VariantRec.GENE_NAME,
		 VariantRec.NM_NUMBER,
		 VariantRec.CDOT,
		 VariantRec.PDOT,
		 "zygosity",
		 "chrom",
		 "pos",
		 VariantRec.DEPTH,
		 "quality",
		 VariantRec.VARIANT_TYPE, 
		 VariantRec.EXON_FUNCTION,
		 "parent.1.zygosity",
		 "parent.2.zygosity",
		 VariantRec.RSNUM,
		 VariantRec.HGMD_HIT,
		 Gene.GENE_RELEVANCE};
	
	
	public String[] getKeys() {
		return theseKeys;
	}

	
	@Override
	public void writeVariant(VariantRec rec, PrintStream outputStream) {
		if (rec.getAnnotation(VariantRec.HGMD_HIT) != null 
				&& rec.getAnnotation(VariantRec.HGMD_HIT).length() > 2) {
			super.writeVariant(rec, outputStream);
		}
		//if (rec.getGene() != null 
		//		&& rec.getGene().getAnnotation(Gene.HGMD_INFO) != null 
		//		&& rec.getGene().getAnnotation(Gene.HGMD_INFO).length()>1) {
		//	super.writeVariant(rec, outputStream);
		//}
	}

}
