package operator.variant;

import buffer.variant.VariantRec;


/**
 * This was created using clinvar database normalized with the following script.
 * Scrutil/bash/DatabaseScripts/clinvar_cron_update
 *
 * The database is downloaded and normalized to remove multiallelic lines, Biallelic blocks, and
 * non unique variants.
 *
 * @annontations from clinvar include
 * CLNSIG (clinical significance)
 * CLNDBN (disease name)
 * CLNDSDBID (database id)
 * CLNREVSTAT (review status )
 * CLNDSDB (clinical database)
 *
 *
 * @author keith simmon - moved under the AbstactTabixAnnotator class
 * @date April 12, 2015
 *
 */

public class ClinVarAnnotator extends AbstractTabixAnnotator {
    /**
     * Parses allele frequency annotation from the given string and
     * converts it to a property on the variant
     * @param var
     * @param str
     * @throws operator.OperationFailedException
     */
    public static final String CLINVAR_PATH = "clinvar.path";


    @Override
    protected String getPathToTabixedFile() {
        return searchForAttribute(CLINVAR_PATH);
    }

    @Override
    protected boolean addAnnotationsFromString(VariantRec var, String val, int altIndex) {
        String[] toks = val.split("\t")[7].split(";");
        if (val.split("\t")[7].contains("=")) {
            var.addAnnotation(VariantRec.CLNSIG, toks[0].split("=")[1]);
            var.addAnnotation(VariantRec.CLNDBN, toks[1].split("=")[1]);
            var.addAnnotation(VariantRec.CLNDSDBID, toks[2].split("=")[1]);
            var.addAnnotation(VariantRec.CLNDSDB, toks[3].split("=")[1]);
            var.addAnnotation(VariantRec.CLNREVSTAT, toks[4].split("=")[1]);
            return true;
        }
        else {
            return false;
        }
    }
}
	

