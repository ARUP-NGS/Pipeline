package operator.variant;

import java.io.PrintStream;

import buffer.variant.VariantRec;

public class WeirdWriter extends VariantPoolWriter {

	@Override
	public void writeHeader(PrintStream outputStream) {
		//No header, I guess
	}

	@Override
	public void writeVariant(VariantRec rec, PrintStream out) {
		String rsNum = rec.getAnnotation(VariantRec.RSNUM);
		if (rsNum == null)
			rsNum = "-";
		String validationStr = "no validation";
		if (rsNum.length() > 2) {
			validationStr = "Public database: dbSNP132";
		}
		Double tkgFreq = rec.getProperty(VariantRec.POP_FREQUENCY);
		if (tkgFreq != null && tkgFreq > 0)
			validationStr = "Public database : 1000 Genomes";
		else {
			Double espFreq = rec.getProperty(VariantRec.EXOMES_FREQ);
			if (espFreq != null && espFreq > 0)
				validationStr = "Public database : ESP5400 ";
		}
		out.println(rec.getContig() + "\t" + rec.getStart() + "\t" + rsNum + "\t" + rec.getRef() + "\t" + rec.getAlt() + "\t" + validationStr);
	}

}
