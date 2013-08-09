package operator.variant;

import java.io.PrintStream;

import buffer.variant.VariantRec;

public class EmitHGMD extends VariantPoolWriter {

	@Override
	public void writeHeader(PrintStream outputStream) {
		
	}

	@Override
	public void writeVariant(VariantRec rec, PrintStream out) {
		String hgmdHit = rec.getAnnotation(VariantRec.HGMD_HIT);
		if (hgmdHit != null) {
			String geneName = rec.getAnnotation(VariantRec.GENE_NAME);
			String cDot = rec.getAnnotation(VariantRec.CDOT);
			String pDot = rec.getAnnotation(VariantRec.PDOT);
			String depth = rec.getPropertyOrAnnotation(VariantRec.DEPTH);
			Double quality = rec.getQuality();
			
			String hetStr = "het";
			if (! rec.isHetero())
				hetStr = "hom";
			
			out.println(geneName + "\t" + pDot + "\t" + cDot + "\t" + hetStr + "\t" + depth + "\t" + quality + "\t" + hgmdHit);
		}
	}

	
}
