package operator.fqUtils;

import operator.CommandOperator;
import operator.OperationFailedException;
import buffer.FastQFile;

public class FastxTrimmer extends CommandOperator {
	public static final String TRIM_NUM = "trim.num";
    public static final String FASTX_TK = "fastx.tk";
	@Override
	protected final String getCommand() throws OperationFailedException {
		String inputFastq = getInputBufferForClass(FastQFile.class)
				.getAbsolutePath();
		String outputFastq = getOutputBufferForClass(FastQFile.class)
				.getAbsolutePath();

		String trim = properties.get(TRIM_NUM);
		String fastxTrim = getPipelineProperty(FASTX_TK);
		String command_str = fastxTrim + " -l " + trim + " -i " 
				+ inputFastq + " -o " + outputFastq;
		return command_str;
	}

}
