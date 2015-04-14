package operator.variant;

import java.io.IOException;
//import java.util.logging.Logger;

import operator.OperationFailedException;
//import operator.annovar.Annotator;
import org.broad.tribble.readers.TabixReader;
//import pipeline.Pipeline;
//import util.flatFilesReader.DBNSFPReader;
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
 * tabix -s 1 -b 2 -e 2 dbscSNV_cat_1-22XY.tab
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


	@Override
	protected boolean addAnnotationsFromString(VariantRec var, String val) {
		String[] toks = val.split("\t");
		//System.out.println(toks[23]);
		//	var.addProperty(VariantRec.SIFT_SCORE, sift);
		// *23 SIFT_score <----- SIFT
		/*SIFT_score: SIFT score (SIFTori). Scores range from 0 to 1. The smaller the score the
		more likely the SNP has damaging effect.
		Multiple scores separated by ";", corresponding to Ensembl_proteinid.
		*/
		try {
			//if multiple values present keep the most damaging value [SMALLER]
			if (toks[23].contains(";")) {
				String [] values = toks[23].split(";");
				double lowest = 2.0;
				for (int i = 0; i < values.length; i++){
					//System.out.println(values[i]);
					try {
						if (Double.parseDouble(values[i]) < lowest){
							lowest = Double.parseDouble(values[i]);
						}
					} catch (NumberFormatException ex){
						//System.out.println(ex.toString());
						//Thrown if the value in the tabix is not parsable "."
						//System.err.println(ex + ", ada: " + _ada);
					}
				}
				var.addProperty(VariantRec.SIFT_SCORE, lowest);
			} else {
				var.addProperty(VariantRec.SIFT_SCORE, Double.parseDouble(toks[23]));
			}

		}
		catch (NumberFormatException ex){
			//System.out.println(ex.toString());
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}

		//	Double pp = reader.getValue(DBNSFPReader.PP);
		//	var.addProperty(VariantRec.POLYPHEN_SCORE, pp);
		//*29 Polyphen2_HDIV_score  <---- PP
		/*
		Polyphen2_HDIV_pred: Polyphen2 prediction based on HumDiv, "D" ("porobably damaging",
                HDIV score in [0.957,1] or rankscore in [0.52844,0.89865]), "P" ("possibly damaging",
                HDIV score in [0.453,0.956] or rankscore in [0.34282,0.52689]) and "B" ("benign",
                HDIV score in [0,0.452] or rankscore in [0.02634,0.34268]). Score cutoff for binary
                classification is 0.5 for HDIV score or 0.3528 for rankscore, i.e. the prediction is
                "neutral" if the HDIV score is smaller than 0.5 (rankscore is smaller than 0.3528),
                and "deleterious" if the HDIV score is larger than 0.5 (rankscore is larger than
                0.3528). Multiple entries are separated by ";".
                		 */
		try {
			//if multiple values present keep the most damaging value [SMALLER]
			if (toks[29].contains(";")) {
				String[] values = toks[29].split(";");
				double highest = 0.0;
				for (int i = 0; i < values.length; i++) {
					//System.out.println(values[i]);
					try {
						if (Double.parseDouble(values[i]) > highest) {
							highest = Double.parseDouble(values[i]);
						}
					} catch (NumberFormatException ex) {
						//System.out.println(ex.toString());
						//Thrown if the value in the tabix is not parsable "."
						//System.err.println(ex + ", ada: " + _ada);
					}
				}
				var.addProperty(VariantRec.POLYPHEN_SCORE, highest);
			} else {
				var.addProperty(VariantRec.POLYPHEN_SCORE, Double.parseDouble(toks[29]));
			}
		}
		catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}


		//	Double ppHvar = reader.getValue(DBNSFPReader.PP_HVAR);
		//	var.addProperty(VariantRec.POLYPHEN_HVAR_SCORE, ppHvar);
		//*32 Polyphen2_HVAR_score <---- PP_HVAR
		/*
		Polyphen2_HVAR_score: Polyphen2 score based on HumVar, i.e. hvar_prob.
                The score ranges from 0 to 1.
                Multiple entries separated by ";", corresponding to Uniprot_acc_Polyphen2.
		 */
		try {
			//if multiple values present keep the most damaging value [LARGER]
			if (toks[32].contains(";")) {
				String[] values = toks[32].split(";");
				double highest = 0.0;
				for (int i = 0; i < values.length; i++) {
					//System.out.println(values[i]);
					try {
						if (Double.parseDouble(values[i]) > highest) {
							highest = Double.parseDouble(values[i]);
						}
					} catch (NumberFormatException ex) {
						//System.out.println(ex.toString());
						//Thrown if the value in the tabix is not parsable "."
						//System.err.println(ex + ", ada: " + _ada);
					}
				}
				var.addProperty(VariantRec.POLYPHEN_HVAR_SCORE, highest);
			} else {
				var.addProperty(VariantRec.POLYPHEN_HVAR_SCORE, Double.parseDouble(toks[32]));
			}
		}
		catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}

		//	Double lrt = reader.getValue(DBNSFPReader.LRT);
		//	var.addProperty(VariantRec.LRT_SCORE, lrt);
		//*35 LRT_score  <---- LRT
		/*
		LRT_score: The original LRT two-sided p-value (LRTori), ranges from 0 to 1.
		 */
		try {
			var.addProperty(VariantRec.LRT_SCORE, Double.parseDouble(toks[35]));
		}
		catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}

		//	Double mt = reader.getValue(DBNSFPReader.MT);
		//	var.addProperty(VariantRec.MT_SCORE, mt);
		// *39 MutationTaster_score <------ MT
		/*
		MutationTaster_score: MutationTaster p-value (MTori), ranges from 0 to 1.
		Does not say but these are ";" seperated.
		 */
		int _column = 39;
		try {
			if (toks[_column].contains(";")) {
				String[] values = toks[_column].split(";");
				double highest = 0.0;
				for (int i = 0; i < values.length; i++) {
					//System.out.println(values[i]);
					try {
						if (Double.parseDouble(values[i]) > highest) {
							highest = Double.parseDouble(values[i]);
						}
					} catch (NumberFormatException ex) {
						//System.out.println(ex.toString());
						//Thrown if the value in the tabix is not parsable "."
						//System.err.println(ex + ", ada: " + _ada);
					}
				}
				var.addProperty(VariantRec.MT_SCORE, highest);
			} else {
				var.addProperty(VariantRec.MT_SCORE, Double.parseDouble(toks[_column]));
			}
		}
		catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}

		//	Double ma = reader.getValue(DBNSFPReader.MA);
		//	var.addProperty(VariantRec.MA_SCORE, ma);
		//*46 MutationAssessor_score  <----- MA
		/*
		MutationAssessor_score: MutationAssessor functional impact combined score (MAori). The
                score ranges from -5.545 to 5.975 in dbNSFP.
		 */
		_column = 46;
		try {
			//if multiple values present keep the most damaging value [LARGER]
			if (toks[_column].contains(";")) {
				String[] values = toks[_column].split(";");
				double highest = -6.0;
				for (int i = 0; i < values.length; i++) {
					//System.out.println(values[i]);
					try {
						if (Double.parseDouble(values[i]) > highest) {
							highest = Double.parseDouble(values[i]);
						}
					} catch (NumberFormatException ex) {
						//System.out.println(ex.toString());
						//Thrown if the value in the tabix is not parsable "."
						//System.err.println(ex + ", ada: " + _ada);
					}
				}
				var.addProperty(VariantRec.MA_SCORE, highest);
			} else {
				var.addProperty(VariantRec.MA_SCORE, Double.parseDouble(toks[_column]));
			}
		}
		catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}


		//*62 GERP++_NR -----> GERP_NR
		//	Double gerpNR = reader.getValue(DBNSFPReader.GERP_NR);
		//	var.addProperty(VariantRec.GERP_NR_SCORE, gerpNR);
		//  GERP++_NR: GERP++ neutral rate
		try {
			var.addProperty(VariantRec.GERP_NR_SCORE, Double.parseDouble(toks[62]));
		}
		catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}


		//	Double gerp = reader.getValue(DBNSFPReader.GERP);
		//	var.addProperty(VariantRec.GERP_SCORE, gerp);
		//*63 GERP++_RS -----> GERP
		// GERP++_RS: GERP++ RS score, the larger the score, the more conserved the site.
		try {
			var.addProperty(VariantRec.GERP_SCORE, Double.parseDouble(toks[63]));
		}
		catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}

		//	Double phylop = reader.getValue(DBNSFPReader.PHYLOP);
		//	var.addProperty(VariantRec.PHYLOP_SCORE, phylop);
		//*65 phyloP7way_vertebrate  <----- PHYLOP
		try {
			var.addProperty(VariantRec.PHYLOP_SCORE, Double.parseDouble(toks[65]));
		}
		catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}

		//	Double siphy = reader.getValue(DBNSFPReader.SIPHY);
		//	var.addProperty(VariantRec.SIPHY_SCORE, siphy);
		//*70 SiPhy_29way_logOdds  <----- SIPHY
		try {
			var.addProperty(VariantRec.SIPHY_SCORE, Double.parseDouble(toks[70]));
		}
		catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}


		//	Double popFreq = reader.getValue(DBNSFPReader.TKG);
		//	var.addProperty(VariantRec.POP_FREQUENCY, popFreq);
		//	*73 1000Gp3_AF   <---- TKG
		try {
			var.addProperty(VariantRec.POP_FREQUENCY, Double.parseDouble(toks[73]));
		}
		catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}

		//	Double afrFreq = reader.getValue(DBNSFPReader.TKG_AFR);
		// var.addProperty(VariantRec.AFR_FREQUENCY, afrFreq);
		//*75 1000Gp3_AFR_AF  <----TKG_AFR

		try {
			var.addProperty(VariantRec.AFR_FREQUENCY, Double.parseDouble(toks[75]));
		}
		catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}

		//	Double eurFreq = reader.getValue(DBNSFPReader.TKG_EUR);
		// var.addProperty(VariantRec.EUR_FREQUENCY, eurFreq);
		//*77 1000Gp3_EUR_AF  <-----TKG_EUR
		try {
			var.addProperty(VariantRec.EUR_FREQUENCY, Double.parseDouble(toks[77]));
		}
		catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}


		//*79 1000Gp3_AMR_AF   <-----TKG_AMR
		//	Double amrFreq = reader.getValue(DBNSFPReader.TKG_AMR);
		//		var.addProperty(VariantRec.AMR_FREQUENCY, amrFreq);
		try {
			var.addProperty(VariantRec.AMR_FREQUENCY, Double.parseDouble(toks[79]));
		}
		catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}

		//	Double asnFreq = reader.getValue(DBNSFPReader.TKG_ASN);
		// var.addProperty(VariantRec.ASN_FREQUENCY, asnFreq);
		//*81 1000Gp3_EAS_AF
		try {
			var.addProperty(VariantRec.ASN_FREQUENCY, Double.parseDouble(toks[81]));
		}
		catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}

		//	Double espFreq = reader.getValue(DBNSFPReader.ESP5400);
		//var.addProperty(VariantRec.EXOMES_FREQ, espFreq);
		//*91 ESP6500_EA_AF
		try {
			var.addProperty(VariantRec.EXOMES_FREQ, Double.parseDouble(toks[91]));
		}
		catch (NumberFormatException ex){
			//Thrown if the value in the tabix is not parsable "."
			//System.err.println(ex + ", ada: " + _ada);
		}
		return true;
	}


	//***************  THIS VALUE NO LONGER EXISTS  ******************************//
	//	Double slr = reader.getValue(DBNSFPReader.SLR_TEST); //deprecated
	//	var.addProperty(VariantRec.SLR_TEST, slr);
	//*******************


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
		//System.out.println(contig + " " + pos);
		String queryStr = contig + ":" + (pos) + "-" + (pos);

		try {
			//Perform the lookup
			TabixReader.Iterator iter = reader.query(queryStr);

			if(iter != null) {
				try {
					String val = iter.next();

					while(val != null) {
						String[] toks = val.split("\t"); //length of the array 16

						if (toks.length > 15) {


							//Convert the result (which is a line of a VCF file) into a variant rec
							// call the constructer and set variants
							VariantRec queryResultVar = new VariantRec(toks[0], Integer.parseInt(toks[1]), Integer.parseInt(toks[1]), toks[2], toks[3]);
							//Important: Normalize the record so that it will match the
							//variants in the variant pool that we want to annotate
							queryResultVar = VCFParser.normalizeVariant(queryResultVar);

							//Make sure the (normalized) variant we get from the tabix query matches the
							//variant we want to annotate
							check_variant(queryResultVar.getAlt());
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