package operator.variant;

import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;

import org.broad.tribble.readers.TabixReader;

import pipeline.Pipeline;
import buffer.variant.VariantRec;

/**
 * Provides several 1000-Genomes based annotations, using the new 11/23/2010, version 3 calls
 * In contrast to previous 1000 Genomes annotator which parsed Annovar output, this uses 
 * a tabix-indexed "sites" file usually downloaded directly from : 
 * ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/release/20110521/ALL.wgs.phase1_release_v3.20101123.snps_indels_sv.sites.vcf.gz
 * 
 * to produce the annotations.
 * 
 * @author brendan
 *
 */
public class TGPTabixAnnotator extends Annotator {

	public static final String TGP_SITES_PATH = "tgp.sites.path";
	private boolean initialized = false;
	private TabixReader reader = null;
	
	private void initializeReader() {
		String filePath = this.getAttribute(TGP_SITES_PATH);
		if (filePath == null) {
			filePath = this.getPipelineProperty(TGP_SITES_PATH);
		}
		
		if (filePath == null) {
			throw new IllegalArgumentException("Path to 1000 Genomes frequency data not specified, use " + TGP_SITES_PATH);
		}
		
		try {
			reader = new TabixReader(filePath);
		} catch (IOException e) {
			throw new IllegalArgumentException("Error opening TGP data at path " + filePath + " error : " + e.getMessage());
		}
		initialized = true;
	}
	
	@Override
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
					throw new OperationFailedException("Error reading TGP data file: " + e.getMessage(), this);
				}
			}
		}
		catch (RuntimeException rex) {
			//Bad contigs will cause an array out-of-bounds exception to be thrown by
			//the tabix reader. There's not much we can do about this since the methods
			//are private... right now we just ignore it and skip this variant
		}
	}

	/**
	 * Checks to see if the string provided matches the contig, position, and alt of the variants given. 
	 * If so, parses the frequency information from the string and annotates the variant.
	 * Returns false if no match, true if annotations successfully added 
	 * @param var
	 * @param str
	 * @throws OperationFailedException
	 */
	private boolean addAnnotationsFromString(VariantRec var, String str) throws OperationFailedException {
		String[] toks = str.split("\t");
		if (! toks[0].equals(var.getContig())) {
			//We expect that sometimes we'll not get the right contig
			return false;
		}
		if (! toks[1].equals("" + var.getStart())) {
			//We expect that sometimes we'll not get the right position (not sure why exactly... tabix doesn't work perfectly I guess			return;
		}
		if ( (!toks[3].equals(var.getRef())) && toks[2].length()==1 && var.getRef().length() ==1) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Ref alleles don't match in TGP annotator");
			return false;
		}
		if (! toks[4].equals(var.getAlt())) {
			//Not an error, this may happen sometimes
			return false;
		}
		
		String[] formatToks = toks[7].split(";");
		String overallFreqStr = valueForKey(formatToks, "AF");
		if (overallFreqStr != null) {
			Double freq = Double.parseDouble(overallFreqStr);
			var.addProperty(VariantRec.POP_FREQUENCY, freq);
		}
		
		
		String freqStr = valueForKey(formatToks, "AMR_AF");
		if (freqStr != null) {
			Double freq = Double.parseDouble(freqStr);
			var.addProperty(VariantRec.AMR_FREQUENCY, freq);
		}
		
		String afrFreqStr = valueForKey(formatToks, "AFR_AF");
		if (afrFreqStr != null) {
			Double freq = Double.parseDouble(afrFreqStr);
			var.addProperty(VariantRec.AFR_FREQUENCY, freq);
		}
		
		String eurFreqStr = valueForKey(formatToks, "EUR_AF");
		if (eurFreqStr != null) {
			Double freq = Double.parseDouble(eurFreqStr);
			var.addProperty(VariantRec.EUR_FREQUENCY, freq);
		}
		return true;
	}

	private static String valueForKey(String[] toks, String key) {
		for(int i=0; i<toks.length; i++) {
			if (toks[i].startsWith(key)) {
				return toks[i].replace(key, "").replace("=", "").replace(";", "").trim();
			}
		}
		return null;
	}
}
