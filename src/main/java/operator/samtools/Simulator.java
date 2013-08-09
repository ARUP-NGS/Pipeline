package operator.samtools;

import operator.PipedCommandOp;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class Simulator extends PipedCommandOp {

	public static final String PATH = "path";
	public static final String READ_PAIRS = "readpairs";
	public static final String MUTATION_RATE = "mutation.rate";
	public static final String INDEL_RATE = "indel.rate";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	protected int readPairs = 100000;
	protected double mutationRate = 0.001; //Same as wgsim default
	protected double indelRate = 0.15; //Also same as wgsim default
	
	
	@Override
	protected String getCommand() {
	
		Object samPropPath = getPipelineProperty(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPropPath != null)
			samtoolsPath = samPropPath.toString();
		
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
		
		String rpStr = properties.get(READ_PAIRS);
		if (rpStr != null) {
			readPairs = Integer.parseInt(rpStr);
		}
		
		String mutStr = properties.get(MUTATION_RATE);
		if (mutStr != null) {
			mutationRate = Double.parseDouble(mutStr);
		}
		
		String indelRateStr = properties.get(INDEL_RATE);
		if (indelRateStr != null)
			indelRate = Double.parseDouble(indelRateStr);
		
		String inputPath = inputBuffers.get(0).getAbsolutePath();
		
		String mutatedSites = outputBuffers.get(0).getAbsolutePath(); 
		String outputReads1 = outputBuffers.get(1).getAbsolutePath();
		String outputReads2 = outputBuffers.get(2).getAbsolutePath();
		
		if (samtoolsPath.endsWith("samtools")) {
			samtoolsPath = samtoolsPath.substring(0, samtoolsPath.length()-8);
			samtoolsPath = samtoolsPath + "/misc/";
		}
		
		//Ouput handled automagically!
		String command = samtoolsPath + "wgsim -r " + mutationRate + " -R " + indelRate + " -N " + readPairs + " " + inputPath + " " + outputReads1 + " " + outputReads2;
		return command;
	}

}
