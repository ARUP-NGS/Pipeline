package operator.variant;

import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import pipeline.Pipeline;
import util.flatFilesReader.DBNSFPReader;
import buffer.variant.VariantRec;

/**
 * Reads dbNSFP info and provides numerous annotations for nonsynonymous SNPs
 * @author brendan
 *
 */
public class DBNSFPAnnotator extends Annotator {

	public static final String DBNSFP_PATH = "dbnsfp.path";
	private DBNSFPReader reader = null; 
	private int examined = 0;
	private int annotated = 0;
	
	public void performOperation() throws OperationFailedException {
		String pathToDBNSFP = this.getPipelineProperty(DBNSFP_PATH);
		if (pathToDBNSFP != null) {
			Logger.getLogger(Pipeline.primaryLoggerName).info("dbNSFP reader using directory : " + pathToDBNSFP);
			reader = new DBNSFPReader(pathToDBNSFP);
		}
		else { 
			Logger.getLogger(Pipeline.primaryLoggerName).info("dbNSFP reader using default base directory");
			reader = new DBNSFPReader();
		}
		
		super.performOperation();
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("dbNSFP annotator annotated " + annotated + " of " + examined + " variants found");
	}
	
	@Override
	public void annotateVariant(VariantRec var) {
		examined++;
		if (! var.isSNP()) {
			return;
		}
		String contig = var.getContig();
		int pos = var.getStart();
		char alt = var.getAlt().charAt(0);
		
		try {
			//System.out.println("Requesting " + contig + " : " + pos);
			boolean ok = reader.advanceTo(contig, pos, alt);
			if (ok) {
				Double gerp = reader.getValue(DBNSFPReader.GERP);
				var.addProperty(VariantRec.GERP_SCORE, gerp);
				
				Double sift = reader.getValue(DBNSFPReader.SIFT);
				var.addProperty(VariantRec.SIFT_SCORE, sift);
				
				Double siphy = reader.getValue(DBNSFPReader.SIPHY);
				var.addProperty(VariantRec.SIPHY_SCORE, siphy);
				
				Double ma = reader.getValue(DBNSFPReader.MA);
				var.addProperty(VariantRec.MA_SCORE, ma);
				
				Double slr = reader.getValue(DBNSFPReader.SLR_TEST);
				var.addProperty(VariantRec.SLR_TEST, slr);
				
				Double ppHvar = reader.getValue(DBNSFPReader.PP_HVAR);
				var.addProperty(VariantRec.POLYPHEN_HVAR_SCORE, ppHvar);

				Double gerpNR = reader.getValue(DBNSFPReader.GERP_NR);
				var.addProperty(VariantRec.GERP_NR_SCORE, gerpNR);
				
				Double lrt = reader.getValue(DBNSFPReader.LRT);
				var.addProperty(VariantRec.LRT_SCORE, lrt);

				Double phylop = reader.getValue(DBNSFPReader.PHYLOP);
				var.addProperty(VariantRec.PHYLOP_SCORE, phylop);

				Double mt = reader.getValue(DBNSFPReader.MT);
				var.addProperty(VariantRec.MT_SCORE, mt);

				Double pp = reader.getValue(DBNSFPReader.PP);
				var.addProperty(VariantRec.POLYPHEN_SCORE, pp);

				Double popFreq = reader.getValue(DBNSFPReader.TKG);
				if (! Double.isNaN(popFreq))
					var.addProperty(VariantRec.POP_FREQUENCY, popFreq);
				
				Double amrFreq = reader.getValue(DBNSFPReader.TKG_AMR);
				if (! Double.isNaN(amrFreq)) {
					var.addProperty(VariantRec.AMR_FREQUENCY, amrFreq);
				}
				
				Double eurFreq = reader.getValue(DBNSFPReader.TKG_EUR);
				if (! Double.isNaN(eurFreq)) {
					var.addProperty(VariantRec.EUR_FREQUENCY, eurFreq);
				}
				
				Double afrFreq = reader.getValue(DBNSFPReader.TKG_AFR);
				if (! Double.isNaN(afrFreq)) {
					var.addProperty(VariantRec.AFR_FREQUENCY, afrFreq);
				}
				
				Double asnFreq = reader.getValue(DBNSFPReader.TKG_ASN);
				if (! Double.isNaN(asnFreq)) {
					var.addProperty(VariantRec.ASN_FREQUENCY, asnFreq);
				}

				Double espFreq = reader.getValue(DBNSFPReader.ESP5400);
				if (!Double.isNaN(espFreq))
					var.addProperty(VariantRec.EXOMES_FREQ, espFreq);
				
				annotated++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
