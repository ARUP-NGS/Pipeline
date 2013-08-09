package operator.writer;

import gene.Gene;

import java.io.PrintStream;
import java.util.Comparator;

import operator.variant.VariantPoolWriter;
import buffer.variant.VariantRec;

public class GeneRankWriter extends VariantPoolWriter {

	public final static String[] keys = new String[]{VariantRec.GENE_NAME,
		 VariantRec.NM_NUMBER,
		 VariantRec.CDOT,
		 VariantRec.PDOT,
		 VariantRec.EXON_NUMBER,
		 VariantRec.VARIANT_TYPE, 
		 VariantRec.EXON_FUNCTION,
		 VariantRec.POP_FREQUENCY,
		 VariantRec.EXOMES_FREQ,
		 VariantRec.RSNUM, 
		 VariantRec.HGMD_HIT,
		 VariantRec.EFFECT_RELEVANCE_PRODUCT,
		 "87253-mom",
		 "87252-mfs2",
		 "87969-orig",
		 "87614-grandkid",
		 VariantRec.EFFECT_PREDICTION2,
		 VariantRec.SIFT_SCORE, 
		 VariantRec.POLYPHEN_SCORE, 
		 VariantRec.PHYLOP_SCORE, 
		 VariantRec.MT_SCORE,
		 VariantRec.GERP_SCORE};

	public static String[] geneKeys = {
		Gene.HGMD_INFO,
		Gene.GENE_RELEVANCE,
		Gene.SUMMARY_SCORE, 
		Gene.PUBMED_SCORE, 
		//Gene.INTERACTION_SCORE, 
		//Gene.DBNSFP_MIMDISEASE, 
		//Gene.DBNSFP_DISEASEDESC, 
		Gene.PUBMED_HIT
	};

	public GeneRankWriter() {
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		this.setComparator(new GeneRankSorter());
	}
	
	@Override
	public void writeHeader(PrintStream outputStream) {
		StringBuilder builder = new StringBuilder();
		builder.append(VariantRec.getSimpleHeader());
		for(int i=0; i<keys.length; i++) {
			builder.append("\t " + keys[i]);
		}

		for(int i=0; i<geneKeys.length; i++) {
			builder.append("\t " + geneKeys[i]);
		}

		outputStream.println(builder.toString());
	}

	@Override
	public void writeVariant(VariantRec rec, PrintStream outputStream) {
		StringBuilder builder = new StringBuilder();
		builder.append(rec.toSimpleString());
		for(int i=0; i<keys.length; i++) {
			String val = rec.getPropertyOrAnnotation(keys[i]).trim();
			
			if (keys[i].equals("87253-mom")) {
				VariantRec var = additionalVariants.get(0).findRecordNoWarn(rec.getContig(), rec.getStart());
				if (var == null)
					val = "ref";
				else {
					if (var.isHetero()) {
						val = "het";
					}
					else {
						val = "hom";
					}
				}
			}
			if (keys[i].equals( "87252-mfs2")) {
				VariantRec var = additionalVariants.get(1).findRecordNoWarn(rec.getContig(), rec.getStart());
				if (var == null)
					val = "ref";
				else {
					if (var.isHetero()) {
						val = "het";
					}
					else {
						val = "hom";
					}
				}
			}
			if (keys[i].equals("87969-orig")) {
				VariantRec var = additionalVariants.get(2).findRecordNoWarn(rec.getContig(), rec.getStart());
				if (var == null)
					val = "ref";
				else {
					if (var.isHetero()) {
						val = "het";
					}
					else {
						val = "hom";
					}
				}
			}
			if (keys[i].equals("87614-grandkid")) {
				VariantRec var = additionalVariants.get(3).findRecordNoWarn(rec.getContig(), rec.getStart());
				if (var == null)
					val = "ref";
				else {
					if (var.isHetero()) {
						val = "het";
					}
					else {
						val = "hom";
					}
				}
			}
						
			builder.append("\t" + val);
		}
		
		for(int i=0; i<geneKeys.length; i++) {
			Gene g = rec.getGene();
			if (g == null) {
				String geneName = rec.getAnnotation(VariantRec.GENE_NAME);
				if (geneName != null && genes != null)
					g = genes.getGeneByName(geneName);
			}
			
			String val = "-";
			if (g != null) {
				val = g.getPropertyOrAnnotation(geneKeys[i]).trim();
			}
			
			builder.append("\t" + val);
		}
		
		
		outputStream.println(builder.toString());
	}

	
	/**
	 * Compares variants by their gene's GENE_RELEVANCE property
	 * @author brendan
	 *
	 */
	class GeneRankSorter implements Comparator<VariantRec> {

		@Override
		public int compare(VariantRec v0, VariantRec v1) {
			
			Double s0 = v0.getProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT);
			Double s1 = v1.getProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT);
			
			if (s0 == null || Double.isNaN(s0))
				s0 = 0.0;
			if (s1 == null || Double.isNaN(s1))
				s1 = 0.0;
			
			if (s0 == s1) {
				return 0;
			}
			
			return s0 < s1 ? 1 : -1;
		}
		
	}
}
