package operator.variant;

import operator.annovar.Annotator;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * Computes an FP score for all variants in the pool
 * @author brendan
 *
 */
public class FPComputer extends Annotator {

	/**
	 * Compute the FP score for all variants in the pool and add it as the FALSEPOS_PROB variant 
	 * @param pool
	 */
	public static void computeFPForPool(VariantPool pool) {
		for(String contig : pool.getContigs()) {
			for(VariantRec rec : pool.getVariantsForContig(contig)) {
				Double fp = computeFPScore(rec);
				rec.addProperty(VariantRec.FALSEPOS_PROB, fp);
			}
		}
	}
	
	@Override
	public void annotateVariant(VariantRec rec) {
		Double fp = computeFPScore(rec);
		rec.addProperty(VariantRec.FALSEPOS_PROB, fp);	
	}

	/**
	 * Compute and return the FP score for the given variant. 
	 * @param rec
	 * @return
	 */
	public static Double computeFPScore(VariantRec rec) {
		Double T = rec.getProperty(VariantRec.DEPTH);
		Double X = rec.getProperty(VariantRec.VAR_DEPTH);
		
		if (T > 250) {
			X = 250 * X/T;
			T = 250.0;
		}
		
		if (T == null || X == null) {
			return Double.NaN;
		}
		
		if (T < 6) {
			//System.out.println("T: " + T + " X:" + X + " skipping, coverage too low (" +  T + ")");
			return Double.NaN;
		}
		//Compute het prob
		//Each read has 50% chance of coming from source with a non-reference base
		double hetProb = binomPDF((int)Math.round(X), (int)Math.round(T), 0.5);
		
		
		//Compute homo prob
		double homProb = binomPDF((int)Math.round(X), (int)Math.round(T), 0.99);
		
		//Compute error prob
		double errProb = binomPDF((int)Math.round(X), (int)Math.round(T), 0.25);
		
		double result = Math.log(errProb / (hetProb + homProb + errProb)); 
		//System.out.println("T: " + T + " X:" + X + " het: " + hetProb  + " hom: " + homProb + " err:" + errProb + " log ratio: " + result);
		return result;
	}

	public static double binomPDF(int k, int n, double p) {		
		return nChooseK(n, k) * Math.pow(p, k) * Math.pow(1.0-p, n-k);
	}
	
	public static double nChooseK(int n, int k) {
		double prod = 1.0;
		for(double i=1; i<=k; i++) {
			prod *= (double)(n-k+i)/i;
		}
		return prod;
	}
	
	public static void main(String[] args) {
		int n = 200;
		for(int k = 0; k<=n; k++) {
			System.out.println(n + " choose " + k + " is:" + FPComputer.nChooseK(n, k));
		}
	}


}
