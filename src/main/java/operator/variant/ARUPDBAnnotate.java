package operator.variant;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import pipeline.Pipeline;
import buffer.variant.VariantRec;


/**
 * Adds the ARUP_FREQ annotation to a variant. Uses an ARUPDB object to provide db info. 
 * @author brendan
 *
 */
public class ARUPDBAnnotate extends Annotator {

	private ARUPDB arupDB = null;
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		if (arupDB == null) {
			String arupDBFile = this.getPipelineProperty("arup.db.path");
			if (arupDBFile == null) {
				throw new OperationFailedException("No path to ARUP db specified in pipeline properties", this);
			}
			else {
				Logger.getLogger(Pipeline.primaryLoggerName).info("Looking up ARUP frequency db info in : " + arupDBFile);
			}
			try {
				arupDB = new ARUPDB(new File(arupDBFile));
			} catch (IOException e) {
				e.printStackTrace();
				throw new OperationFailedException("Error opening ARUP db file: " + e.getMessage(), this);
			}
		}
		
		String[] dbInfo;
		try {
			dbInfo = arupDB.getInfoForPostion(var.getContig(), var.getStart());
			if (dbInfo != null) {
				var.addProperty(VariantRec.ARUP_OVERALL_FREQ, Double.parseDouble(dbInfo[0]));
				var.addAnnotation(VariantRec.ARUP_FREQ_DETAILS, dbInfo[1]);
				
			}
			else {
				var.addProperty(VariantRec.ARUP_OVERALL_FREQ, 0.0);
				var.addAnnotation(VariantRec.ARUP_FREQ_DETAILS, "Total samples: 0");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	
}
