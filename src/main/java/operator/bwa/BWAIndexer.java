package operator.bwa;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import operator.PipedCommandOp;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class BWAIndexer extends PipedCommandOp {

	public static final String PATH = "path";
	public static final String ALGORITHM = "algorithm";
	
	protected String defaultAlgorithm = "bwtsw"; //Other options are "is" and "div", both of which do not work for long genomes
	protected String algorithm = defaultAlgorithm;
	
	protected String filePathToIndex = null;
	
	protected String pathToBWA = "bwa";
	
	@Override
	public String getCommand() {
		
		Object propsPath = getPipelineProperty(PipelineXMLConstants.BWA_PATH);
		if (propsPath != null)
			pathToBWA = propsPath.toString();
		
		String bwaPathAttr = properties.get(PATH);
		if (bwaPathAttr != null) {
			pathToBWA = bwaPathAttr;
		}
		
		String algoAttr = properties.get(ALGORITHM);
		if (algoAttr != null) {
			algorithm = algoAttr;
		}
		
		filePathToIndex = inputBuffers.get(0).getAbsolutePath();
		
		String command = pathToBWA + " index -a " + algorithm + " " + filePathToIndex;
		return command;		
	}

	
}
