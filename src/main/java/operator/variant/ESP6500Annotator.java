package operator.variant;

import java.io.IOException;

import operator.OperationFailedException;
import operator.annovar.Annotator;

import org.broad.tribble.readers.TabixReader;

import buffer.variant.VariantRec;

/**
 * This annotator computes the ESP 6500 exomes frequency, and adds it as the VariantRec.EXOMES_FREQ
 * annotation. The raw data is looked up in a VCF file that can be obtained from : http://evs.gs.washington.edu/EVS/
 * The VCF files (one for each chromosome) must be merged into a single VCF, and then tabix compressed and 
 * indexed.
 * @author brendan
 *
 */
public class ESP6500Annotator extends Annotator {

	public static final String ESP_PATH = "esp.path";
	private boolean initialized = false;
	private TabixReader reader = null;
	
	private void initializeReader() {
		String filePath = this.getAttribute(ESP_PATH);
		if (filePath == null) {
			filePath = this.getPipelineProperty(ESP_PATH);
		}
		
		if (filePath == null) {
			throw new IllegalArgumentException("Path to frequency data not specified, use " + ESP_PATH);
		}
		
		try {
			reader = new TabixReader(filePath);
		} catch (IOException e) {
			throw new IllegalArgumentException("Error opening ESP data at path " + filePath + " error : " + e.getMessage());
		}
		initialized = true;
	}
	
	public void annotateVariant(VariantRec var) throws OperationFailedException {
	if (! initialized) {
		initializeReader();
	}
	
	if (reader == null) {
		throw new OperationFailedException("Could not initialize tabix reader", this);
	}
	
	String contig = var.getContig();
	Integer pos = var.getStart();
	
	String queryStr = contig + ":" + pos + "-" + (pos);
	
	try {
		TabixReader.Iterator iter = reader.query(queryStr);

		if(iter != null) {
			try {
				String val = iter.next();
				while(val != null) {
					boolean ok = addAnnotationsFromString(var, val);
					if (ok)
						break;
					val = iter.next();
				}
			} catch (IOException e) {
				throw new OperationFailedException("Error reading ESP data file: " + e.getMessage(), this);
			}
		}
	}
	catch (RuntimeException rex) {
		//Bad contigs will cause an array out-of-bounds exception to be thrown by
		//the tabix reader. There's not much we can do about this since the methods
		//are private... right now we just ignore it and skip this variant
	}
}

	private boolean addAnnotationsFromString(VariantRec var, String val) {
		String[] toks = val.split("\t");
		String[] format = toks[7].split(";");
		
		Double totOverall = 0.0;
		Double homOverall = 0.0;
		
		for(int i=0; i<format.length; i++) {
			String tok = format[i];
			if (tok.startsWith("MAF=")) {
				tok = tok.replace("MAF=", "");
				String[] vals = tok.split(",");
				try {
					Double maf = Double.parseDouble(vals[vals.length-1]);
					Double mafEA = Double.parseDouble(vals[0]);
					Double mafAA = Double.parseDouble(vals[1]);
					var.addProperty(VariantRec.EXOMES_FREQ, maf);
					var.addProperty(VariantRec.EXOMES_FREQ_EA, mafEA);
					var.addProperty(VariantRec.EXOMES_FREQ_AA, mafAA);
				}
				catch(NumberFormatException ex) {
					//Don't worry about it, no annotation though
				}
			}
			if (tok.startsWith("EA_GTC=")) {
				tok = tok.replace("EA_GTC=", "");
				String[] vals = tok.split(",");
				try {
					Double homRef = Double.parseDouble(vals[vals.length-1]);
					Double het = Double.parseDouble(vals[1]);
					Double homAlt = Double.parseDouble(vals[0]);
					double tot = homRef + het + homAlt;
					var.addProperty(VariantRec.EXOMES_EA_HOMREF, homRef / tot);
					var.addProperty(VariantRec.EXOMES_EA_HET, het/tot);
					var.addProperty(VariantRec.EXOMES_EA_HOMALT, homAlt/ tot);
					totOverall += tot;
					homOverall += homAlt;
				}
				catch(NumberFormatException ex) {
					//Don't worry about it, no annotation though
				}
			}
			
			if (tok.startsWith("AA_GTC=")) {
				tok = tok.replace("AA_GTC=", "");
				String[] vals = tok.split(",");
				try {
					Double homRef = Double.parseDouble(vals[vals.length-1]);
					Double het = Double.parseDouble(vals[1]);
					Double homAlt = Double.parseDouble(vals[0]);
					double tot = homRef + het + homAlt;
					var.addProperty(VariantRec.EXOMES_AA_HOMREF, homRef / tot);
					var.addProperty(VariantRec.EXOMES_AA_HET, het/tot);
					var.addProperty(VariantRec.EXOMES_AA_HOMALT, homAlt/ tot);
					totOverall += tot;
					homOverall += homAlt;
				}
				catch(NumberFormatException ex) {
					//Don't worry about it, no annotation though
				}
			}
		}
		
		if (totOverall > 0) {
			var.addProperty(VariantRec.EXOMES_HOM_FREQ, homOverall / totOverall);
		} else {
			var.addProperty(VariantRec.EXOMES_HOM_FREQ, 0.0);	
		}
		
		return true;
	}

	
}
