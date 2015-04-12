package operator.variant;

import java.io.IOException;

import operator.OperationFailedException;
import operator.annovar.Annotator;

import org.broad.tribble.readers.TabixReader;

import util.vcfParser.VCFParser;
import buffer.variant.VariantRec;
import java.util.Arrays;
/**
 * This is (well, should be) the base class for all annotators that read a Tabix-ed
 * vcf file to get their annotation info. This handles several important functions such 
 * as creation of the TabixReader andnormalization of variants that are read in from the tabix.
 *
 *      support mulitple ALT alleles.
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
	 * @param vcfLine
	 */
	protected abstract boolean addAnnotationsFromString(VariantRec variantToAnnotate, String vcfLine);

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



	public void check_variant(String referenceAlt) throws OperationFailedException {
		if (referenceAlt.contains(",")) {
			throw new OperationFailedException(
					"The database contains mulitple ALT alleles on a single line.  Use a vcfTidy'd version of the DB",
					this);
		}
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

		try {
			//Perform the lookup
			TabixReader.Iterator iter = reader.query(queryStr);

			if(iter != null) {
				try {
					String val = iter.next();

					while(val != null) {
						String[] toks = val.split("\t");
						if (toks.length > 6) {
							//Convert the result (which is a line of a VCF file) into a variant rec
							VariantRec queryResultVar = new VariantRec(toks[0], Integer.parseInt(toks[1]), Integer.parseInt(toks[1])+toks[3].length(), toks[3], toks[4]);
							//Important: Normalize the record so that it will match the 
							//variants in the variant pool that we want to annotate
							queryResultVar = VCFParser.normalizeVariant(queryResultVar);
							//System.out.println("XX" + varToAnnotate.toString());

							//Added this to throw an error if the DB has multiple Alt on a single line
							check_variant(queryResultVar.getAlt());

							//variant we want to annotate
							for (int i = 0; i < varToAnnotate.getAllAlts().length; i++) {
								if (queryResultVar.getContig().equals(varToAnnotate.getContig())
										&& queryResultVar.getStart() == varToAnnotate.getStart()
										&& queryResultVar.getRef().equals(varToAnnotate.getRef())
										&& queryResultVar.getAlt().equals(varToAnnotate.getAllAlts()[i])) { //change to loop through all alts
									//Everything looks good, so go ahead and annotate
									boolean ok = addAnnotationsFromString(varToAnnotate, val);
									if (ok)
										break;
								}

							}
						}
						val = iter.next();
					}
				} catch (IOException e) {
					throw new OperationFailedException("Error reading data file: " + e.getMessage(), this);
				}
			}
		}

		catch (RuntimeException rex) {
			//Bad contigs will cause an array out-of-bounds exception to be thrown by
			//the tabix reader. There's not much we can do about this since the methods
			//are private... right now we just ignore it and skip this variant
		}
	}



}
