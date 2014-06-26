package operator.test;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.NodeList;

/**
 * This operator just throws an error during performOperation. 
 * @author brendan
 *
 */
public class OperationFailOperator extends Operator {

	@Override
	public void performOperation() throws OperationFailedException {
		throw new OperationFailedException("This operation has failed.", this);
	}

	@Override
	public void initialize(NodeList children) {
		// TODO Auto-generated method stub
		
	}


}
