package operator.examples;

import operator.CommandOperator;
import operator.OperationFailedException;
import buffer.BAMFile;

/**
 * Shows general use of the CommandOperator, which executes shell commands
 * @author brendan
 *
 */
public class ExampleCommandOp extends CommandOperator {

	@Override
	/**
	 * Basic idea: execute a shell command. We can easily call external tools this way
	 */
	protected String getCommand() throws OperationFailedException {

		//Get the first BAM file provided in the XML
		BAMFile inputBAM = (BAMFile) super.getInputBufferForClass(BAMFile.class);
		
		if (inputBAM == null) {
			throw new OperationFailedException("No BAM file provided", this);
		}
		
		String pathOfBam = inputBAM.getAbsolutePath();
		
		return "samtools index " + pathOfBam;
	}

}
