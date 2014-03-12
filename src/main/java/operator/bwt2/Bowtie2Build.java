package operator.bwt2;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static java.lang.System.out;

import org.apache.tools.ant.types.CommandlineJava.SysProperties;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * For Bowtie2's "2" algorithm
 * Builds .bwt
 * 
 *
 *   
 * @author Daniel
 *
 */

public class Bowtie2Build extends operator.CommandOperator {

	public static final String PATH = "path";
	public static final String BOWTIE2_DIR = "bowtie2.dir";
	
	protected String filePathToIndex = "/mounts/genome/human_g1k_v37.fasta"; //Providing a default
	protected String pathToBowtie2 = "/mounts/bin/bowtie2-2.1.0/bowtie2-build"; //Providing a default
	
	
	@Override
	public String getCommand() {
		filePathToIndex = inputBuffers.get(0).getAbsolutePath();
		String command = pathToBowtie2 + " " + filePathToIndex + " " + filePathToIndex.substring(0, filePathToIndex.lastIndexOf('.'));
		//System.out.println(command + " OMG LULZ IT WORKED");
		return command;
	}
	
	@Override
	public void initialize(NodeList children) {
		super.initialize(children);
		
		String pathAttr = this.getAttribute(BOWTIE2_DIR);
		if (pathAttr == null) {
			pathAttr = this.getPipelineProperty(BOWTIE2_DIR);
		}
		if (pathAttr == null) {
			throw new IllegalArgumentException("No path to Bowtie2 found, please specify " + BOWTIE2_DIR);
		}
		if (! (new File(pathAttr).exists())) {
			throw new IllegalArgumentException("No file found at Bowtie2 path : " + pathAttr);
		}
		this.pathToBowtie2 = (pathAttr.concat("bowtie2-build"));
		}
	
}