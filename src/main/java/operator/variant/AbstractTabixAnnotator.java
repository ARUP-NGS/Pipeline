package operator.variant;

import java.io.IOException;

import operator.OperationFailedException;
import operator.annovar.Annotator;

import org.broad.tribble.readers.TabixReader;

import util.vcfParser.VCFParser;
import buffer.variant.VariantRec;

/**
 * This is (well, should be) the base class for all annotators that read a Tabix-ed
 * vcf file to get their annotation info 
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
	 * we get from the VCF
	 * @param var
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
	
	/**
	 * Try to get the attribute associated with the given key from the XML attributes for this element.
	 * If it doesn't exist for this element, get it from PipelineProperties. Returns null
	 * if it cant be found anywhere. 
	 * @param attributeKey
	 * @return
	 */
	protected String searchForAttribute(String attributeKey) {
		String filePath = this.getAttribute(attributeKey);
		if (filePath == null) {
			filePath = this.getPipelineProperty(attributeKey);
		}
		return filePath;		
	}
	
	
	
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
			TabixReader.Iterator iter = reader.query(queryStr);

			if(iter != null) {
				try {
					String val = iter.next();
					
					while(val != null) {
						String[] toks = val.split("\t");
						if (toks.length > 6) {
							VariantRec queryResultVar = new VariantRec(toks[0], Integer.parseInt(toks[1]), Integer.parseInt(toks[1])+toks[3].length(), toks[3], toks[4]);
							queryResultVar = VCFParser.normalizeVariant(queryResultVar);
							
							//Make sure the (normalized) variant we get from the tabix query matches the
							//variant we want to annotate
							if (queryResultVar.getContig().equals(varToAnnotate.getContig())
									&& queryResultVar.getStart() == varToAnnotate.getStart()
									&& queryResultVar.getRef().equals(varToAnnotate.getRef())
									&& queryResultVar.getAlt().equals(varToAnnotate.getAlt())) { 
								boolean ok = addAnnotationsFromString(varToAnnotate, val);
								if (ok)
									break;
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
