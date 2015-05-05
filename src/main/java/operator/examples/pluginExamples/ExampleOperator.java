package operator.examples.pluginExamples;

import java.io.IOException;

import json.JSONException;
import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.NodeList;

public class ExampleOperator extends Operator {

	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		System.out.println("My fancy operator is performing!");
	}

	@Override
	public void initialize(NodeList children) {
		System.out.println("My fancy operator is initializing");
	}
}
