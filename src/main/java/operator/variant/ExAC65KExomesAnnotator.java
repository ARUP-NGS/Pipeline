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
 * as annotation to a variant. Uses a ExAC65KExomesDB object to provide db info. 
 * @author daniel
 *
 */
public class ExAC65KExomesAnnotator extends Annotator {

	private ExAC65KExomesDB ExAC65KDB = null;
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException, NullPointerException {
		if (ExAC65KDB == null) {
			String ExAC65KDBFile = this.getPipelineProperty("65k.db.path");
			if (ExAC65KDBFile == null) {
				throw new OperationFailedException("No path to 65K db specified in pipeline properties", this);
			}
			else {
				Logger.getLogger(Pipeline.primaryLoggerName).info("Looking up 65K frequency db info in : " + ExAC65KDBFile);
			}
			try {
				ExAC65KDB = new ExAC65KExomesDB(new File(ExAC65KDBFile));
			} catch (IOException e) {
				e.printStackTrace();
				throw new OperationFailedException("Error opening 65K exome db file: " + e.getMessage(), this);
			}
		}
		
		String[] dbInfo;
		try {
			dbInfo = ExAC65KDB.getInfoForPostion(var.getContig(), var.getStart());
			Double[] dbDoubles = new Double[21];
			if(dbInfo!=null){
				for(int el = 0; el < dbInfo.length; el++){
					if(!Double.isNaN(Double.valueOf(dbInfo[el])))
						dbDoubles[el] = Double.valueOf(dbInfo[el]);
					else
						dbDoubles[el] = Double.NaN;
				}
				if (dbDoubles != null) {
					if(!dbDoubles[0].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_FREQ, dbDoubles[0]);
					if(!dbDoubles[1].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_AC_HOM, dbDoubles[1]);
					if(!dbDoubles[2].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_AC_HET, dbDoubles[2]);
					if(!dbDoubles[3].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_AFR_FREQ, dbDoubles[3]);
					if(!dbDoubles[4].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_AFR_HOM, dbDoubles[4]);
					if(!dbDoubles[5].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_AFR_HET, dbDoubles[5]);
					if(!dbDoubles[6].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_AMR_FREQ, dbDoubles[6]);
					if(!dbDoubles[7].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_AMR_HOM, dbDoubles[7]);
					if(!dbDoubles[8].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_AMR_HET, dbDoubles[8]);
					if(!dbDoubles[9].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_EAS_FREQ, dbDoubles[9]);
					if(!dbDoubles[10].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_EAS_HOM, dbDoubles[10]);
					if(!dbDoubles[11].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_EAS_HET, dbDoubles[11]);
					if(!dbDoubles[12].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_FIN_FREQ, dbDoubles[12]);
					if(!dbDoubles[13].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_FIN_HOM, dbDoubles[13]);
					if(!dbDoubles[14].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_FIN_HET, dbDoubles[14]);
					if(!dbDoubles[15].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_NFE_FREQ, dbDoubles[15]);
					if(!dbDoubles[16].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_NFE_HOM, dbDoubles[16]);
					if(!dbDoubles[17].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_NFE_HET, dbDoubles[17]);
					if(!dbDoubles[18].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_SAS_FREQ, dbDoubles[18]);
					if(!dbDoubles[19].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_SAS_HOM, dbDoubles[19]);
					if(!dbDoubles[20].equals(Double.NaN))
						var.addProperty(VariantRec.EXOMES_65K_SAS_HET, dbDoubles[20]);
					
					//Allele Frequency, Hom Freq, Het Freq * All, AFR, AMR, EAS, FIN, NFE, SAS
					/*
					var.addProperty(VariantRec.ARUP_OVERALL_FREQ, Double.parseDouble(dbInfo[0]));
					var.addAnnotation(VariantRec.ARUP_FREQ_DETAILS, dbInfo[1]);
					*/
				}	
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("AHH IT DIED");
			e.printStackTrace();
		}
		
		
		
	}

	
}
