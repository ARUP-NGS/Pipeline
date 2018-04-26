package operator.variant;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import operator.variant.ARUPDB.QueryResult;
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
		
		try {
			QueryResult dbInfo = arupDB.getInfoForVariant(var.getContig(), var.getStart(), var.getRef(), var.getAlt());
			if (dbInfo != null) {
				var.addProperty(VariantRec.ARUP_OVERALL_FREQ, dbInfo.overallFreq);
				var.addProperty(VariantRec.ARUP_HET_COUNT, dbInfo.totHets);
				var.addProperty(VariantRec.ARUP_HOM_COUNT, dbInfo.totHoms);
				var.addProperty(VariantRec.ARUP_SAMPLE_COUNT, dbInfo.totSamples);
				var.addAnnotation(VariantRec.ARUP_FREQ_DETAILS, dbInfo.details);
			}
			else {
				var.addProperty(VariantRec.ARUP_OVERALL_FREQ, 0.0);
				var.addProperty(VariantRec.ARUP_HET_COUNT, 0.0);
				var.addProperty(VariantRec.ARUP_HOM_COUNT, 0.0);
				var.addProperty(VariantRec.ARUP_SAMPLE_COUNT, 0.0);
				var.addAnnotation(VariantRec.ARUP_FREQ_DETAILS, "Total samples: 0");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	
}

