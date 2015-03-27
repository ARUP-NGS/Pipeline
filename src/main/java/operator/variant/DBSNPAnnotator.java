package operator.variant;

import buffer.variant.VariantRec;


/**
 * Uses dbSNP info downloaded directly from NCBI for annotations The raw data is obtained from
 * ftp://ftp.ncbi.nlm.nih.gov/snp/organisms/human_9606/VCF/common_all.vcf.gz
 *  and is tabix-indexed
 * @author brendan
 * @author keith simmon - moved under the AbstactTabixAnnotator
 *
 */
public class DBSNPAnnotator extends AbstractTabixAnnotator {

	public static final String DBSNP_PATH = "dbsnp.path";

    @Override
    protected String getPathToTabixedFile() {return searchForAttribute(DBSNP_PATH);}

    @Override
    protected boolean addAnnotationsFromString(VariantRec var, String str) {

        String[] toks = str.split("\t");
        if (! toks[0].equals(var.getContig())) {
            //We expect that sometimes we'll not get the right contig
            return false;
        }
        if (! toks[1].equals("" + var.getStart())) {
            //We expect that sometimes we'll not get the right position (not sure why exactly... tabix doesn't work perfectly I guess			return;
        }
        if (toks[4].equals(var.getAlt())) {
            String rsNum = toks[2];
            var.addAnnotation(VariantRec.RSNUM, rsNum);
            return true;
        }
        return false;
    }
}

	

