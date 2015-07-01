package operator.variant;

import buffer.variant.VariantRec;


/**
 *  Uses dbSNP info downloaded from NCBI for annotations The raw data is obtained from
 *  ftp://ftp.ncbi.nlm.nih.gov/snp/organisms/human_9606/VCF/common_all.vcf.gz
 *  and is tabix-indexed
 *
 *  Database should be retrieved via this script, which normalizes the database and does some QC
 *  Scrutil/bash/DatabaseScripts/dbsnp_cron_update
 *  ftp://ftp.ncbi.nih.gov/snp/organisms/human_9606_b142_GRCh37p13/VCF/00-All.vcf.gz"

 *
 *
 * @author brendan
 * @author keith simmon - moved under the AbstactTabixAnnotator class

 *
 */
public class DBSNPAnnotator extends AbstractTabixAnnotator {
    /**
     * Parses allele frequency annotation from the given string and
     * converts it to a property on the variant
     * @param var
     * @param str
     * @throws operator.OperationFailedException
     */
    public static final String DBSNP_PATH = "dbsnp.path";

    @Override
    protected String getPathToTabixedFile() {return searchForAttribute(DBSNP_PATH);}

    @Override
    protected boolean addAnnotationsFromString(VariantRec var, String str, int altIndex) {
        String[] toks = str.split("\t");
        String rsNum = toks[2];
        var.addAnnotation(VariantRec.RSNUM, rsNum);
        return true;
    }
}

	

