package operator.writer;

import gene.Gene;

import java.io.IOException;
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
			VariantRec.GENOTYPE,
			VariantRec.VCF_POS,
			VariantRec.VCF_REF,
			VariantRec.VCF_ALT,
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
			//=========================================================
			//Exac columns, 32 of them. 4 for overall, and 4 for each of the 7 populations.
			VariantRec.EXAC63K_VERSION,
			
			VariantRec.EXAC63K_OVERALL_FREQ_HET,
			VariantRec.EXAC63K_OVERALL_FREQ_HOM,
			VariantRec.EXAC63K_OVERALL_HET_HOM,
			VariantRec.EXAC63K_OVERALL_FREQ_HEMI,
			
			VariantRec.EXAC63K_OVERALL_ALLELE_COUNT,
			VariantRec.EXAC63K_OVERALL_ALLELE_NUMBER,
			VariantRec.EXAC63K_OVERALL_HEMI_COUNT,
			VariantRec.EXAC63K_OVERALL_HOM_COUNT,
			VariantRec.EXAC63K_OVERALL_ALLELE_FREQ,
			//African
			VariantRec.EXAC63K_AFRICAN_ALLELE_COUNT,
			VariantRec.EXAC63K_AFRICAN_ALLELE_NUMBER,
			VariantRec.EXAC63K_AFRICAN_HEMI_COUNT,
			VariantRec.EXAC63K_AFRICAN_HOM_COUNT,
			VariantRec.EXAC63K_AFRICAN_ALLELE_FREQ,
			//Latino
			VariantRec.EXAC63K_LATINO_ALLELE_COUNT,
			VariantRec.EXAC63K_LATINO_ALLELE_NUMBER,
			VariantRec.EXAC63K_LATINO_HEMI_COUNT,
			VariantRec.EXAC63K_LATINO_HOM_COUNT,
			VariantRec.EXAC63K_LATINO_ALLELE_FREQ,
			//East Asian
			VariantRec.EXAC63K_EASTASIAN_ALLELE_COUNT,
			VariantRec.EXAC63K_EASTASIAN_ALLELE_NUMBER,
			VariantRec.EXAC63K_EASTASIAN_HEMI_COUNT,
			VariantRec.EXAC63K_EASTASIAN_HOM_COUNT,
			VariantRec.EXAC63K_EASTASIAN_ALLELE_FREQ,
			//Finnish
			VariantRec.EXAC63K_EUR_FINNISH_ALLELE_COUNT,
			VariantRec.EXAC63K_EUR_FINNISH_ALLELE_NUMBER,
			VariantRec.EXAC63K_EUR_FINNISH_HEMI_COUNT,
			VariantRec.EXAC63K_EUR_FINNISH_HOM_COUNT,
			VariantRec.EXAC63K_EUR_FINNISH_ALLELE_FREQ,
			//Non-Finnish Europeans
			VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_COUNT,
			VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_NUMBER,
			VariantRec.EXAC63K_EUR_NONFINNISH_HEMI_COUNT,
			VariantRec.EXAC63K_EUR_NONFINNISH_HOM_COUNT,
			VariantRec.EXAC63K_EUR_NONFINNISH_ALLELE_FREQ,
			//South Asian
			VariantRec.EXAC63K_SOUTHASIAN_ALLELE_COUNT,
			VariantRec.EXAC63K_SOUTHASIAN_ALLELE_NUMBER,
			VariantRec.EXAC63K_SOUTHASIAN_HEMI_COUNT,
			VariantRec.EXAC63K_SOUTHASIAN_HOM_COUNT,
			VariantRec.EXAC63K_SOUTHASIAN_ALLELE_FREQ,
			//Other populations
			VariantRec.EXAC63K_OTHER_ALLELE_COUNT,
			VariantRec.EXAC63K_OTHER_ALLELE_NUMBER,
			VariantRec.EXAC63K_OTHER_HEMI_COUNT,
			VariantRec.EXAC63K_OTHER_HOM_COUNT,
			VariantRec.EXAC63K_OTHER_ALLELE_FREQ,
			//=========================================================

			VariantRec.MITOMAP_FREQ,
			VariantRec.MITOMAP_ALLELE_ID,
			VariantRec.MITOMAP_DIS_CODING,
			VariantRec.POP_ALT,
			//added by for the scSNCannotator
			//VariantRec.scSNV_gene,
			VariantRec.scSNV_ada,
			VariantRec.scSNV_rf,
			VariantRec.CLNSIG,
			VariantRec.CLNDSDBID,
			VariantRec.CLNDSDB,
			VariantRec.CLNDBN,
			//added for snpEff use of arupBedFile
			//adds up to two additional annotations if multiple transcripts are defined for a bed file region
			VariantRec.CDOT2,
			VariantRec.PDOT2,
			VariantRec.EXON_NUMBER2,
			VariantRec.NM_NUMBER2,
			VariantRec.GENE_NAME2,
			VariantRec.VARIANT_TYPE2,
			VariantRec.CDOT3,
			VariantRec.PDOT3,
			VariantRec.EXON_NUMBER3,
			VariantRec.NM_NUMBER3,
			VariantRec.GENE_NAME3,
			VariantRec.VARIANT_TYPE3,
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

	@Override
	public void writeFooter(PrintStream outputStream) throws IOException {
		// TODO Auto-generated method stub
	}

}
