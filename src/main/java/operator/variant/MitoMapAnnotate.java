package operator.variant;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import pipeline.Pipeline;
import buffer.variant.VariantRec;


/**
 * Adds the ARUP_FREQ annotation to a variant. Uses an MitoMapFreqDB object to provide db info. 
 * @author brendan
 *
 */
public class MitoMapAnnotate extends Annotator {

	private MitoMapFreqDB mitoMapFreqDB = null;
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		if (mitoMapFreqDB == null) {
			String mitoMapFreqDBFile = this.getPipelineProperty("mitomap.freq.db.path");
			if (mitoMapFreqDBFile == null) {
				
				throw new OperationFailedException("No path to MitoMap db specified in pipeline properties", this);
			}
			else {
				Logger.getLogger(Pipeline.primaryLoggerName).info("Looking up MitoMap frequency db info in : " + mitoMapFreqDBFile);
			}
			try {
				mitoMapFreqDB = new MitoMapFreqDB(new File(mitoMapFreqDBFile));
			} catch (IOException e) {
				e.printStackTrace();
				throw new OperationFailedException("Error opening MitoMap db file: " + e.getMessage(), this);
			}
		}
		
		String[] dbInfo;
		try {
			dbInfo = mitoMapFreqDB.getInfoForPosition(var.getContig(), var.getStart(), var.getAlt());
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
