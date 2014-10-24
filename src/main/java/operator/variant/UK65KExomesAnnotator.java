package operator.variant;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import pipeline.Pipeline;
import buffer.variant.VariantRec;


/**
 * Adds the Allele Frequencies and Counts, total and for ethnic groups,
 * as annotation to a variant. Uses a UK65KExomesDB object to provide db info. 
 * @author daniel
 *
 */
public class UK65KExomesAnnotator extends Annotator {

	private UK65KExomesDB UK65KDB = null;
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		if (UK65KDB == null) {
			String UK65KDBFile = this.getPipelineProperty("65k.db.path");
			if (UK65KDBFile == null) {
				throw new OperationFailedException("No path to 65K db specified in pipeline properties", this);
			}
			else {
				Logger.getLogger(Pipeline.primaryLoggerName).info("Looking up 65K frequency db info in : " + UK65KDBFile);
			}
			try {
				UK65KDB = new UK65KExomesDB(new File(UK65KDBFile));
			} catch (IOException e) {
				e.printStackTrace();
				throw new OperationFailedException("Error opening 65K exome db file: " + e.getMessage(), this);
			}
		}
		
		String[] dbInfo;
		try {
			dbInfo = UK65KDB.getInfoForPostion(var.getContig(), var.getStart());
			if (dbInfo != null) {
				var.addProperty(VariantRec.EXOMES_65K_FREQ, Double.valueOf(dbInfo[0]));
				var.addProperty(VariantRec.EXOMES_65K_AC_HOM, Double.valueOf(dbInfo[1]));
				var.addProperty(VariantRec.EXOMES_65K_AC_HET, Double.valueOf(dbInfo[2]));
				var.addProperty(VariantRec.EXOMES_65K_AFR_FREQ, Double.valueOf(dbInfo[3]));
				var.addProperty(VariantRec.EXOMES_65K_AFR_HOM, Double.valueOf(dbInfo[4]));
				var.addProperty(VariantRec.EXOMES_65K_AFR_HET, Double.valueOf(dbInfo[5]));
				var.addProperty(VariantRec.EXOMES_65K_AMR_FREQ, Double.valueOf(dbInfo[6]));
				var.addProperty(VariantRec.EXOMES_65K_AMR_HOM, Double.valueOf(dbInfo[7]));
				var.addProperty(VariantRec.EXOMES_65K_AMR_HET, Double.valueOf(dbInfo[8]));
				var.addProperty(VariantRec.EXOMES_65K_EAS_FREQ, Double.valueOf(dbInfo[9]));
				var.addProperty(VariantRec.EXOMES_65K_EAS_HOM, Double.valueOf(dbInfo[10]));
				var.addProperty(VariantRec.EXOMES_65K_EAS_HET, Double.valueOf(dbInfo[11]));
				var.addProperty(VariantRec.EXOMES_65K_FIN_FREQ, Double.valueOf(dbInfo[12]));
				var.addProperty(VariantRec.EXOMES_65K_FIN_HOM, Double.valueOf(dbInfo[13]));
				var.addProperty(VariantRec.EXOMES_65K_FIN_HET, Double.valueOf(dbInfo[14]));
				var.addProperty(VariantRec.EXOMES_65K_NFE_FREQ, Double.valueOf(dbInfo[15]));
				var.addProperty(VariantRec.EXOMES_65K_NFE_HOM, Double.valueOf(dbInfo[16]));
				var.addProperty(VariantRec.EXOMES_65K_NFE_HET, Double.valueOf(dbInfo[17]));
				var.addProperty(VariantRec.EXOMES_65K_SAS_FREQ, Double.valueOf(dbInfo[18]));
				var.addProperty(VariantRec.EXOMES_65K_SAS_HOM, Double.valueOf(dbInfo[19]));
				var.addProperty(VariantRec.EXOMES_65K_SAS_HET, Double.valueOf(dbInfo[20]));
				
				//Allele Frequency, Hom Freq, Het Freq * All, AFR, AMR, EAS, FIN, NFE, SAS
				/*
				var.addProperty(VariantRec.ARUP_OVERALL_FREQ, Double.parseDouble(dbInfo[0]));
				var.addAnnotation(VariantRec.ARUP_FREQ_DETAILS, dbInfo[1]);
				*/
				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	
}
