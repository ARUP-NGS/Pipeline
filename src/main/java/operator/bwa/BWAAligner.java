package operator.bwa;

import operator.PipedCommandOp;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class BWAAligner extends PipedCommandOp {
	
	public static final String PATH = "path";
	public static final String THREADS = "threads";
	protected String pathToBWA = "bwa";
	protected int defaultThreads = 4;
	protected int threads = defaultThreads;

	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	protected String getCommand() {
		
		Object propsPath = getPipelineProperty(PipelineXMLConstants.BWA_PATH);
		if (propsPath != null)
			pathToBWA = propsPath.toString();
		
		String bwaPathAttr = properties.get(PATH);
		if (bwaPathAttr != null) {
			pathToBWA = bwaPathAttr;
		}
		
		String threadsAttr = properties.get(THREADS);
		if (threadsAttr != null) {
			threads = Integer.parseInt(threadsAttr);
		}
			
		String referencePath = inputBuffers.get(0).getAbsolutePath();
		String readsPath = inputBuffers.get(1).getAbsolutePath();

		String command = pathToBWA + " aln -t " + threads + " " + referencePath + " " + readsPath;
		return command;
	}

}
