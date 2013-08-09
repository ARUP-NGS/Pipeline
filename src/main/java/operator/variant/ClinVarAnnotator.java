package operator.variant;

import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;

import org.broad.tribble.readers.TabixReader;

import pipeline.Pipeline;
import buffer.variant.VariantRec;

/**
 * Adds annotations from a dbSNP clinvar file. 
 * @author brendan
 *
 */
public class ClinVarAnnotator extends Annotator {

	public static final String DBSNP_PATH = "dbsnp.clinvar.path";
	private boolean initialized = false;
	private TabixReader reader = null;
	
	private void initializeReader() {
		String filePath = this.getAttribute(DBSNP_PATH);
		if (filePath == null) {
			filePath = this.getPipelineProperty(DBSNP_PATH);
		}
		
		if (filePath == null) {
			throw new IllegalArgumentException("Path to dbSNP clinvar data not specified, use " + DBSNP_PATH);
		}
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("Initializing dbSNP CLINVAR annotator using data file: " + filePath);
		
		try {
			reader = new TabixReader(filePath);
		} catch (IOException e) {
			throw new IllegalArgumentException("Error opening dbSNP ClinVar data at path " + filePath + " error : " + e.getMessage());
		}
		initialized = true;
	}
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		if (reader == null) {
			initializeReader();
		}
		
		if (reader == null) {
			throw new OperationFailedException("Could not initialize tabix reader", this);
		}
		
		String contig = var.getContig();
		Integer pos = var.getStart();
		
		String queryStr = contig + ":" + pos + "-" + (pos);
		//int count = 0;
		try {
			TabixReader.Iterator iter = reader.query(queryStr);

			if(iter != null) {
				try {
					String val = iter.next();
					while(val != null) {
						boolean ok = addAnnotationsFromString(var, val);
						if (ok) {
							break;
						}
						val = iter.next();
					}
				} catch (IOException e) {
					throw new OperationFailedException("Error reading dbSNP data file: " + e.getMessage(), this);
				}
			}
		}
		catch (RuntimeException rex) {
			//Bad contigs will cause an array out-of-bounds exception to be thrown by
			//the tabix reader. There's not much we can do about this since the methods
			//are private... right now we just ignore it and skip this variant
		}

	}
	
	
	private boolean addAnnotationsFromString(VariantRec var, String str) throws OperationFailedException {
		String[] toks = str.split("\t");
		if (! toks[0].equals(var.getContig())) {
			//We expect that sometimes we'll not get the right contig
			return false;
		}
		if (! toks[1].equals("" + var.getStart())) {
			//We expect that sometimes we'll not get the right position (not sure why exactly... tabix doesn't work perfectly I guess			return;
		}
		
		if (toks[4].equals(var.getAlt())) {
			//Found a match!
			String[] infos = toks[7].split(";");
			
			boolean validated = false;
			for(int i=0; i<infos.length; i++) {
				String inf = infos[i].trim();
				if (inf.contains("VLD") || inf.equals("KGValidated")) {
					validated = true;
				}
				
				if (inf.contains("CLNSIG")) {
					try {
						String valStr = inf.replace("CLNSIG=", "");
						Integer val = Integer.parseInt(valStr);
						String sigStr = getClinSigStr(val);
						var.addAnnotation(VariantRec.CLINVAR_TYPE, sigStr);
					}
					catch (NumberFormatException nfe) {
						//don't stress it
					}
				}
			}
			
			if (validated) {
				var.addAnnotation(VariantRec.CLINVAR_VALIDATED, "yes");
			} 
			else {
				var.addAnnotation(VariantRec.CLINVAR_VALIDATED, "no");	
			}
			
			
		}
		
		return false;
	}
	
	/**
	 * Translate CLNSIG field into user-readable annotation. Mapping taken from clinvar header
	 * @param val
	 * @return
	 */
	private static String getClinSigStr(int val) {
		if (val == 0) {
			return "Unknown";
		}
		if (val == 1) {
			return "Untested";
		}
		if (val == 2) {
			return "Not pathogenic";
		}
		if (val == 3) {
			return "Probably not pathogenic";
		}
		if (val == 4) {
			return "Probably pathogenic";
		}
		if (val == 5) {
			return "Pathogenic";
		}
		if (val == 6) {
			return "Drug-response";
		}
		if (val == 7) {
			return "Histocompatibility";
		}
		
		return "Other";
	}

}
