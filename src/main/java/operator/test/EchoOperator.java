package operator.test;

import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;

/**
 * A debugging operator that just emits a message to system.out and the primary logger
 * @author brendan
 *
 */
public class EchoOperator extends Operator {

	String message = "";
	
	@Override
	public void performOperation() throws OperationFailedException {
		System.out.println(message);
		Logger.getLogger(Pipeline.primaryLoggerName).info("Echo operator says : " + message);
	}

	
	
	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.TEXT_NODE) {
				message = child.getNodeValue();
			}
		}
	}

}
