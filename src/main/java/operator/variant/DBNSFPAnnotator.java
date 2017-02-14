package operator.variant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.regex.Pattern;

import com.sun.org.apache.xpath.internal.SourceTree;

import operator.OperationFailedException;

import org.broad.tribble.readers.TabixReader;
import org.w3c.dom.NodeList;

import util.vcfParser.VCFParser;
import buffer.variant.VariantRec;

/**
 * Uses a tabix-index file.
 * <p/>
 * Tabix file is created from a db [tab delimited] obtained form [ftp://dbnsfp:dbnsfp@dbnsfp.softgenetics.com/dbNSFPv3.0b2c.zip]
 * the file [ftp://dbnsfp:dbnsfp@dbnsfp.softgenetics.com/dbscSNV.zip] dbscSNv.zip unpacks
 * to 24 files (chr 1 - 22, X, Y).  These files are cat together with the bash command below.
 * <p/>
 * for i in {1..22} X Y; do tail -n +2 dbNSFP3.01bc_variant.chr${i} >> dbNSFP3.0b1c_2015_04_13.tab ; done
 * #tail to remove the header
 * <p/>
 * <p/>
 * 0 chr          B38!
 * 1 pos(1-based) B38!
 * 2 ref
 * 3 alt
 * 4 aaref
 * 5 aaalt
 * 6 rs_dbSNP142
 * 7 hg19_chr     Actual its using just #'s so B37, not chr
 * 8 hg19_pos(1-based)
 * 9 hg18_chr
 * 10 hg18_pos(1-based)
 * 11 genename
 * 12 cds_strand
 * 13 refcodon
 * 14 codonpos
 * 15 codon_degeneracy
 * 16 Ancestral_allele
 * 17 AltaiNeandertal
 * 18 Denisova
 * 19 Ensembl_geneid
 * 20 Ensembl_transcriptid
 * 21 Ensembl_proteinid
 * 22 aapos
 * 23 SIFT_score <----- SIFT
 * SIFT_score: SIFT score (SIFTori). Scores range from 0 to 1. The smaller the score the
 * more likely the SNP has damaging effect.
 * Multiple scores separated by ";", corresponding to Ensembl_proteinid.
 * <p/>
 * <p/>
 * 24 SIFT_converted_rankscore
 * 25 SIFT_pred
 * 26 Uniprot_acc_Polyphen2
 * 27 Uniprot_id_Polyphen2
 * 28 Uniprot_aapos_Polyphen2
 * 29 Polyphen2_HDIV_score  <---- PP
 * <p/>
 * <p/>
 * <p/>
 * 30 Polyphen2_HDIV_rankscore
 * 31 Polyphen2_HDIV_pred
 * 32 Polyphen2_HVAR_score <---- PP_HVAR
 * 33 Polyphen2_HVAR_rankscore
 * 34 Polyphen2_HVAR_pred
 * 35 LRT_score  <---- LRT
 * 36 LRT_converted_rankscore
 * 37 LRT_pred
 * 38 LRT_Omega
 * 39 MutationTaster_score <------ MT this is column 37 in 2.9
 * 40 MutationTaster_converted_rankscore
 * 41 MutationTaster_pred <------ MT this is column 38 in 2.9
 * 42 MutationTaster_model
 * 43 MutationTaster_AAE
 * 44 Uniprot_id_MutationAssessor
 * 45 Uniprot_variant_MutationAssessor
 * 46 MutationAssessor_score  <----- MA
 * 47 MutationAssessor_rankscore
 * 48 MutationAssessor_pred
 * 49 FATHMM_score
 * 50 FATHMM_converted_rankscore
 * 51 FATHMM_pred
 * 52 PROVEAN_score
 * 53 PROVEAN_converted_rankscore
 * 54 PROVEAN_pred
 * 55 MetaSVM_score
 * 56 MetaSVM_rankscore
 * 57 MetaSVM_pred
 * 58 MetaLR_score
 * 59 MetaLR_rankscore
 * 60 MetaLR_pred
 * 61 Reliability_index
 * 62 GERP++_NR -----> GERP_NR
 * 63 GERP++_RS -----> GERP
 * 64 GERP++_RS_rankscore
 * 65 phyloP7way_vertebrate  <----- PHYLOP
 * 66 phyloP7way_vertebrate_rankscore
 * 67 phastCons7way_vertebrate
 * 68 phastCons7way_vertebrate_rankscore
 * 69 SiPhy_29way_pi
 * 70 SiPhy_29way_logOdds  <----- SIPHY
 * 71 SiPhy_29way_logOdds_rankscore
 * 72 1000Gp3_AC
 * 73 1000Gp3_AF   <---- TKG
 * 74 1000Gp3_AFR_AC
 * 75 1000Gp3_AFR_AF  <----TKG_AFR
 * 76 1000Gp3_EUR_AC
 * 77 1000Gp3_EUR_AF  <-----TKG_EUR
 * 78 1000Gp3_AMR_AC
 * 79 1000Gp3_AMR_AF   <-----TKG_AMR
 * 80 1000Gp3_EAS_AC
 * 81 1000Gp3_EAS_AF   <-----TKG_ASN
 * 82 1000Gp3_SAS_AC
 * 83 1000Gp3_SAS_AF
 * 84 TWINSUK_AC
 * 85 TWINSUK_AF
 * 86 ALSPAC_AC
 * 87 ALSPAC_AF
 * 88 ESP6500_AA_AC
 * 89 ESP6500_AA_AF
 * 90 ESP6500_EA_AC
 * 91 ESP6500_EA_AF
 * 92 ExAC_AC
 * 93 ExAC_AF
 * 94 ExAC_Adj_AC
 * 95 ExAC_Adj_AF
 * 96 ExAC_AFR_AC
 * 97 ExAC_AFR_AF
 * 98 ExAC_AMR_AC
 * 99 ExAC_AMR_AF
 * 100 ExAC_EAS_AC
 * 101 ExAC_EAS_AF
 * 102 ExAC_FIN_AC
 * 103 ExAC_FIN_AF
 * 104 ExAC_NFE_AC
 * 105 ExAC_NFE_AF
 * 106 ExAC_SAS_AC
 * 107 ExAC_SAS_AF
 * 108 clinvar_rs
 * 109 clinvar_clnsig
 * 110 clinvar_trait
 * 111 Interpro_domain
 * <p/>
 * <p/>
 * this cat'd file is tabix index using the following commands
 * bgzip -c dbNSFP3.0b1c_2015_04_13.tab > dbNSFP3.0b1c_2015_04_13.tab.gz
 * tabix -s 1 -b 2 -e 2 dbNSFP3.0b1c_2015_04_13.tab.gz
 * <p/>
 * the MD5 for the files are:
 * dbNSFP3.0b1c_2015_04_13.tab.gz
 * dbscSNV_cat_1-22XY.tab.bgz.tbi 688K Feb 24
 * <p/>
 * <p/>
 * TESTING
 * A truncated database was utilized; see TestDBNSFP for details.
 *
 * @author Keith Simmon
 * @date April 13th 2015
 * 
Mods by Nix, 17 March 2016
# big headache something is breaking, processing with individual steps works.

# downloaded the zip archive
wget ftp://dbnsfp:dbnsfp@dbnsfp.softgenetics.com/dbNSFPv3.1a.zip

# uncompressed
unzip dbNSFPv3.1a.zip 

# sort each independently
for x in *_variant.chr* 
do
echo $x
sort -k8,8 -k9,9 --numeric -T . $x | grep -v ^# > $x".sorted"

# modifying chr22, chr6, and chr11 to pull off b37 coordinates with diff chromosomes and appropriately append
cat dbNSFP3.1a_variant.chr6.sorted | awk '{if($8!=6)print;}' > notChr6
cat dbNSFP3.1a_variant.chr6.sorted | awk '{if($8==6)print;}' > chr6

# only three cases where there were variants that shifted chromosome
cat notChr6_append2ChrY >> dbNSFP3.1a_variant.chrY.sorted 
cat notChr17_append2Chr11 >> dbNSFP3.1a_variant.chr11.sorted 
cat notChr22_append2Chr14 >> dbNSFP3.1a_variant.chr14.sorted 

# re sort these
for x in dbNSFP3.1a_variant.chrY.sorted dbNSFP3.1a_variant.chr11.sorted dbNSFP3.1a_variant.chr14.sorted
do
echo $x
sort -k8,8 -k9,9 --numeric -T . $x  > $x".resorted"
done

# gzip
cat dbNSFP3.1a_variant.chr1.sorted dbNSFP3.1a_variant.chr2.sorted dbNSFP3.1a_variant.chr3.sorted dbNSFP3.1a_variant.chr4.sorted dbNSFP3.1a_variant.chr5.sorted chr6 dbNSFP3.1a_variant.chr7.sorted dbNSFP3.1a_variant.chr8.sorted dbNSFP3.1a_variant.chr9.sorted dbNSFP3.1a_variant.chr10.sorted dbNSFP3.1a_variant.chr11.sorted.resorted dbNSFP3.1a_variant.chr12.sorted dbNSFP3.1a_variant.chr13.sorted dbNSFP3.1a_variant.chr14.sorted.resorted dbNSFP3.1a_variant.chr15.sorted dbNSFP3.1a_variant.chr16.sorted chr17 dbNSFP3.1a_variant.chr18.sorted dbNSFP3.1a_variant.chr19.sorted dbNSFP3.1a_variant.chr20.sorted dbNSFP3.1a_variant.chr21.sorted chr22 dbNSFP3.1a_variant.chrM.sorted dbNSFP3.1a_variant.chrX.sorted dbNSFP3.1a_variant.chrY.sorted.resorted \
| ~/Ref/Apps/HTSlib/1.3/bin/bgzip > dbNSFPv3.1a.b37.gz

# tab index it but use the hg19/ b37 columns, first column is 1 not zero
~/Ref/Apps/HTSlib/1.3/bin/tabix -s 8 -b 9 -e 9 dbNSFPv3.1a.b37.gz
 */

public class DBNSFPAnnotator extends AbstractTabixAnnotator {

    private boolean initialized = false;
    private TabixReader reader = null;

    public static final String DBNSFP_PATH = "dbnsfp.path";
    public static final String DBNSFP_VERSION = "dbnsfp.version";
    public static final Pattern TAB = Pattern.compile("\\t");
    protected String dbnsfpVersion = null;
    
    private int sift_score_col;
    private int sift_pred_col;
    private int polyphen_score_col;
    private int Polyphen2_hvar_score_col;
    private int Polyphen2_hvar_pred_col;
    private int lrt_score_column;
    private int mt_score_column;
    private int mt_pred_column;
    private int ma_score_column;
    private int ma_pred_column;
    private int gerp_nr_score_column;
    private int gerp_score_column;
    private int phylop_score_column;
    private int siphy_score_column;
    private int b37Chr_column;
    private int b37Pos_column;
    private int b37Ref_column;
    private int b37Alt_column;
    
    
    public static final String MTprediction="";
    public static final String MAprediction="";
    

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
     * returns the sift column for a specific dbNSFP DB
     *
     * @param dbnsfpVersion
     * @return column index
     */
    private int getSiftColumn(String dbnsfpVersion) {
        if (dbnsfpVersion.equals("3.0") || dbnsfpVersion.equals("3.1a")) return 23;
        if (dbnsfpVersion.equals("2.9") || dbnsfpVersion.equals("2.9.2")) return 26; 
        if (dbnsfpVersion.equals("2.0")) return 21;
        return -1;
    }
    /**
     * returns the sift prediction column for a specific dbNSFP DB
     *
     * @param dbnsfpVersion
     * @return column index
     */
    private int getSiftPredColumn(String dbnsfpVersion) {
        if (dbnsfpVersion.equals("3.0") || dbnsfpVersion.equals("3.1a")) return -1;
        if (dbnsfpVersion.equals("2.9") || dbnsfpVersion.equals("2.9.2")) return 28;
        if (dbnsfpVersion.equals("2.0")) return -1;
        return -1;
    }

    /**
     * returns the Polyphen2_HDIV_score column index for a specific dbNSFP DB
     *
     * @param dbnsfpVersion
     * @return column index
     */
    private int getPolyphenScoreColumn(String dbnsfpVersion) {
        if (dbnsfpVersion.equals("3.0") || dbnsfpVersion.equals("3.1a")) return 29;
        if (dbnsfpVersion.equals("2.9") || dbnsfpVersion.equals("2.9.2")) return 29;
        if (dbnsfpVersion.equals("2.0")) return 22;
        return -1;
    }
    
    /**
     * Returns the Polyphen2_HVAR_score column index for a specific dbNSFP DB
     *
     * @param dbnsfpVersion
     * @return column index
     */
    private int getPolyphenScoreHVARColumn(String dbnsfpVersion) {
        if (dbnsfpVersion.equals("3.0") || dbnsfpVersion.equals("3.1a")) return 32;
        if (dbnsfpVersion.equals("2.9") || dbnsfpVersion.equals("2.9.2")) return 32;
        if (dbnsfpVersion.equals("2.0")) return 24;
        return -1;
    }
    /**
     * Returns the Polyphen2_HVAR_PRED column index for a specific dbNSFP DB
     *
     * @param dbnsfpVersion
     * @return column index
     */
    private int getPolyphenScoreHVARPREDColumn(String dbnsfpVersion) {
        if (dbnsfpVersion.equals("3.0") || dbnsfpVersion.equals("3.1a")) return -1;
        if (dbnsfpVersion.equals("2.9") || dbnsfpVersion.equals("2.9.2")) return 34;
        if (dbnsfpVersion.equals("2.0")) return -1;
        return -1;
    }

    /**
     * Returns the LRT_score column index for a specific dbNSFP DB
     *
     * @param dbnsfpVersion
     * @return column index
     */
    private int getLRTScoreColumn(String dbnsfpVersion) {
        if (dbnsfpVersion.equals("3.0") || dbnsfpVersion.equals("3.1a")) return 35;
        if (dbnsfpVersion.equals("2.9") || dbnsfpVersion.equals("2.9.2")) return 35;
        if (dbnsfpVersion.equals("2.0")) return 26;
        return -1;
    }

    /**
     * Returns the Mutation_taster_score column index for a specific dbNSFP DB
     *
     * @param dbnsfpVersion
     * @return column index
     */
    private int getMTScoreColumn(String dbnsfpVersion) {
        if (dbnsfpVersion.equals("3.0") || dbnsfpVersion.equals("3.1a")) return -1;
        if (dbnsfpVersion.equals("2.9") || dbnsfpVersion.equals("2.9.2")) return 38;
        if (dbnsfpVersion.equals("2.0")) return -1;
        return -1;
    }
    /**
     * Returns the Mutation_taster_prediction column index for a specific dbNSFP DB
     *
     * @param dbnsfpVersion
     * @return column index
     */
    private int getMTPredColumn(String dbnsfpVersion) {
        if (dbnsfpVersion.equals("3.0") || dbnsfpVersion.equals("3.1a")) return -1;
        if (dbnsfpVersion.equals("2.9") || dbnsfpVersion.equals("2.9.2")) return 40;//from 37????
        if (dbnsfpVersion.equals("2.0")) return -1;
        return -1;
    }    

    /**
     * Returns the Mutation_Assessor_score column index for a specific dbNSFP DB
     *
     * @param dbnsfpVersion
     * @return column index
     */
    private int getMAScoreColumn(String dbnsfpVersion) {
        if (dbnsfpVersion.equals("3.0") || dbnsfpVersion.equals("3.1a")) return -1;
        if (dbnsfpVersion.equals("2.9") || dbnsfpVersion.equals("2.9.2")) return 42;//this is the "converted" score of 0-1
        if (dbnsfpVersion.equals("2.0")) return -1;
        return -1;
    }   
    
    /**
     * Returns the Mutation_Assessor_prediction column index for a specific dbNSFP DB
     *
     * @param dbnsfpVersion
     * @return column index
     */
    private int getMAPredColumn(String dbnsfpVersion) {
        if (dbnsfpVersion.equals("3.0") || dbnsfpVersion.equals("3.1a")) return -1;
        if (dbnsfpVersion.equals("2.9") || dbnsfpVersion.equals("2.9.2")) return 43;
        if (dbnsfpVersion.equals("2.0")) return -1;
        return -1;
    }    

    /**
     * Returns the GERP_NR column index for a specific dbNSFP DB
     *
     * @param dbnsfpVersion
     * @return column index
     */
    private int getGerpNRColumn(String dbnsfpVersion) {
    	if (dbnsfpVersion.equals("3.1a")) return 87;
    	if (dbnsfpVersion.equals("3.0")) return 62;
        if (dbnsfpVersion.equals("2.9") || dbnsfpVersion.equals("2.9.2")) return 62;
        if (dbnsfpVersion.equals("2.0")) return 32;
        return -1;
    }

    /**
     * Returns the GERP_RS column index for a specific dbNSFP DB
     *
     * @param dbnsfpVersion
     * @return column index
     */
    private int getGerpColumn(String dbnsfpVersion) {
    	if (dbnsfpVersion.equals("3.1a")) return 88;
        if (dbnsfpVersion.equals("3.0")) return 63;
        if (dbnsfpVersion.equals("2.9") || dbnsfpVersion.equals("2.9.2")) return 63;
        if (dbnsfpVersion.equals("2.0")) return 33;
        return -1;
    
    }

    /**
     * Returns the PhyloP column index for a specific dbNSFP DB
     *
     * @param dbnsfpVersion
     * @return column index
     */
    private int getPhylopColumn(String dbnsfpVersion) {
    	if (dbnsfpVersion.equals("3.1a")) return 90;
        if (dbnsfpVersion.equals("3.0")) return 65;
        if (dbnsfpVersion.equals("2.9") || dbnsfpVersion.equals("2.9.2")) return 69;
        if (dbnsfpVersion.equals("2.0")) return 34;
        return -1;

    }

    /**
     * Returns the SiPhy_29way_logOdds column index for a specific dbNSFP DB
     *
     * @param dbnsfpVersion
     * @return column index
     */
    private int getSiphyColumn(String dbnsfpVersion) {
    	if (dbnsfpVersion.equals("3.1a")) return 99;
        if (dbnsfpVersion.equals("3.0")) return 70;
        if (dbnsfpVersion.equals("2.9") || dbnsfpVersion.equals("2.9.2")) return 78;
        if (dbnsfpVersion.equals("2.0")) return 36;
        return -1;
    }


    /**
     * When the variant exists in the dbNSFP database annotation properties are added to the variant
     * <p/>
     * The current annotations added include:
     * <p/>
     * SIFT_SCORE [column 23
     * POLYPHEN_SCORE [column 29] Polyphen2_HDIV_score:
     * POLYPHEN_HVAR_SCORE [column 32]	 * 	Polyphen2_HVAR_score:
     * LRT_score [column 35] RT_score:
     * MT_SCORE [column 39] utationTaster_score:
     * MA_SCORE [column 46]	MutationAssessor_score
     * GERP_NR_SCORE [column 62]GERP++_NR: GERP++ neutral rate
     * GERP_SCORE [column 63]GERP++_RS: GERP++ RS score, the larger the score, the more conserved the site.
     * PHYLOP_SCORE hyloP7way_vertebrate: phyloP (phylogenetic p-values) conservation score based on the multiple alignments of 7
     * SIPHY_SCORE	 * 	SiPhy_29way_logOdds: SiPhy score based on 29 mammals genomes. The larger the score, the more conserved the site.
     * <p/>
     * *THIS VALUE NO LONGER EXISTS  *********************************************
     * *	Double slr = reader.getValue(DBNSFPReader.SLR_TEST); //deprecated     *
     * *	var.addProperty(VariantRec.SLR_TEST, slr);						      *
     * ***************************************************************************
     *
     * @param var the variant record to be annotated
     * @param val the string (line) retrieved by tabix
     * @return true when annotations are added
     */
    @Override
    protected boolean addAnnotationsFromString(VariantRec var, String val, int altIndex) {
    	String[] toks = TAB.split(val);

        //SIFT_SCORE, takes the lowest
    	int siftindex = 0;
        try {
            if (toks[sift_score_col].contains(";")) {
                String[] values = toks[sift_score_col].split(";");
                double lowest = 2.0;
                for (String i : values) {
                    try {
                        if (Double.parseDouble(i) < lowest) {
                            lowest = Double.parseDouble(i);
                            siftindex = Arrays.asList(values).indexOf(i);//get index of  "highest"
                        }
                    } catch (NumberFormatException ex) {
                        //Thrown if the value in the tabix is not parsable "." and moves to the next value
                    }
                }
                var.addProperty(VariantRec.SIFT_SCORE, lowest);
            } else {
                var.addProperty(VariantRec.SIFT_SCORE, Double.parseDouble(toks[sift_score_col]));
            }           
        } catch (NumberFormatException ex) {//Thrown if the value in the tabix is not parsable "."
        }
        
        //SIFT_PRED
        try {
            String nonAbrrvSIFTPredColumn = "";
            String abrrvSIFTPredColumn = "";
            if (toks[sift_pred_col].contains(";")) {
                String[] values = toks[sift_pred_col].split(";");
                abrrvSIFTPredColumn = values[siftindex];
            } else {
                abrrvSIFTPredColumn = toks[sift_pred_col];
            }
            if (abrrvSIFTPredColumn.equals("D")) {
                nonAbrrvSIFTPredColumn = "damaging";
            } else if(abrrvSIFTPredColumn.equals("T")) {
                nonAbrrvSIFTPredColumn = "tolerated";
            }
            else{
                nonAbrrvSIFTPredColumn = abrrvSIFTPredColumn;
            }
                var.addAnnotation(VariantRec.SIFT_PRED, nonAbrrvSIFTPredColumn);   	         
        } catch (NumberFormatException ex) {
        	//Thrown if the value in the tabix is not parsable "."
        }

        //Polyphen2_HDIV_score, takes the highest
        try {
            if (toks[polyphen_score_col].contains(";")) {
                String[] values = toks[polyphen_score_col].split(";");
                double highest = 0.0;
                for (String i : values) {
                    try {
                        if (Double.parseDouble(i) > highest) {
                            highest = Double.parseDouble(i);
                        }
                    } catch (NumberFormatException ex) {
                    }
                }
                var.addProperty(VariantRec.POLYPHEN_SCORE, highest);
            } else {
                var.addProperty(VariantRec.POLYPHEN_SCORE, Double.parseDouble(toks[polyphen_score_col]));
            }
        } catch (NumberFormatException ex) {
        }

        //POLYPHEN_HVAR_SCORE, takes the highest
        int hvarindex = 0;
        try {
            if (toks[Polyphen2_hvar_score_col].contains(";")) {
                String[] values = toks[Polyphen2_hvar_score_col].split(";");
                double highest = 0.0;
                for (String i : values) {
                    try {
                        if (Double.parseDouble(i) > highest) {
                            highest = Double.parseDouble(i);
                            hvarindex = Arrays.asList(values).indexOf(i);//get index of  "highest"
                        }
                    } catch (NumberFormatException ex) {
                    }
                }
                var.addProperty(VariantRec.POLYPHEN_HVAR_SCORE, highest);
            } else {
                var.addProperty(VariantRec.POLYPHEN_HVAR_SCORE, Double.parseDouble(toks[Polyphen2_hvar_score_col]));
            }
        } catch (NumberFormatException ex) {
        }
        //POLYPHEN_HVAR_PRED
        try {
        	String nonAbrrvPPPredColumn = "";
        	String abrrvPPPredColumn = "";
            if (toks[Polyphen2_hvar_pred_col].contains(";")) {
                String[] values = toks[Polyphen2_hvar_pred_col].split(";");
                abrrvPPPredColumn = values[hvarindex];
            } else {
            	abrrvPPPredColumn = toks[Polyphen2_hvar_pred_col];
            }
            if (abrrvPPPredColumn.equals("D")) {
            		nonAbrrvPPPredColumn = "probably_damaging";
            } else if(abrrvPPPredColumn.equals("P")) {
            		nonAbrrvPPPredColumn= "possibly_damaging";
            } else if(abrrvPPPredColumn.equals("B")) {
            		nonAbrrvPPPredColumn= "benign";
            }
            else{
            		nonAbrrvPPPredColumn = abrrvPPPredColumn;
            }
            var.addAnnotation(VariantRec.POLYPHEN_HVAR_PRED, nonAbrrvPPPredColumn);
        } catch (NumberFormatException ex) {
        }

        //LRT SCORE, just adds
        try {
            var.addProperty(VariantRec.LRT_SCORE, Double.parseDouble(toks[lrt_score_column])); 
        } catch (NumberFormatException ex) {
        }

        int mutalyzerindex = 0;
        //MT_SCORE, takes the highest
        try {
            if (toks[mt_score_column].contains(";")) {
                String[] values = toks[mt_score_column].split(";");
                double highest = 0.0;
                for (String i : values) {
                    try {
                        if (Double.parseDouble(i) > highest) {
                            highest = Double.parseDouble(i);
                            mutalyzerindex = Arrays.asList(values).indexOf(i);//get index of  "highest" 
                        }
                    } catch (NumberFormatException ex) {
                    }
                }
                var.addProperty(VariantRec.MT_SCORE, highest);
            } else {
                var.addProperty(VariantRec.MT_SCORE, Double.parseDouble(toks[mt_score_column]));
            }
        } catch (NumberFormatException ex) {
        	//Thrown if the value in the tabix is not parsable "."
        }

        String mt_pred = null;
        try {
        	String nonAbrrvPredColumn = "";
        	String abrrvPredColumn = "";
        	
            if (toks[mt_pred_column].contains(";")) {
                String[] values = toks[mt_pred_column].split(";");
                mt_pred = values[mutalyzerindex];
                var.addAnnotation(VariantRec.MT_PRED, mt_pred);
                }
            else{
            	abrrvPredColumn = toks[mt_pred_column];
            }
            if (abrrvPredColumn.equals("A")) {
            		nonAbrrvPredColumn = "disease_causing_automatic";
            } else if(abrrvPredColumn.equals("D")) {
            		nonAbrrvPredColumn= "disease_causing";
            } else if(abrrvPredColumn.equals("N")) {
            		nonAbrrvPredColumn= "polymorphism";
            } else if(abrrvPredColumn.equals("P")) {
            		nonAbrrvPredColumn= "polymorphism_automatic";
            }
            else{
            	nonAbrrvPredColumn = abrrvPredColumn;
            }
            var.addAnnotation(VariantRec.MT_PRED, nonAbrrvPredColumn);
        } catch (NumberFormatException ex) {
        	
        }        
        
        
        //MA_SCORE, takes highest
        try {
            //if multiple values present keep the most damaging value [LARGER]
            if (toks[ma_score_column].contains(";")) {
                String[] values = toks[ma_score_column].split(";");
                double highest = -6.0;
                for (String i : values) {
                    try {
                        if (Double.parseDouble(i) > highest) {
                            highest = Double.parseDouble(i);
                            mutalyzerindex = Arrays.asList(values).indexOf(i);
                        }
                    } catch (NumberFormatException ex) {
                    }
                }
                var.addProperty(VariantRec.MA_SCORE, highest);
            } else {
                var.addProperty(VariantRec.MA_SCORE, Double.parseDouble(toks[ma_score_column]));
            }
        } catch (NumberFormatException ex) {
        }
        String ma_pred = null;
        try {
            if (toks[ma_pred_column].contains(";")) {
                String[] values = toks[ma_pred_column].split(";");
                ma_pred = values[mutalyzerindex];
                var.addAnnotation(VariantRec.MA_PRED, ma_pred);
                }
            else{
            	var.addAnnotation(VariantRec.MA_PRED, toks[ma_pred_column]);
            }
        }
            catch (NumberFormatException ex) {
            	//Thrown if the value in the tabix is not parsable "."
        }   

        //GERP_NR_SCORE, just adds
        try {
            var.addProperty(VariantRec.GERP_NR_SCORE, Double.parseDouble(toks[gerp_nr_score_column]));
        } catch (NumberFormatException ex) {
        }

        // GERP_SCORE, just adds
        try {
            var.addProperty(VariantRec.GERP_SCORE, Double.parseDouble(toks[gerp_score_column]));
        } catch (NumberFormatException ex) {
        }

        //PHYLOP_SCORE, just adds
        try {
            var.addProperty(VariantRec.PHYLOP_SCORE, Double.parseDouble(toks[phylop_score_column]));
        } catch (NumberFormatException ex) {
        }

        //SIPHY_SCORE, just adds
        try {
            var.addProperty(VariantRec.SIPHY_SCORE, Double.parseDouble(toks[siphy_score_column]));
        } catch (NumberFormatException ex) {
        }


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

        if (!initialized) throw new OperationFailedException("Failed to initialize", this);
        if (reader == null) throw new OperationFailedException("Tabix reader not initialized", this);

        String contig = varToAnnotate.getContig();
        Integer pos = varToAnnotate.getStart();
        String queryStr = contig + ":" + (pos) + "-" + (pos);
        
        try {
            //Perform the lookup
            TabixReader.Iterator iter = reader.query(queryStr);
            if (iter != null) {
            	
                try {
                    String val = iter.next();
                    while (val != null) {
                        String[] toks = val.split("\t"); //length of the array 16
                        // call the constructer and set variants
                        //VariantRec queryResultVar = new VariantRec(toks[0], Integer.parseInt(toks[1]), Integer.parseInt(toks[1]), toks[2], toks[3]);
                        VariantRec queryResultVar = new VariantRec(toks[b37Chr_column], Integer.parseInt(toks[b37Pos_column]), Integer.parseInt(toks[b37Pos_column]), toks[b37Ref_column], toks[b37Alt_column]);

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
                                boolean ok = addAnnotationsFromString(varToAnnotate, val, i);
                                if (ok) break;
                            }
                        }
                        val = iter.next();
                    }
                } catch (IOException e) {
                    throw new OperationFailedException("Error reading data file: " + e.getMessage(), this);
                }
            }
        } catch (RuntimeException rex) {
            //Bad contigs will cause an array out-of-bounds exception to be thrown by
            //the tabix reader. There's not much we can do about this since the methods
            //are private... right now we just ignore it and skip this variant
        }

    }

    public void initialize(NodeList children) {
        super.initialize(children);

        dbnsfpVersion = getAttribute(DBNSFP_VERSION);
        if (dbnsfpVersion == null) dbnsfpVersion= getPipelineProperty (DBNSFP_VERSION);
        
        //I'm going to force a declaration of what version they are using, Nix
        if (dbnsfpVersion == null) throw new IllegalArgumentException ("Failed to parse your "+DBNSFP_VERSION +". Please include it in your pipeline properties xml file.");
        else if (dbnsfpVersion.equals("2.0") == false && dbnsfpVersion.equals("2.9") == false && dbnsfpVersion.equals("2.9.2") == false && dbnsfpVersion.equals("3.0") == false && dbnsfpVersion.equals("3.1a") == false){
        	throw new IllegalArgumentException ("Only the 2.0, 2.9, 2.9.2, 3.0, 3.1a versions of "+DBNSFP_VERSION +" are supported");
        }
        
        //set column indexes, bad way of doing this!
        sift_score_col = getSiftColumn(dbnsfpVersion);
        sift_pred_col = getSiftPredColumn(dbnsfpVersion);
        polyphen_score_col = getPolyphenScoreColumn(dbnsfpVersion);
        Polyphen2_hvar_score_col = getPolyphenScoreHVARColumn(dbnsfpVersion);
        Polyphen2_hvar_pred_col = getPolyphenScoreHVARPREDColumn(dbnsfpVersion);
        lrt_score_column = getLRTScoreColumn(dbnsfpVersion);
        mt_score_column = getMTScoreColumn(dbnsfpVersion);
        mt_pred_column = getMTPredColumn(dbnsfpVersion);
        ma_pred_column = getMAPredColumn(dbnsfpVersion);
        ma_score_column = getMAScoreColumn(dbnsfpVersion);
        gerp_nr_score_column = getGerpNRColumn(dbnsfpVersion);
        gerp_score_column = getGerpColumn(dbnsfpVersion);
        phylop_score_column = getPhylopColumn(dbnsfpVersion);
        siphy_score_column = getSiphyColumn(dbnsfpVersion);
        
        //set indexes for the correct columns in a dbnsfp line, first is 0.
        if (dbnsfpVersion.equals("3.1a")){
            b37Chr_column =7;
            b37Pos_column =8;
            b37Ref_column =2;
            b37Alt_column =3;
        }
        //everything else default to:
        else {
            b37Chr_column =0;
            b37Pos_column =1;
            b37Ref_column =2;
            b37Alt_column =3;
        }
        

    }
}
