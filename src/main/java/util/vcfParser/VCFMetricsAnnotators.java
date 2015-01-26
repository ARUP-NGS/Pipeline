package util.vcfParser;

import java.util.Map;

import buffer.variant.VariantRec;

public class VCFMetricsAnnotators {

	/**
	 * Adds the AF (often Allele Frequency) property as an annotation  
	 * @author brendan
	 *
	 */
	public static class AFAnnotator implements VCFMetricsAnnotator {

		@Override
		public void addAnnotation(VariantRec var, Map<String, String> sampleMetrics) {

			String afStr = sampleMetrics.get("AF");
			try {
				Double af = Double.parseDouble(afStr);
				var.addProperty(VariantRec.AF, af);
			} 
			catch (Exception ex) {
				//ignored, we add nothing
			}
		}
		
	}
}
