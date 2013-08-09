package operator.variant;

import gene.Gene;

import java.io.PrintStream;
import java.util.Comparator;

import buffer.variant.VariantRec;

/**
 * Writes a variant pool in a nifty format designed to work well with microsoft excel
 * @author brendan
 *
 */
public class ExcelWriter extends VariantPoolWriter {

	public final static String[] keys = new String[]{VariantRec.GENE_NAME,
								 VariantRec.NM_NUMBER,
								 VariantRec.CDOT,
								 VariantRec.PDOT,
								 VariantRec.EXON_NUMBER,
								 VariantRec.VARIANT_TYPE, 
								 VariantRec.EXON_FUNCTION,
								 VariantRec.SVM_EFFECT,
								 //VariantRec.INTERACTION_SCORE,
								 //VariantRec.GO_EFFECT_PROD,
								 VariantRec.EFFECT_RELEVANCE_PRODUCT,
								 VariantRec.POP_FREQUENCY,
								 VariantRec.AMR_FREQUENCY,
								 VariantRec.EUR_FREQUENCY,
								 VariantRec.AFR_FREQUENCY,
								 VariantRec.EXOMES_FREQ,
								 VariantRec.ARUP_TOT,
								 VariantRec.ARUP_FREQ,
								 VariantRec.VQSR,
								 //VariantRec.CG69_FREQUENCY,
								 //VariantRec.VARBIN_BIN,
								 VariantRec.RSNUM, 
								 VariantRec.SIFT_SCORE,
								 VariantRec.POLYPHEN_SCORE,
								 VariantRec.MT_SCORE,
								 VariantRec.GERP_SCORE,
								 VariantRec.PHYLOP_SCORE,
								 VariantRec.MA_SCORE
								 //VariantRec.OMIM_ID,
								 //VariantRec.HGMD_HIT,
								 //VariantRec.CLINVAR_TYPE,
								 //VariantRec.CLINVAR_VALIDATED
								 };
	
	public ExcelWriter() {
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		this.setComparator(new EffectProdSorter());
	}
	
	public static String[] geneKeys = {
		Gene.HGMD_INFO,
		Gene.GENE_RELEVANCE,
//		Gene.OMIM_DISEASES,
		Gene.OMIM_NUMBERS,
//		Gene.OMIM_INHERITANCE,
//		Gene.OMIM_PHENOTYPES
		Gene.SUMMARY_SCORE, 
		Gene.PUBMED_SCORE, 
		Gene.INTERACTION_SCORE, 
		Gene.DBNSFPGENE_SCORE,
		Gene.GO_SCORE,
		Gene.OMIM_PHENOTYPE_SCORE,
		Gene.DBNSFP_MIMDISEASE, 
//		Gene.DBNSFP_DISEASEDESC, 
//		Gene.PUBMED_HIT
		};
	
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
	 * Compares variants by their EFFECT_RELEVANCE_PRODUCT property
	 * @author brendan
	 *
	 */
	class EffectProdSorter implements Comparator<VariantRec> {

		@Override
		public int compare(VariantRec v0, VariantRec v1) {
			Double s0 = v0.getProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT);
			Double s1 = v1.getProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT);
			
			if (s0 == null && s1 == null)
				return 0;
			if (s0 == null && s1 != null) {
				return -1;
			}
			if (s0 != null && s1 == null) {
				return 1;
			}
			
			return s0 > s1 ? -1 : 1;
			
		}
		
	}

}
