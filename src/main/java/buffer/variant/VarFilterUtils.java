package buffer.variant;

import util.vcfParser.VCFParser.GTType;

/**
 * Contains static utils for obtaining a variety of oft-used variant filters
 * @author brendan
 *
 */
public class VarFilterUtils {
	
	public static VariantFilter getQualityFilter(double minQual) {
		return new QualFilter(minQual);
	}

	public static VariantFilter getHeteroFilter() {
		return new HetFilter();
	}
	
	public static VariantFilter getHomoFilter() {
		return new HomFilter();
	}

	public static VariantFilter getPopFreqFilter(double maxFreq) {
		return new PopFreqFilter(maxFreq);
	}

	public static VariantFilter getNonSynFilter() {
		return new NonSynFilter();
	}
	
	public static VariantFilter getfilterFilter() {
		return new filterFilter();
	}
	
	

	static class QualFilter implements VariantFilter {

		final double qual;
		
		public QualFilter(double minQual) {
			this.qual = minQual;
		}
		
		@Override
		public boolean passes(VariantRec rec) {
			return rec.getQuality() >= qual;
		}
		
	}


	static class PopFreqFilter implements VariantFilter {

		final double maxFreq;
		
		public PopFreqFilter(double maxFreq) {
			this.maxFreq = maxFreq;
		}
		
		@Override
		public boolean passes(VariantRec rec) {
			Double freq = rec.getProperty(VariantRec.POP_FREQUENCY);
			return freq == null || freq < maxFreq;
		}
		
	}
	
	static class HomFilter implements VariantFilter {

		@Override
		public boolean passes(VariantRec rec) {
			return rec.getZygosity() == GTType.HOM;
		}
		
	}

	static class NonSynFilter implements VariantFilter {

		@Override
		public boolean passes(VariantRec rec) {
			String func = rec.getAnnotation(VariantRec.EXON_FUNCTION);
			return func.contains("nonsyn") || func.contains("delet") || func.contains("insert") || func.contains("frame") || func.contains("splice");
		}
		
	}
	
	static class HetFilter implements VariantFilter {

		@Override
		public boolean passes(VariantRec rec) {
			return rec.getZygosity() == GTType.HET;
		}
		
	}
	
	static class filterFilter implements VariantFilter {
		final String filterField = "";
		
		@Override
		public boolean passes(VariantRec rec) {
			String filter = rec.getAnnotation(VariantRec.VCF_FILTER);
			if ( filter != null  && !filter.equals("LowQual") ) {
				return true;
			} else {
				return false;
			}
		}
	}
	
}
