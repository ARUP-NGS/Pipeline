package operator.variant;

import java.io.IOException;
import java.util.logging.Logger;

import buffer.variant.VariantRec;
import operator.OperationFailedException;
import operator.annovar.Annotator;
import pipeline.Pipeline;
import util.flatFilesReader.DBNSFPReader;

/**
 * Reads dbNSFP info and provides numerous annotations for nonsynonymous SNPs
 * @author brendan
 *
 */
public class DBNSFPAnnotator extends Annotator {

	private DBNSFPReader reader = new DBNSFPReader();
	private int examined = 0;
	private int annotated = 0;
	
	public void performOperation() throws OperationFailedException {
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
