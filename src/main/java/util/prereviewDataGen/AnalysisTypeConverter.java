package util.prereviewDataGen;

public class AnalysisTypeConverter {

	/**
	 * Converts an analysisType string found in a sampleManifest into a three-letter 
	 * analysis type abbreviation that NGS Web can recognize. 
	 * @param analysisType
	 * @return
	 */
	public String convert(String analysisType) {
		String aType = analysisType.toLowerCase();
		if (aType.contains("exome") || aType.contains("trackex")) {
			return "EXO";
		}
		
		if (aType.contains("retinitis") || aType.contains(" rp ")) {
			return "RET";
		}
		
		if (aType.contains("aort") || aType.contains("2.0")) {
			return "AOR";
		}
		
		if (aType.contains("vasc")) {
			return "VAS";
		}
		
		if (aType.contains("prfev") || aType.contains("periodic fever")) {
			return "PRF";
		}
		
		if (aType.contains("holopro")) {
			return "HOS";
		}
		
		
		return analysisType;
	}
}
