package operator.variant;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import pipeline.Pipeline;
import buffer.variant.VariantRec;

public class FPAnnotator extends Annotator{
	 	HaloplexDB haloDB = null;
	     
	    @Override
		public void annotateVariant(VariantRec var) throws OperationFailedException {
			if (haloDB == null) {
				String HaloPlexDBFile = this.getPipelineProperty("haloplex.db.path");
				if (HaloPlexDBFile == null) {
					throw new OperationFailedException("No path to haloplex db specified in pipeline properties", this);
				}
				else {
					Logger.getLogger(Pipeline.primaryLoggerName).info("Looking up ARUP frequency db info in : " + HaloPlexDBFile);
				}
				try {
					haloDB = new HaloplexDB(new File(HaloPlexDBFile));
				} catch (IOException e) {
					e.printStackTrace();
					throw new OperationFailedException("Error opening HaloPlex db file: " + e.getMessage(), this);
				}
			}
			
			String[] dbInfo;
			try {
				dbInfo = haloDB.getInfoForPostion(var.getContig(), var.getStart());
				if (dbInfo != null) {
					//var.addProperty(VariantRec.ARUP_OVERALL_FREQ, Double.parseDouble(dbInfo[0]));
					//var.addAnnotation(VariantRec.ARUP_FREQ_DETAILS, dbInfo[1]);
					var.addProperty(VariantRec.HALOPLEX_PANEL_FREQ, Double.parseDouble(dbInfo[0]));
					
				}
				else {
					//var.addProperty(VariantRec.ARUP_OVERALL_FREQ, 0.0);
					//var.addAnnotation(VariantRec.ARUP_FREQ_DETAILS, "Total samples: 0");
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
}
