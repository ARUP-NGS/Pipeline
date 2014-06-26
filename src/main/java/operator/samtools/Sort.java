package operator.samtools;

import java.io.File;

import operator.CommandOperator;
import operator.OperationFailedException;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.FileBuffer;

/**
 * Use samtools to sort the given input file
 * @author brendan
 * Default sort option is coordinate. 
 * If query name is desired, set sort.type to "qname"
 */
public class Sort extends CommandOperator {

	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	public static final String SORT_TYPE = "sort.type";
	String defaultSort = "coordinate";
	@Override
	protected String getCommand() throws OperationFailedException {
		
		//Obtain path to the samtools executable from Pipeline class in static fashion
		Object samPropsPath = getPipelineProperty(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPropsPath != null) {
			samtoolsPath = samPropsPath.toString();
		}
		String sortType = defaultSort;
		String sortAttr = getAttribute("SORT_TYPE");
		if (sortAttr != null) {
			sortType = sortAttr;
		}
		String sortStr;
		if (sortType == "qname") {
			sortStr = "-n ";
		}
		else {
			sortStr = "";
		}
		//Now mostly obsolete, but if an alternative path is specified in this operator's 
		//properties, it supercede's the path obtained above. 
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
		
		//Take as the input file the first .bam file specified in the input files
		//list, all others will be ignored
		FileBuffer inputBuffer = getInputBufferForClass(BAMFile.class);
		
		//Output file is just the first .bam file given in output files list
		// ... but watch out for the confusing part below... we will need to change the name of the output file
		FileBuffer outputBuffer = getOutputBufferForClass(BAMFile.class);
		String path = outputBuffer.getAbsolutePath();
		path = path.replace(".bam", ""); //strip off trailing .bam from output file path 
		
		//Build command string
		String command = samtoolsPath + " sort " + sortStr + inputBuffer.getAbsolutePath() + " " + path;
		
		//CONFUSING! : The last argument to sort is just a file prefix, the new file will be that prefix + .bam. So we
		//need to make sure that the file with the correct name is associated with the outputbuffer...
		File newOutputFile = new File(path + ".bam");
		outputBuffer.setFile(newOutputFile);
		
		return command;
	}

}
