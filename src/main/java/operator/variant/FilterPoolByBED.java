package operator.variant;

import java.io.IOException;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.BEDFile;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

import operator.IOOperator;
import operator.OperationFailedException;
import operator.Operator;
import pipeline.PipelineObject;

public class FilterPoolByBED extends Operator {

	VariantPool poolToFilter = null;
	VariantPool outputPool = null;
	BEDFile bedFile = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		try {
			outputPool = poolToFilter.filterByBED(bedFile);
			
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new OperationFailedException("IO Error filtering variant pool by BED file, " + e.getMessage(), this);
		}
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
