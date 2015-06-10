package operator.variant;

import java.io.IOException;
import java.io.PrintStream;

import util.vcfParser.VCFParser.GTType;
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
			if (rec.getZygosity() == GTType.HOM) {
				hetStr = "hom";
			} else if (rec.getZygosity() == GTType.HEMI) {
				hetStr = "hemi";
			} else if (rec.getZygosity() == GTType.UNKNOWN) {
				hetStr = "unknown";
			}
			
			out.println(geneName + "\t" + pDot + "\t" + cDot + "\t" + hetStr + "\t" + depth + "\t" + quality + "\t" + hgmdHit);
		}
	}

	@Override
	public void writeFooter(PrintStream outputStream) throws IOException {
		// TODO Auto-generated method stub
		
	}

	
}
