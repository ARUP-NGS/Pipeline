package operator.variant;

import java.io.IOException;

import operator.OperationFailedException;
import operator.annovar.Annotator;

import org.broad.tribble.readers.TabixReader;

import buffer.variant.VariantRec;


public class UK10KAnnotator_2 extends Annotator{


	/*
	 * 
	 * This annotator gets the Allele Frequency from the UK10K tabixed VCF file, and adds it as VariantRec.UK10K_ALLELE_FREQ annotation
	 * 
	 * VCF file obtained from : http://www.uk10k.org/data.html
	 * 
	 * @author brendan, edited by chrisk
	 *
	 */
	
		public static final String UK10K_PATH = "UK10K.path";
		private boolean initialized = false;
		private TabixReader reader = null;
		
	private void initializeReader() {
			String filePath = this.getAttribute(UK10K_PATH);
			if (filePath == null) {
				filePath = this.getPipelineProperty(UK10K_PATH);
			}
			if (filePath == null) {
				throw new IllegalArgumentException("Path to frequency data not specified, use " + UK10K_PATH);
			}
			try {
				reader = new TabixReader(filePath);
			} catch (IOException e) {
				throw new IllegalArgumentException("Error opening UK10K data at path " + filePath + " error : " + e.getMessage());
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
		
		String queryStr = contig + ":" + pos;
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
					throw new OperationFailedException("Error reading UK10K data file: " + e.getMessage(), this);
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
			//grabbing and breaking up the INFO field
			String[] infoField = toks[7].split(";");
			
			for(int i=0; i<infoField.length; i++) {
				String tok = infoField[i];
				if (tok.startsWith("AF=")) {
					tok = tok.replace("AF=", "");
					String[] vals = tok.split(",");
					try {
						
						Double af = Double.parseDouble(vals[vals.length-1]);
						var.addProperty(VariantRec.UK10K_ALLELE_FREQ, af);
						
					}
					catch(NumberFormatException ex) {
						//Don't worry about it, no annotation though
					}
				

				}
			}
			
			
			return true;
		}

}
