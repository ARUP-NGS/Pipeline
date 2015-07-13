package operator.variant;

import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;

import org.broad.tribble.readers.TabixReader;

import pipeline.Pipeline;
import util.vcfParser.VCFParser;
import buffer.variant.VariantRec;
/**
 * This is (well, should be) the base class for all annotators that read a Tabix-ed
 * vcf file to get their annotation info. This handles several important functions such 
 * as creation of the TabixReader andnormalization of variants that are read in from the tabix.
 *
 *      support mulitple ALT alleles.
 * as creation of the TabixReader and normalization of variants that are read in from the tabix.
 * 
 * @author brendan
 *
 */
public abstract class AbstractTabixAnnotator extends Annotator {

	private boolean initialized = false;
	private TabixReader reader = null;

	/**
	 * Should return the path to the file we want to read
	 * @return
	 */
	protected abstract String getPathToTabixedFile();


	/**
	 * Subclasses should override this method to actually perform the annotations.
	 * The VariantRec is the variant to annotate, and the 'line' argument is the information
	 * we get from the VCF. Usually, annotators will extract some information from the line 
	 * (like allele frequency, dbSNP ids, etc. etc) and turn that into an annotation or property 
	 * for the VariantToAnnotate
	 * @param
	 * Subclasses should implement logic to handle the current altIndex given, as this changes from annotator to
	 * annotator. For now most annotators ignore the altIndex.
	 * @param var
	 * @param vcfLine
	 * @param altIndex
	 */

	protected abstract boolean addAnnotationsFromString(VariantRec variantToAnnotate, String vcfLine, int altIndex);

	protected void initializeReader(String filePath) {
		try {
			reader = new TabixReader(filePath);
		} catch (IOException e) {
			throw new IllegalArgumentException("Error opening data at path " + filePath + " error : " + e.getMessage());
		}
		initialized = true;
	}

	/**
	 * This overrides the 'prepare' method in the base Annotator class. It is always called 
	 * prior to the first call to annotateVariant, and gives us a chance to do a little
	 * initialization. 
	 */
	protected void prepare() {
		initializeReader( getPathToTabixedFile());
	}


	/**
	 *
	 * if the reference Alt is multiallelic it will throw error.  Databases should be normilized priot to use with the
	 * AbstractTabixAnnotator class
	 *
	 * @param referenceAlt
	 * @throws OperationFailedException
	 */
	public void checkVariant(String referenceAlt) throws OperationFailedException {
		if (referenceAlt.contains(",")) {
			throw new OperationFailedException("The database contains multiple ALT alleles on a single line.  It should be normalized prior to use.", this);
		}
	}

	/**
	 * Parses variants from the given VCF line (appropriately handling multiple alts) and compare each variant tot he
	 * 'varToAnnotate'. If a perfect match (including both chr, pos, ref, and alt) 
	 * @param varToAnnotate
	 * @param vcfLine
	 * @return
	 */
	protected int findMatchingVariant(VariantRec varToAnnotate, String vcfLine) {
		String[] toks = vcfLine.split("\t");
		String[] alts = toks[4].split(",");
		for(int i=0; i<alts.length; i++) {
			VariantRec queryResultVar = new VariantRec(toks[0], Integer.parseInt(toks[1]), Integer.parseInt(toks[1])+toks[3].length(), toks[3], alts[i]);
			queryResultVar = VCFParser.normalizeVariant(queryResultVar);

			if (queryResultVar.getContig().equals(varToAnnotate.getContig())
					&& queryResultVar.getStart() == varToAnnotate.getStart()
					&& queryResultVar.getRef().equals(varToAnnotate.getRef())
					&& queryResultVar.getAlt().equals(varToAnnotate.getAlt())) { //change to loop through all alts

				//Everything looks good, so go ahead and annotate		
				boolean ok = addAnnotationsFromString(varToAnnotate, vcfLine, i);
				if (ok) {
					return i;
				}
			} //if perfect variant match

		}//Loop over alts	
		return -1;
	}
	
	/**
	 * This actually annotates the variant - it performs new tabix query, then converts the
	 * result to a normalized VariantRec, then sees if the normalized VariantRec matches the
	 * variant we want to annotate. If so 
	 */
	@Override
	public void annotateVariant(VariantRec varToAnnotate) throws OperationFailedException {

		if (! initialized) {
			throw new OperationFailedException("Failed to initialize", this);
		}

		if (reader == null) {
			throw new OperationFailedException("Tabix reader not initialized", this);
		}

		String contig = varToAnnotate.getContig();
		Integer pos = varToAnnotate.getStart();


		String queryStr = contig + ":" + (pos-10) + "-" + (pos+10);

		//Perform the lookup
		
		TabixReader.Iterator iter = null;
		
		try {
			iter = reader.query(queryStr);
		} catch (RuntimeException ex) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Exception during tabix reading for query: " + queryStr + " : " + ex.getLocalizedMessage());
		}

		if(iter != null) {
			try {
				String val = iter.next();
				while(val != null) {
					String[] toks = val.split("\t");
					if (toks.length > 6) {

						int altIndex = findMatchingVariant(varToAnnotate, val);
						
						if (altIndex >= 0) {
							break; //break out of searching over tabix results
						}
						else {
							val = iter.next(); 
						}
					}//If there are enough  tokens in this VCF line 

				}//If iter returned non-null value
			} catch (IOException ex) {
				Logger.getLogger(Pipeline.primaryLoggerName).warning("Tabix iterator exception: " + ex.getLocalizedMessage());
			}
		}
	}
	

}
