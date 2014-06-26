package operator.bcrabl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import buffer.BAMFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * This is a basic implementation of the BCR-ABL report generator
 * @author brendan
 *
 */
public class SimpleReportGenerator {

	public static final String NEGATIVE_RESULT_MESSAGE = "No variants were detected by this assay.";
	
	CisTransClassifier cisTransHandler = new CisTransClassifier();
	
	
	final String[] snpsToIgnore = new String[]{ "K247R", "Y320C", "E499E", "F311V", "T240T", "T315T" }; 
	
	public BCRABLReport getReportForSample(Double meanCoverage, VariantPool vars, BAMFile bam) {
		BCRABLReport report = new BCRABLReport();
		
		report.setMeanCoverage(meanCoverage);
		
		
		if (vars == null || vars.size() == 0) {
			report.setMessage(NEGATIVE_RESULT_MESSAGE);
			return report;
		}
		
		//Sanity check
		if (vars.getContigCount() > 1) {
			Logger.getLogger(getClass()).error("Incorrect number of contigs found for BCR-ABL sample!");
			report.setMessage("There was an error generating this report: An incorrect number of contigs (" + vars.getContigCount() + ") was found");
			return report;
		}
		
		List<VariantRec> varList = vars.getVariantsForContig( vars.getContigs().iterator().next() );
		
		if (varList.size()==0) {
			report.setMessage(NEGATIVE_RESULT_MESSAGE);
			return report;
		}

		List<String> resistanceComments = new ArrayList<String>();
		
		if (varList.size() == 1) {
			report.setMessage(varList.size() + " mutation was detected.");
			String line = createLineForVariant( varList.get(0));
			report.addReportTextLine(line);
			resistanceComments.add( createVarComment(varList.get(0)));
		}
		else {
			report.setMessage(varList.size() + " mutations were detected.");
			
			for(VariantRec var : varList) {
				String line = createLineForVariant(var);
				resistanceComments.add( createVarComment(var));
				
				//Compute all possible cis/trans relationships
				String cisTransPhrase = "";
				for(VariantRec var2 : varList) {
					String phrase = computeCisTransText(bam, var, var2);
					
					if (phrase.length() > 0) {
						if (cisTransPhrase.length() > 0)
							cisTransPhrase = cisTransPhrase + ", ";
						cisTransPhrase = cisTransPhrase + phrase;
					}
				}
				
				if (cisTransPhrase.length() > 0) {
					cisTransPhrase = cisTransPhrase.trim();
					if (cisTransPhrase.endsWith(",")) {
						cisTransPhrase = cisTransPhrase.substring(0, cisTransPhrase.length()-1);
					}
					line = line + "  (" + cisTransPhrase + ")";
				}

				
				report.addReportTextLine( line );
			}
			
			
		}

		report.addReportTextLine("");
		for(String comment : resistanceComments) {
			report.addReportTextLine(comment);
		}

		return report;
	}
	
	private String createVarComment(VariantRec var) {
		//Generate ignore comment
		String pDot = var.getAnnotation("pdot").replace("p.", "");
		for(int i=0; i<snpsToIgnore.length; i++) {
			if (pDot.equals(snpsToIgnore[i])) {
				return pDot + " is a common SNP. Do not report.";
			}
		}
		
		//Generate resistance comment
		String known = var.getAnnotation("Known");
		boolean knownResistant = false;
		if (known != null) {
			knownResistant = Boolean.parseBoolean(known);
		}
		
		if (knownResistant) {
			return pDot + " has been reported to confer resistance to BCR-ABL1 tyrosine kinase inhibitors.";
		}
		else {
			return "No data on resistance for " + pDot;	
		}
	}
	
	private String createLineForVariant(VariantRec var) {
		String freqStr = "Error computing frequency";
		String depthStr = "?";
		try {
			Double depth = var.getProperty("depth");
			Double varDepth = var.getProperty("var.depth");
			
			if (depth == null || Double.isNaN(depth)) {
				depthStr = "?";
			}
			else {
				depthStr = "" + (int)Math.round(depth);
			}
			
			if (varDepth == null) {
				Logger.getLogger(getClass()).error("Could not read var.depth annotation for VariantRec: " + var);
			}
			
			if (depth > 0) {
				freqStr = formatFreq(varDepth / depth);	
			}
			else {
				freqStr = "Error computing frequency (unknown read depth)";
			}
			
		}
		catch (Exception ex) {
			Logger.getLogger(getClass()).error("Error computing alt.freq for BCR-ABL sample with VariantRec: " + var + " Exception: " + ex);	
		}
		
		return var.getAnnotation(VariantRec.PDOT).replace("p.",  "") + "  (" + var.getAnnotation(VariantRec.CDOT) + "); " + freqStr + " Coverage: " + depthStr;
	}

	/**
	 * Use the Cis/Trans classifier to generate a short string stating whether the variants are in cis, trans, or too far apart to tell
	 * @param focalVar
	 * @param otherVar
	 * @return
	 */
	private String computeCisTransText(BAMFile bam, VariantRec focalVar, VariantRec otherVar) {
		if (focalVar == otherVar) {
			return "";
		}
		
		if (! cisTransHandler.closeEnoughToCompute(focalVar, otherVar)) {
			return "";
		}
		
		CisTransResult result = cisTransHandler.computeCisTransResult(bam, focalVar, otherVar);
		if (result.getCoverage()<1) {
			return "";
		}
		else {
			
			double rawCis = result.getCisFrac();
			double rawTrans = result.getTransFrac();
			double normalizedCis = rawCis / (rawCis + rawTrans);
			
			if (normalizedCis > 0.8) {
				return "in cis with " + otherVar.getAnnotation(VariantRec.PDOT).replace("p.", "");
			}
			
			if (normalizedCis < 0.2) {
				return "in trans with " + otherVar.getAnnotation(VariantRec.PDOT).replace("p.", "");
			}
			
			return "";
		}
		
	}
	
	private String formatFreq(double freq) {
		String str = "" + (int)Math.round(freq*100.0);
		if (str.length() > 4) {
			str = str.substring(0, 4);
		}
		str = str + "%";
		return str;
	}

}
