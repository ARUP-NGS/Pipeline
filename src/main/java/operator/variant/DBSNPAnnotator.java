package operator.variant;

import buffer.variant.VariantRec;


/**
 * Uses dbSNP info downloaded from NCBI for annotations The raw data is obtained from
 * ftp://ftp.ncbi.nlm.nih.gov/snp/organisms/human_9606/VCF/common_all.vcf.gz
 *  and is tabix-indexed
 *
 *  This vcf file should be run through VCFtidy inorder to deconvolute variants recorded on
 *  a single line (e.g. ref=A, alt=AT,AAT,ACT) if a variant is encountered with this attribute
 *  an error will be thrown and pipeline will exit.  THe pipeline properties files should point
 *  to the vcftidy'd database.
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
    protected boolean addAnnotationsFromString(VariantRec var, String str) {
        String[] toks = str.split("\t");
        String rsNum = toks[2];
        var.addAnnotation(VariantRec.RSNUM, rsNum);
        return true;
    }
}

	

