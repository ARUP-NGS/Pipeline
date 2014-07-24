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

public class PfamScopAnnotator extends Annotator{
	 	DomainDB PfamScop = null;
	     
	    @Override
		public void annotateVariant(VariantRec var) throws OperationFailedException {
			if (PfamScop == null) {
				String PfamScopDBFile = this.getPipelineProperty("pfam.scop.db.path");
				if (PfamScopDBFile == null) {
					throw new OperationFailedException("No path to Pfam/Scop DB specified in pipeline properties", this);
				}
				else {
					Logger.getLogger(Pipeline.primaryLoggerName).info("Looking up Protein Domain Information : " + PfamScopDBFile);
				}
				try {
					PfamScop = new DomainDB(new File(PfamScopDBFile));
				} catch (IOException e) {
					e.printStackTrace();
					throw new OperationFailedException("Error opening Pfam/Scop db file: " + e.getMessage(), this);
				}
			}
		//pfamAC,pfamID,pfamDesc,domainName	
			String[] dbInfo;
			try {
				dbInfo = PfamScop.getInfoForPostion(var.getContig(), var.getStart());
				if (dbInfo != null) {
					if(dbInfo[0]!="n/a" || dbInfo[1]!="n/a" || dbInfo[2]!="n/a" || dbInfo[3]!="n/a" ){
						var.addAnnotation(VariantRec.PFAM_AC, dbInfo[0]);
						var.addAnnotation(VariantRec.PFAM_ID, dbInfo[1]);
						var.addAnnotation(VariantRec.PFAM_DESC, dbInfo[2]);
						var.addAnnotation(VariantRec.SCOP_DOMAIN, dbInfo[3]);
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
