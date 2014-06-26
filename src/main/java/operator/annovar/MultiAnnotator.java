package operator.annovar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.AnnovarInputFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineObject;
import util.VCFLineParser;

/**
 * An operator that can flexibly add multiple types of annotations to a variant pool
 * @author brendan
 *
 */
public class MultiAnnotator extends IOOperator {

	protected VCFFile vcfFile = null;
	protected VariantPool variants = null;
	protected AnnovarInputFile annovarInputFile = null;
	protected List<Annotator> annotators = new ArrayList<Annotator>();
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Beginning multi-annotation operator " + getObjectLabel());
		if (vcfFile != null && variants == null) {
			variants = new VariantPool();
			try {
				VCFLineParser vParser = new VCFLineParser(vcfFile);
				while( vParser.advanceLine()) {
					VariantRec rec = vParser.toVariantRec();
					if (rec != null)
						variants.addRecord(rec);
					logger.info("Multi-annotation operator created variant pool with " + getObjectLabel());

				}
			} catch (IOException e) {
				throw new OperationFailedException("Could not open vcf file for reading : " + e.getMessage(), this);
			}
			
		}
		
		for(Annotator annotator : annotators) {
			
		}
		
		

		logger.info("Completed multi-annotation operator " + getObjectLabel());
	}

	public void initialize(NodeList children) {	
		Element inputList = getChildForLabel("input", children);
		Element outputList = getChildForLabel("output", children);
		
		if (inputList != null) {
			NodeList inputChildren = inputList.getChildNodes();
			for(int i=0; i<inputChildren.getLength(); i++) {
				Node iChild = inputChildren.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj instanceof Annotator) {
						annotators.add( (Annotator)obj );
					}
					else {
						if (obj instanceof VCFFile) {
							vcfFile = (VCFFile)vcfFile;
						}
						if (obj instanceof VariantPool) {
							variants = (VariantPool)obj;
						}
					}
				}
			}
		}
		
		if (outputList != null) {
			NodeList outputChilden = outputList.getChildNodes();
			for(int i=0; i<outputChilden.getLength(); i++) {
				Node iChild = outputChilden.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj instanceof VariantPool) {
						VariantPool outputVariants = (VariantPool)obj;
						if (variants != null && outputVariants != variants) {
							throw new IllegalArgumentException("When input is a variant pool (not a vcf file), it's an error to specify a different variant pool as output");
						}
					}
					else {
						throw new IllegalArgumentException("Found non-variant pool object in output list for Operator " + getObjectLabel());
					}
				}
			}
		}
		
		if ( requiresReference() ) {
			ReferenceFile ref = (ReferenceFile) getInputBufferForClass(ReferenceFile.class);
			if (ref == null) {
				throw new IllegalArgumentException("Operator " + getObjectLabel() + " requires reference file, but none were found");
			}
		}
	}
	
}
