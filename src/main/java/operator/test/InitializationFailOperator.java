package operator.test;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.NodeList;

/**
 * This operator just throws a NullPointerException during initialization.
 * @author brendan
 *
 */
public class InitializationFailOperator extends Operator {

	@Override
	public void performOperation() throws OperationFailedException {
		//Nothing to do here
	}

	@Override
	public void initialize(NodeList children) {
		
		throw new NullPointerException("Ahh! NullPointerException during initialization!");
	}


}
