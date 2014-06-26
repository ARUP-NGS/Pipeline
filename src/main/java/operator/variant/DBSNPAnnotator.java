package operator.variant;

import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;

import org.broad.tribble.readers.TabixReader;

import pipeline.Pipeline;
import buffer.variant.VariantRec;

/**
 * Uses dbSNP info downloaded directly from NCBI for annotations The raw data is obtained from
 * ftp://ftp.ncbi.nlm.nih.gov/snp/organisms/human_9606/VCF/common_all.vcf.gz
 *  and is tabix-indexed
 * @author brendan
 *
 */
public class DBSNPAnnotator extends Annotator {

	public static final String DBSNP_PATH = "dbsnp.path";
	private boolean initialized = false;
	private TabixReader reader = null;
	
	private void initializeReader() {
		String filePath = this.getAttribute(DBSNP_PATH);
		if (filePath == null) {
			filePath = this.getPipelineProperty(DBSNP_PATH);
		}
		
		if (filePath == null) {
			throw new IllegalArgumentException("Path to dbSNP data not specified, use " + DBSNP_PATH);
		}
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("Initializing dbSNP annotator using data file: " + filePath);
		
		try {
			reader = new TabixReader(filePath);
		} catch (IOException e) {
			throw new IllegalArgumentException("Error opening dbSNP data at path " + filePath + " error : " + e.getMessage());
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
			String rsNum = toks[2];
			var.addAnnotation(VariantRec.RSNUM, rsNum);
			return true;
		}
		return false;
	}

}
