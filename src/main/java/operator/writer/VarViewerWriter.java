package operator.writer;

import gene.Gene;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import operator.variant.VariantPoolWriter;
import buffer.variant.VariantRec;

/**
 * Emits a variant pool in varviewer-friendly form. 
 * @author brendan
 *
 */
public class VarViewerWriter extends VariantPoolWriter {

	public final static List<String> keys = new ArrayList<String>( Arrays.asList(new String[]{
			VariantRec.GENE_NAME,
			 VariantRec.NM_NUMBER,
			 VariantRec.CDOT,
			 VariantRec.PDOT,
			 VariantRec.DEPTH,
			 VariantRec.EXON_NUMBER,
			 VariantRec.VARIANT_TYPE, 
			 VariantRec.EXON_FUNCTION,
			 VariantRec.POP_FREQUENCY,
			 VariantRec.AMR_FREQUENCY,
			 VariantRec.EXOMES_FREQ,
			 VariantRec.EXOMES_HOM_FREQ,
			 VariantRec.RSNUM,
			 VariantRec.ARUP_FREQ,
			 VariantRec.ARUP_OVERALL_FREQ,
			 VariantRec.ARUP_FREQ_DETAILS,
			 VariantRec.VARBIN_BIN,
			 VariantRec.SVM_EFFECT,
			 VariantRec.SIFT_SCORE, 
			 VariantRec.POLYPHEN_SCORE, 
			 VariantRec.PHYLOP_SCORE, 
			 VariantRec.MT_SCORE,
			 VariantRec.GERP_SCORE,
			 VariantRec.LRT_SCORE,
			 VariantRec.SIPHY_SCORE,
			 VariantRec.MA_SCORE,
			 VariantRec.HGMD_HIT,
			 VariantRec.HALOPLEX_PANEL_FREQ,
			 VariantRec.SPLICING_ALL,
			 VariantRec.SPLICING_TOPNM,
			 VariantRec.SPLICING_TOPNMDIFF,
			 VariantRec.NON_PREFERRED_TRANSCRIPT,
			 VariantRec.EXOMES_AA_HET,
			 VariantRec.EXOMES_AA_HOMALT,
			 VariantRec.EXOMES_AA_HOMREF,
			 VariantRec.EXOMES_EA_HET,
			 VariantRec.EXOMES_EA_HOMALT,
			 VariantRec.EXOMES_EA_HOMREF,
			 VariantRec.PFAM_AC,
			 VariantRec.PFAM_ID,
			 VariantRec.PFAM_DESC,
			 VariantRec.SCOP_DOMAIN,
			 VariantRec.COSMIC_ID,
			 VariantRec.COSMIC_COUNT,
			 VariantRec.UK10K_ALLELE_FREQ,
			 VariantRec.EXOMES_63K_FREQ,
			 VariantRec.EXOMES_63K_AFR_FREQ,
			 VariantRec.EXOMES_63K_AMR_FREQ,
			 VariantRec.EXOMES_63K_EAS_FREQ,
			 VariantRec.EXOMES_63K_FIN_FREQ,
			 VariantRec.EXOMES_63K_NFE_FREQ,
			 VariantRec.EXOMES_63K_OTH_FREQ,
			 VariantRec.EXOMES_63K_SAS_FREQ,
			 VariantRec.EXOMES_63K_OTH_FREQ,	
			 VariantRec.EXOMES_63K_AC_HET,
			 VariantRec.EXOMES_63K_AFR_HET,
			 VariantRec.EXOMES_63K_AMR_HET,
			 VariantRec.EXOMES_63K_EAS_HET,
			 VariantRec.EXOMES_63K_FIN_HET,
			 VariantRec.EXOMES_63K_NFE_HET,
			 VariantRec.EXOMES_63K_OTH_HET,
			 VariantRec.EXOMES_63K_SAS_HET,
			 VariantRec.EXOMES_63K_OTH_HET,
			 VariantRec.EXOMES_63K_AC_HOM,
			 VariantRec.EXOMES_63K_AFR_HOM,
			 VariantRec.EXOMES_63K_AMR_HOM,
			 VariantRec.EXOMES_63K_EAS_HOM,
			 VariantRec.EXOMES_63K_FIN_HOM,
			 VariantRec.EXOMES_63K_NFE_HOM,
			 VariantRec.EXOMES_63K_SAS_HOM,
			 VariantRec.EXOMES_63K_OTH_HOM,
			 VariantRec.MITOMAP_FREQ,
			 VariantRec.MITOMAP_ALLELE_ID
			 }));
	
	public final static List<String> geneKeys = new ArrayList<String>( Arrays.asList(new String[]{
	 Gene.OMIM_DISEASES,
	 Gene.OMIM_NUMBERS,
	 Gene.OMIM_INHERITANCE,
	 Gene.HGMD_INFO}));
	
	@Override
	public void writeHeader(PrintStream outputStream) {
		StringBuilder builder = new StringBuilder();
		builder.append(VariantRec.getSimpleHeader());
		for(int i=0; i<keys.size(); i++) {
			builder.append("\t " + keys.get(i));
		}
		
		for(int i=0; i<geneKeys.size(); i++) {
			builder.append("\t " + geneKeys.get(i));
		}

		outputStream.println(builder.toString());
	}

	@Override
	public void writeVariant(VariantRec rec, PrintStream outputStream) {
		StringBuilder builder = new StringBuilder();
		builder.append(rec.toSimpleString());
		for(int i=0; i<keys.size(); i++) {
			String val = rec.getPropertyOrAnnotation(keys.get(i)).trim();
			
			if (keys.get(i).equals(VariantRec.HGMD_HIT) && val.length() > 5) {
				val = "true";				
			}
			
			if (keys.get(i).equals(VariantRec.GENE_NAME) && val.contains("(")) {
				val = val.substring(0, val.indexOf("("));
			}
			
			builder.append("\t" + val);
		}
		
		for(int i=0; i<geneKeys.size(); i++) {
			Gene g = rec.getGene();
			if (g == null) {
				String geneName = rec.getAnnotation(VariantRec.GENE_NAME);
				if (geneName != null && genes != null)
					g = genes.getGeneByName(geneName);
			}
			
			String val = "-";
			if (g != null) {
				val = g.getPropertyOrAnnotation(geneKeys.get(i)).trim();
			}
			
			//Special case, if HGMD_INFO, just emit "true" if there is anything
			if (geneKeys.get(i).equals(Gene.HGMD_INFO) && val.length() > 5) {
				val = "true";
			}
			
			builder.append("\t" + val);
		}
		
		
		outputStream.println(builder.toString());
	}

}
