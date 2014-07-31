package operator.variant;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import pipeline.Pipeline;
import buffer.variant.VariantRec;

/*
 * Using UCSC's PFAM and Scop tables to annotate a variant's protein domain if available
 * Borrowing heavily from Brendan and David's code
 * @author daniel
 * 
 */

public class COSMICAnnotator extends Annotator{
	 	COSMICCodingDB cosmicDB = null;
	     
	    @Override
		public void annotateVariant(VariantRec var) throws OperationFailedException {
			if (cosmicDB == null) {
				String CosmicCodingDBFile = this.getPipelineProperty("cosmic.db.path");
				if (CosmicCodingDBFile == null) {
					throw new OperationFailedException("No path to COSMIC coding DB specified in pipeline properties", this);
				}
				else {
					Logger.getLogger(Pipeline.primaryLoggerName).info("Looking up cDot,pDot,COSMIC ID, and COSMIC Counts for mutation : " + CosmicCodingDBFile);
				}
				try {
					cosmicDB = new COSMICCodingDB(new File(CosmicCodingDBFile));
				} catch (IOException e) {
					e.printStackTrace();
					throw new OperationFailedException("Error opening COSMIC vcf: " + e.getMessage(), this);
				}
			}
			String[] dbInfo;
			try {
				dbInfo = cosmicDB.getInfoForPosition(var.getContig(), var.getStart());
				if (dbInfo != null) {
					if(dbInfo[0].equals("n/a") || dbInfo[1].equals("n/a") || dbInfo[2].equals("n/a") || dbInfo[3].equals("n/a")){
						var.addAnnotation(VariantRec.COSMIC_ID, dbInfo[0]);
						var.addAnnotation(VariantRec.COSMIC_COUNT, dbInfo[3]);
					}
				}
				else {
					//var.addProperty(VariantRec.ARUP_OVERALL_FREQ, 0.0);
					//var.addAnnotation(VariantRec.ARUP_FREQ_DETAILS, "Total samples: 0");
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
		}
}
