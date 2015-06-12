package operator.variant;

import java.io.IOException;
import operator.OperationFailedException;
import org.broad.tribble.readers.TabixReader;
import buffer.variant.VariantRec;
import util.vcfParser.VCFParser;

/**
 * Uses a tabix-index file.
 *
 * Tabix file is created from a db [tab delimited] obtained form [ftp://dbnsfp:dbnsfp@dbnsfp.softgenetics.com/dbNSFPv3.0b2c.zip]
 * the file [ftp://dbnsfp:dbnsfp@dbnsfp.softgenetics.com/dbscSNV.zip] dbscSNv.zip unpacks
 * to 24 files (chr 1 - 22, X, Y).  These files are cat together with the bash command below.
 *
 * for i in {1..22} X Y; do tail -n +2 dbNSFP3.01bc_variant.chr${i} >> dbNSFP3.0b1c_2015_04_13.tab ; done
 * #tail to remove the header
 *
 *
 0 chr
 1 pos(1-based)
 2 ref
 3 alt
 4 aaref
 5 aaalt
 6 rs_dbSNP142
 7 hg19_chr
 8 hg19_pos(1-based)
 9 hg18_chr
 10 hg18_pos(1-based)
 11 genename
 12 cds_strand
 13 refcodon
 14 codonpos
 15 codon_degeneracy
 16 Ancestral_allele
 17 AltaiNeandertal
 18 Denisova
 19 Ensembl_geneid
 20 Ensembl_transcriptid
 21 Ensembl_proteinid
 22 aapos
 *23 SIFT_score <----- SIFT
 * 		 SIFT_score: SIFT score (SIFTori). Scores range from 0 to 1. The smaller the score the
 more likely the SNP has damaging effect.
 Multiple scores separated by ";", corresponding to Ensembl_proteinid.

 *
 24 SIFT_converted_rankscore
 25 SIFT_pred
 26 Uniprot_acc_Polyphen2
 27 Uniprot_id_Polyphen2
 28 Uniprot_aapos_Polyphen2
 *29 Polyphen2_HDIV_score  <---- PP
 *
 *
 *
 30 Polyphen2_HDIV_rankscore
 31 Polyphen2_HDIV_pred
 *32 Polyphen2_HVAR_score <---- PP_HVAR
 33 Polyphen2_HVAR_rankscore
 34 Polyphen2_HVAR_pred
 *35 LRT_score  <---- LRT
 36 LRT_converted_rankscore
 37 LRT_pred
 38 LRT_Omega
 *39 MutationTaster_score <------ MT
 40 MutationTaster_converted_rankscore
 41 MutationTaster_pred
 42 MutationTaster_model
 43 MutationTaster_AAE
 44 Uniprot_id_MutationAssessor
 45 Uniprot_variant_MutationAssessor
 *46 MutationAssessor_score  <----- MA
 47 MutationAssessor_rankscore
 48 MutationAssessor_pred
 49 FATHMM_score
 50 FATHMM_converted_rankscore
 51 FATHMM_pred
 52 PROVEAN_score
 53 PROVEAN_converted_rankscore
 54 PROVEAN_pred
 55 MetaSVM_score
 56 MetaSVM_rankscore
 57 MetaSVM_pred
 58 MetaLR_score
 59 MetaLR_rankscore
 60 MetaLR_pred
 61 Reliability_index
 *62 GERP++_NR -----> GERP_NR
 *63 GERP++_RS -----> GERP
 64 GERP++_RS_rankscore
 *65 phyloP7way_vertebrate  <----- PHYLOP
 66 phyloP7way_vertebrate_rankscore
 67 phastCons7way_vertebrate
 68 phastCons7way_vertebrate_rankscore
 69 SiPhy_29way_pi
 *70 SiPhy_29way_logOdds  <----- SIPHY
 71 SiPhy_29way_logOdds_rankscore
 72 1000Gp3_AC
 *73 1000Gp3_AF   <---- TKG
 74 1000Gp3_AFR_AC
 *75 1000Gp3_AFR_AF  <----TKG_AFR
 76 1000Gp3_EUR_AC
 *77 1000Gp3_EUR_AF  <-----TKG_EUR
 78 1000Gp3_AMR_AC
 *79 1000Gp3_AMR_AF   <-----TKG_AMR
 80 1000Gp3_EAS_AC
 81 1000Gp3_EAS_AF   <-----TKG_ASN
 82 1000Gp3_SAS_AC
 83 1000Gp3_SAS_AF
 84 TWINSUK_AC
 85 TWINSUK_AF
 86 ALSPAC_AC
 87 ALSPAC_AF
 88 ESP6500_AA_AC
 89 ESP6500_AA_AF
 90 ESP6500_EA_AC
 91 ESP6500_EA_AF
 92 ExAC_AC
 93 ExAC_AF
 94 ExAC_Adj_AC
 95 ExAC_Adj_AF
 96 ExAC_AFR_AC
 97 ExAC_AFR_AF
 98 ExAC_AMR_AC
 99 ExAC_AMR_AF
 100 ExAC_EAS_AC
 101 ExAC_EAS_AF
 102 ExAC_FIN_AC
 103 ExAC_FIN_AF
 104 ExAC_NFE_AC
 105 ExAC_NFE_AF
 106 ExAC_SAS_AC
 107 ExAC_SAS_AF
 108 clinvar_rs
 109 clinvar_clnsig
 110 clinvar_trait
 111 Interpro_domain
 *
 *
 * this cat'd file is tabix index using the following commands
 * bgzip -c dbNSFP3.0b1c_2015_04_13.tab > dbNSFP3.0b1c_2015_04_13.tab.gz
 * tabix -s 1 -b 2 -e 2 dbNSFP3.0b1c_2015_04_13.tab.gz
 *
 * the MD5 for the files are:
 * 	dbNSFP3.0b1c_2015_04_13.tab.gz
 * 	dbscSNV_cat_1-22XY.tab.bgz.tbi 688K Feb 24
 *
 *
 * TESTING
 * A truncated database was utilized; see TestDBNSFP for details.
 *
 * @author Keith Simmon
 * @date April 13th 2015
 *
 */
public class DBNSFPAnnotator extends AbstractTabixAnnotator {

	private boolean initialized = false;
	private TabixReader reader = null;

	public static final String DBNSFP_PATH = "dbnsfp.path";


	@Override
	protected String getPathToTabixedFile() {
		return searchForAttribute(DBNSFP_PATH);
	}

		protected void initializeReader(String filePath) {

			try {
				reader = new TabixReader(filePath);
			} catch (IOException e) {
				throw new IllegalArgumentException("Error opening data at path " + filePath + " error : " + e.getMessage());
			}
			initialized = true;
	}

	/**
	 * When the variant exists in the dbNSFP database annotation properties are added to the variant
	 *
	 * The current annotations added include:
	 *
	 * SIFT_SCORE [column 23]
	 * 	SIFT_score: SIFT score (SIFTori). Scores range from 0 to 1. The smaller the score the more likely the SNP has
	 * 	damaging effect. Multiple scores separated by ";", corresponding to Ensembl_proteinid.
	 * 	NOTE: if multiple values exist the LOWEST score is added to the variant
	 *
	 * POLYPHEN_SCORE [column 29]
	 * 	Polyphen2_HDIV_score: Polyphen2 score based on HumDiv, i.e. hdiv_prob. The score ranges from 0 to 1. Multiple
	 * 	entries separated by ";", corresponding to Uniprot_acc_Polyphen2.
	 *	NOTE: if multiple values exist the HIGHEST score is added to the variant
	 *
	 * POLYPHEN_HVAR_SCORE [column 32]
	 * 	Polyphen2_HVAR_score: Polyphen2 score based on HumVar, i.e. hvar_prob. The score ranges from 0 to 1. Multiple
	 * 	entries separated by ";", corresponding to Uniprot_acc_Polyphen2.
	 * 	NOTE: if multiple values exist the HIGHEST score is added to the variant
	 *
	 * LRT_score [column 35]
	 * 	LRT_score: The original LRT two-sided p-value (LRTori), ranges from 0 to 1.
	 *
	 * MT_SCORE [column 39]
	 * 	MutationTaster_score: MutationTaster p-value (MTori), ranges from 0 to 1.
	 * 	NOTE: if multiple values exist the HIGHEST score is added to the variant
	 *
	 * MA_SCORE [column 46]
	 * 	MutationAssessor_score: MutationAssessor functional impact combined score (MAori). The score ranges from -5.545
	 * 	to 5.975 in dbNSFP.
	 * 	NOTE: if multiple values exist the HIGHEST score is added to the variant
	 *
	 * GERP_NR_SCORE [column 62]
	 * 	GERP++_NR: GERP++ neutral rate
	 *
	 * GERP_SCORE [column 63]
	 * 	GERP++_RS: GERP++ RS score, the larger the score, the more conserved the site.
	 *
	 * PHYLOP_SCORE
	 *  phyloP7way_vertebrate: phyloP (phylogenetic p-values) conservation score based on the multiple alignments of 7
	 *  vertebrate genomes (including human). The larger the score, the more conserved the site.
	 *
	 * SIPHY_SCORE
	 * 	SiPhy_29way_logOdds: SiPhy score based on 29 mammals genomes. The larger the score, the more conserved the site.
	 *
	 * POP_FREQUENCY
	 * 	1000Gp3_AF: Alternative allele frequency in the whole 1000Gp3 data.
	 *
	 * AFR_FREQUENCY
	 * 	1000Gp3_AFR_AF: Alternative allele frequency in the 1000Gp3 African descendent samples.
	 *
	 * EUR_FREQUENCY
	 * 	1000Gp3_EUR_AF: Alternative allele frequency in the 1000Gp3 European descendent samples.
	 *
	 * AMR_FREQUENCY
	 *	1000Gp3_AMR_AF: Alternative allele frequency in the 1000Gp3 American descendent samples.
	 *
	 * ASN_FREQUENCY
	 * 	1000Gp3_EAS_AF: Alternative allele frequency in the 1000Gp3 East Asian descendent samples.
	 *
	 * EXOMES_FREQ
	 * 	ExAC_AF: Allele frequency in total ExAC samples
	 *
	 * 	*THIS VALUE NO LONGER EXISTS  *********************************************
	 *	*	Double slr = reader.getValue(DBNSFPReader.SLR_TEST); //deprecated     *
	 *	*	var.addProperty(VariantRec.SLR_TEST, slr);						      *
	 *	***************************************************************************
	 *
	 * @param var the variant record to be annotated
	 * @param val the string (line) retrieved by tabix
	 * @return true when annotations are added
	 */
	@Override
	protected boolean addAnnotationsFromString(VariantRec var, String val) {
		String[] toks = val.split("\t");
		int sift_score_col = 23;
		int polyphen_score_col = 29;
		int Polyphen2_hvar_score_col = 32;
		int lrt_score_column = 35;
		int mt_score_column = 39;
		int ma_score_column = 46;
		int gerp_nr_score_column = 62;
		int gerp_score_column = 63;
		int phylop_score_column = 65;
		int siphy_score_column = 70;
		int pop_frequency_column = 73;
		int afr_frequency_column = 75;
		int eur_frequency_column = 77;
		int amr_frequency_column = 79;
		int asn_frequency_column = 81;
		int exomes_freq_column = 91;


		//SIFT_SCORE

		try {
			if (toks[sift_score_col].contains(";")) {
				String [] values = toks[sift_score_col].split(";");
				double lowest = 2.0;
				for (String i : values) {
					try {
						if (Double.parseDouble(i) < lowest) {
							lowest = Double.parseDouble(i);
						}
					} catch (NumberFormatException ex) {
						//Thrown if the value in the tabix is not parsable "." and moves to the next value
					}
				}
				var.addProperty(VariantRec.SIFT_SCORE, lowest);
			} else {
				var.addProperty(VariantRec.SIFT_SCORE, Double.parseDouble(toks[sift_score_col]));
			}
		}
		catch (NumberFormatException ex){//Thrown if the value in the tabix is not parsable "."
		}

		//Polyphen2_HDIV_score
		try {
			if (toks[polyphen_score_col].contains(";")) {
				String[] values = toks[polyphen_score_col].split(";");
				double highest = 0.0;
				for (String i : values) {
					try {
						if (Double.parseDouble(i) > highest) {
							highest = Double.parseDouble(i);
						}
					} catch (NumberFormatException ex) { }
				}
				var.addProperty(VariantRec.POLYPHEN_SCORE, highest);
			} else {
				var.addProperty(VariantRec.POLYPHEN_SCORE, Double.parseDouble(toks[polyphen_score_col]));
			}
		}
		catch (NumberFormatException ex){}

		//POLYPHEN_HVAR_SCORE
		try {
			if (toks[Polyphen2_hvar_score_col].contains(";")) {
				String[] values = toks[Polyphen2_hvar_score_col].split(";");
				double highest = 0.0;
				for (String i : values) {
					try {
						if (Double.parseDouble(i) > highest) {
							highest = Double.parseDouble(i);
						}
					} catch (NumberFormatException ex) { }
				}
				var.addProperty(VariantRec.POLYPHEN_HVAR_SCORE, highest);
			} else {
				var.addProperty(VariantRec.POLYPHEN_HVAR_SCORE, Double.parseDouble(toks[Polyphen2_hvar_score_col]));
			}
		}
		catch (NumberFormatException ex){}

		//LRT SCORE
		try {
			var.addProperty(VariantRec.LRT_SCORE, Double.parseDouble(toks[lrt_score_column]));
		} catch (NumberFormatException ex){}

		//MT_SCORE
		try {
			if (toks[mt_score_column].contains(";")) {
				String[] values = toks[mt_score_column].split(";");
				double highest = 0.0;
				for (String i : values) {
					try {
						if (Double.parseDouble(i) > highest) {
							highest = Double.parseDouble(i);
						}
					} catch (NumberFormatException ex) {}
				}
				var.addProperty(VariantRec.MT_SCORE, highest);
			} else {
				var.addProperty(VariantRec.MT_SCORE, Double.parseDouble(toks[mt_score_column]));
			}
		}
		catch (NumberFormatException ex){}

		//MA_SCORE
		try {
			//if multiple values present keep the most damaging value [LARGER]
			if (toks[ma_score_column].contains(";")) {
				String[] values = toks[ma_score_column].split(";");
				double highest = -6.0;
				for (String i : values) {
					try {
						if (Double.parseDouble(i) > highest) {
							highest = Double.parseDouble(i);
						}
					} catch (NumberFormatException ex) {}
				}
				var.addProperty(VariantRec.MA_SCORE, highest);
			} else {
				var.addProperty(VariantRec.MA_SCORE, Double.parseDouble(toks[ma_score_column]));
			}
		}
		catch (NumberFormatException ex){}

		//GERP_NR_SCORE
		try {
			var.addProperty(VariantRec.GERP_NR_SCORE, Double.parseDouble(toks[gerp_nr_score_column]));
		} catch (NumberFormatException ex){}

		// GERP_SCORE
		try {
			var.addProperty(VariantRec.GERP_SCORE, Double.parseDouble(toks[gerp_score_column]));
		} catch (NumberFormatException ex){}

		//PHYLOP_SCORE
		try {
			var.addProperty(VariantRec.PHYLOP_SCORE, Double.parseDouble(toks[phylop_score_column]));
		} catch (NumberFormatException ex){}

		//SIPHY_SCORE
		try {
			var.addProperty(VariantRec.SIPHY_SCORE, Double.parseDouble(toks[siphy_score_column]));
		} catch (NumberFormatException ex){}

		//POP_FREQUENCY
		try {
			var.addProperty(VariantRec.POP_FREQUENCY, Double.parseDouble(toks[pop_frequency_column]));
		} catch (NumberFormatException ex){}

		//AFR_FREQUENCY
		try {
			var.addProperty(VariantRec.AFR_FREQUENCY, Double.parseDouble(toks[afr_frequency_column]));
		} catch (NumberFormatException ex){}

		//EUR_FREQUENCY
		try {
			var.addProperty(VariantRec.EUR_FREQUENCY, Double.parseDouble(toks[eur_frequency_column]));
		} catch (NumberFormatException ex){}

		//AMR_FREQUENCY
		try {
			var.addProperty(VariantRec.AMR_FREQUENCY, Double.parseDouble(toks[amr_frequency_column]));
		} catch (NumberFormatException ex){}

		//ASN_FREQUENCY
		try {
			var.addProperty(VariantRec.ASN_FREQUENCY, Double.parseDouble(toks[asn_frequency_column]));
		} catch (NumberFormatException ex){}

		//EXOMES_FREQ
		try {
			var.addProperty(VariantRec.EXOMES_FREQ, Double.parseDouble(toks[exomes_freq_column]));
		} catch (NumberFormatException ex){}

		return true;
	}


	/**
	 * Overrides the abstractTabixAnnotator method because the database is not in standard VCF format.
	 *
	 * @param varToAnnotate
	 * @throws OperationFailedException
	 */
	@Override
	public void annotateVariant(VariantRec varToAnnotate) throws OperationFailedException {


		if (! initialized) {
			throw new OperationFailedException("Failed to initialize", this);
		}

		if (reader == null) {
			throw new OperationFailedException("Tabix reader not initialized", this);
		}

		String contig = varToAnnotate.getContig();
		Integer pos = varToAnnotate.getStart();
		String queryStr = contig + ":" + (pos) + "-" + (pos);

		try {
			//Perform the lookup
			TabixReader.Iterator iter = reader.query(queryStr);

			if(iter != null) {
				try {
					String val = iter.next();

					while(val != null) {
						String[] toks = val.split("\t"); //length of the array 16

						if (toks.length == 112) { //the number of columns in the database

							// call the constructer and set variants
							VariantRec queryResultVar = new VariantRec(toks[0], Integer.parseInt(toks[1]), Integer.parseInt(toks[1]), toks[2], toks[3]);
							//Important: Normalize the record so that it will match the
							//variants in the variant pool that we want to annotate
							queryResultVar = VCFParser.normalizeVariant(queryResultVar);

							//Make sure the (normalized) variant we get from the tabix query matches the
							//variant we want to annotate
							//check_variant(queryResultVar.getAlt());
							//Make sure the (normalized) variant we get from the tabix query matches the
							//variant we want to annotate
							for (int i = 0; i < varToAnnotate.getAllAlts().length; i++) {
								if (queryResultVar.getContig().equals(varToAnnotate.getContig())
										&& queryResultVar.getStart() == varToAnnotate.getStart()
										&& queryResultVar.getRef().equals(varToAnnotate.getRef())
										&& queryResultVar.getAlt().equals(varToAnnotate.getAllAlts()[i])) {
									//Everything looks good, so go ahead and annotate
									boolean ok = addAnnotationsFromString(varToAnnotate, val);

									if (ok)
										break;
								}
							}
						}
						val = iter.next();
					}
				} catch (IOException e) {
					throw new OperationFailedException("Error reading data file: " + e.getMessage(), this);
				}
			}
		}
		catch (RuntimeException rex) {
			//Bad contigs will cause an array out-of-bounds exception to be thrown by
			//the tabix reader. There's not much we can do about this since the methods
			//are private... right now we just ignore it and skip this variant
		}



	}
}