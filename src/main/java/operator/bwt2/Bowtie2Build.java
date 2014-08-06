package operator.bwt2;

import java.io.File;
import org.w3c.dom.NodeList;
import pipeline.Pipeline;

/**
 * For Bowtie2's "2" algorithm
 * Builds .bwt
 * 
 * @author daniel
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