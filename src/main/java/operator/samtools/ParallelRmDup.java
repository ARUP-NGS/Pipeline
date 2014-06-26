package operator.samtools;

import operator.CommandOperator;
import operator.OperationFailedException;
import buffer.BAMFile;

/**
 * Uses samtools-mt (multithreaded samtools) to remove duplicates super fast, otherwise
 * behaves just like samtools. 
 * @author brendan
 *
 */
public class ParallelRmDup extends CommandOperator {


	public static final String TREAT_PAIRS_AS_SINGLE = "treat.pairs.as.single";
	public static final String PATH = "path";
	public static final String FORCESINGLEEND = "force.single.end";

	protected boolean treatPairsAsSingle = false;

	
	@Override
	protected String getCommand() throws OperationFailedException {

		String samtoolsMTPath = this.getPipelineProperty("samtools.mt.path");
		
		String treat = this.getAttribute(TREAT_PAIRS_AS_SINGLE);
		if (treat != null) {
			treatPairsAsSingle = Boolean.parseBoolean(treat);
		}
		
		String single = "";
		if (treatPairsAsSingle) {
			single = " -S ";
		}
		
		BAMFile inputBAM = (BAMFile) getInputBufferForClass(BAMFile.class);
		BAMFile outputBAM = (BAMFile) getOutputBufferForClass(BAMFile.class);
		
		int threads = this.getPipelineOwner().getThreadCount();
		
		
		return samtoolsMTPath +
				" -n " + threads + 
				" rmdup " + single + " " +
				inputBAM.getAbsolutePath() + " " +
				outputBAM.getAbsolutePath();
	}
	

}
