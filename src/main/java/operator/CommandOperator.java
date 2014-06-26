package operator;

import java.io.IOException;
import java.sql.Time;
import java.util.Date;
import java.util.logging.Logger;

import pipeline.Pipeline;
import util.ElapsedTimeFormatter;
import util.StringOutputStream;

/**
 * Base class for operators that invoke a system call, but do not emit their output to standard out. Unlike
 * PipedCommandOp, these operators don't need any fancy tricks to capture the output - it's just written to
 * a file by the command that we call (for instance, in GaTK the result is always written to a file). Hence
 * these operators do not need to capture the output stream, and just simply invoke a system call and wait for
 * it to complete. 
 * 
 * @author brendan
 *
 */
public abstract class CommandOperator extends IOOperator {
	
	//Stream used to capture output written to System.err
	protected StringOutputStream errStream = new StringOutputStream();

	/**
	 * Obtain a (single) command to be executed
	 * @return
	 * @throws OperationFailedException
	 */
	protected abstract String getCommand() throws OperationFailedException;

	/**
	 * Obtain a list of commands to be executed. They will be executed serially in the order given 
	 * @return
	 * @throws OperationFailedException
	 */
	protected String[] getCommands() throws OperationFailedException {
		return new String[]{ getCommand() };
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		String[] commands = getCommands();
		if (commands != null) {
			for(int i=0; i<commands.length; i++) {
				logger.info("[ " + (new Date()) + "] Operator: " + getObjectLabel() + " Executing command : " + commands[i] );
				executeCommand(commands[i]);
			}
		}
		else {
			logger.warning("Null command found for command operator " + getObjectLabel() + ", not executing any commands");
		}

	}

}
