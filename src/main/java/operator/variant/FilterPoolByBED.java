package operator.variant;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.BEDFile;
import buffer.variant.VariantPool;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineObject;

public class FilterPoolByBED extends IOOperator {

	VariantPool poolToFilter = null;
	public VariantPool outputPool = null;
	BEDFile bedFile = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		try {
			poolToFilter.filterByBED(bedFile, outputPool);
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new OperationFailedException("IO Error filtering variant pool by BED file, " + e.getMessage(), this);
		}
		
		logger.info("Filtered variant pool has " + outputPool.getContigCount() + " contigs and " + outputPool.size() + " total variants");
	}

	@Override
	public void initialize(NodeList children) {
		Element inputList = getChildForLabel("input", children);
		Element outputList = getChildForLabel("output", children);
		
		NodeList inputChildren = inputList.getChildNodes();
		for(int i=0; i<inputChildren.getLength(); i++) {	
			Node iChild = inputChildren.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof VariantPool) {
					poolToFilter = (VariantPool)obj;
				}
				if (obj instanceof BEDFile) {
					bedFile = (BEDFile)obj;
				}
			}
		}
		
		NodeList outputChildren = outputList.getChildNodes();
		for(int i=0; i<outputChildren.getLength(); i++) {	
			Node iChild = outputChildren.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof VariantPool) {
					outputPool = (VariantPool)obj;
				}
		
			}
		}
		
		
	}

}
