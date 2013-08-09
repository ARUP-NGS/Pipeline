package operator.test;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.NodeList;

public class WaitOperator extends Operator {

	public static final String SECONDS="seconds";
	int secondsToWait = 10;
	
	
	@Override
	public void performOperation() throws OperationFailedException {
		try {
			System.out.println("Operator " + getObjectLabel() + " is waiting for " + secondsToWait + " seconds...");
			Thread.sleep(secondsToWait * 1000); 
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("..done!");
	}

	@Override
	public void initialize(NodeList children) {
		
		//See if there is a 'seconds' attribute supplied to this operator
		String secondsAttr = this.getAttribute(SECONDS);
		
		//If it's not null try to parse an integer value from it. Be sure
		//to catch number-parsing errors
		if (secondsAttr != null) {
			try {
				int seconds = Integer.parseInt(secondsAttr);
				this.secondsToWait = seconds;
				System.out.println("This operator will wait for " + secondsToWait + " seconds");
			}
			catch (NumberFormatException nfe) {
				System.err.println("ERROR: Could not read an integer from : " + secondsAttr);
			}
		}
	}

}
