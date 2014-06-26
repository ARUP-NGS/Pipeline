package operator.annovar;

import buffer.variant.VariantRec;

public class EffectPredictionAnnotator extends Annotator {

	@Override
	public void annotateVariant(VariantRec var) {
		//Integer effect = getEffectPredictionSimple(var);
		//var.addProperty(VariantRec.EFFECT_PREDICTION, new Double(effect));

		Double val = getEffectPredictionLinearWeight(var);
		var.addProperty(VariantRec.EFFECT_PREDICTION2, val);

	}

	public static double getEffectPredictionLinearWeight(VariantRec rec) {
		
		//All splicing variants immediately get a rather high score
		String varType = rec.getAnnotation(VariantRec.VARIANT_TYPE);

		if (varType != null && varType.contains("splic")) {
			return 15;
		}
		
		
		String exonFuncType = rec.getAnnotation(VariantRec.EXON_FUNCTION);
		if (exonFuncType != null) {
			if (exonFuncType.contains("frameshift")) {
				if (exonFuncType.contains("non")) {
					return Math.min(15, rec.getRef().length());
				}
				else {
					return 20;
				}
			}
			if (exonFuncType.contains("stopgain"))
				return 20;
			if (exonFuncType.contains("stoploss"))
				return 15;
		}
		
		//Last changes are from 6/25/2012 when it was realized that there was a
		//mixup in the order of the values in the Optimizer ...
		double siftWeight = -2.22; //-1.24;
		double ppWeight = 4.98; //2.78;
		double mtWeight = 11.29; //17.29;
		double phylopWeight = 1.52; //0.31;
		double gerpWeight = -0.495; //-0.27;
		double siphyWeight = -0.22; //-0.12;
		double lrtWeight = 2.68; //1.5;
				
		Double sift = rec.getProperty(VariantRec.SIFT_SCORE);
		Double pp = rec.getProperty(VariantRec.POLYPHEN_SCORE);
		Double mt = rec.getProperty(VariantRec.MT_SCORE);
		Double phylop = rec.getProperty(VariantRec.PHYLOP_SCORE);
		Double gerp = rec.getProperty(VariantRec.GERP_SCORE);
		Double siphy = rec.getProperty(VariantRec.SIPHY_SCORE);
		Double lrt = rec.getProperty(VariantRec.LRT_SCORE);
		
		if (sift == null || Double.isNaN(sift))
			sift = 0.0;
		if (pp == null || Double.isNaN(pp))
			pp = 0.0;
		if (mt == null || Double.isNaN(mt))
			mt = 0.0;
		if (phylop == null || Double.isNaN(phylop))
			phylop = 0.0;
		if (gerp == null || Double.isNaN(gerp))
			gerp = 0.0;
		if (siphy == null || Double.isNaN(siphy))
			siphy = 0.0;
		if (lrt == null || Double.isNaN(lrt))
			lrt = 0.0;
		
		double val = (sift-siftMean)/siftStdev * siftWeight 
					+ (pp-ppMean)/ppStdev * ppWeight
					+ (mt-mtMean)/mtStdev * mtWeight
					+ (phylop-phylopMean)/phylopStdev * phylopWeight
					+ (gerp-gerpMean)/gerpStdev * gerpWeight
					+ (siphy-siphyMean)/siphyStdev * siphyWeight
					+ (lrt-lrtMean)/lrtStdev * lrtWeight;
		
		return val;
	}
	
	/**
	 * Returns an integer that attempts to summarize sift, polyphen, mutation taster, and phylop
	 * scores into simple categorization
	 * Higher scores are more indicative of damagingness, the highest possible score, 5, means that
	 * every tool thought the given variant was damaging.
	 * Scores near zero are inconclusive, either because no data is available or because the scores conflict
	 * Negative scores mean that the tools believe the variant to be benign
	 * @param rec
	 * @return
	 */
	public static Integer getEffectPredictionSimple(VariantRec rec) {
		Double sift = rec.getProperty(VariantRec.SIFT_SCORE);
		Double pp = rec.getProperty(VariantRec.POLYPHEN_SCORE);
		Double mt = rec.getProperty(VariantRec.MT_SCORE);
		Double phylop = rec.getProperty(VariantRec.PHYLOP_SCORE);
		Double gerp = rec.getProperty(VariantRec.GERP_SCORE);
		
		String exonFuncType = rec.getAnnotation(VariantRec.EXON_FUNCTION);
		if (exonFuncType != null) {
			if (exonFuncType.contains("frameshift")) {
				if (exonFuncType.contains("non"))
					return 3;
				else
					return 5;
			}
			if (exonFuncType.contains("stopgain"))
				return 5;
			if (exonFuncType.contains("stoploss"))
				return 4;
			if (exonFuncType.contains("splice"))
				return 4;
		}
		
		int siftVal = 0;
		int ppVal = 0;
		int mtVal = 0;
		int phylopVal = 0;
		int gerpVal = 0;
		
		int scored = 0;
		
		if (sift != null) {
			if (sift < 0.0001)
				siftVal = 1;
			else if (sift < 0.1)
				siftVal = 0;
			else 
				siftVal = -1;
			scored++;
		}
		
		if (pp != null) {
			if (pp > 0.899)
				ppVal = 1;
			else if (pp > 0.5)
				ppVal = 0;
			else 
				ppVal = -1;
			scored++;
		}
		
		if (mt != null) {
			if (mt > 0.899)
				mtVal = 1;
			else if (mt > 0.5)
				mtVal = 0;
			else 
				mtVal = -1;
			scored++;
		}
		
		if (phylop != null) {
			if (phylop > 0.899)
				phylopVal = 1;
			else if (phylop > 0.5)
				phylopVal = 0;
			else 
				phylopVal = -1;
			scored++;
		}
		
		
		if (gerp != null) {
			if (gerp > 3) 
				gerpVal = 1;
			else if (gerp > 1)
				gerpVal = 0;
			else
				gerpVal = -1;
			scored++;
		}
		
		int sum = siftVal + ppVal + mtVal + phylopVal + gerpVal;
		return sum;
	}
	
	public static final double gerpMean = 3.053;
	public static final double gerpStdev = 3.106;
	
	public static final double siftMean = 0.226;
	public static final double siftStdev = 0.2923;
	
	public static final double ppMean = 0.584;
	public static final double ppStdev = 0.4323;
	
	public static final double mtMean = 0.5604;
	public static final double mtStdev = 0.4318;
	
	public static final double phylopMean = 1.2932;
	public static final double phylopStdev = 1.1921;
	
	public static final double siphyMean = 11.1355;
	public static final double siphyStdev = 5.1848;
	
	public static final double lrtMean = 0.08391;
	public static final double lrtStdev = 0.20298;

}
