package operator;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.BAMFile;

/**
 * Just creates a symbolic link from one dir to the BAM file in a results directory
 * @author brendan
 *
 */
public class LinkCreator extends Operator {
	
	public static final String SAMPLE = "sample";
	public static final String WEBROOT = "web.root";
	public static final String RESULT_DIR = "result.dir";

	private String webRoot = "/var/www/html/";
	private String resultDir = "bamlinks/";
	private BAMFile finalBam = null;
	private String sampleID = null;
	

	@Override
	public void performOperation() throws OperationFailedException {
		String linkName = sampleID + ("" + System.currentTimeMillis()).substring(6) + ".bam"; 
		String linkTarget = finalBam.getAbsolutePath();

		createLink(linkTarget, webRoot +  resultDir + linkName );
		createLink(linkTarget + ".bai", webRoot +  resultDir + linkName + ".bai" );
	}
	
	/**
	 * Create and execute a process that makes a symbolic link to the target from the linkName
	 * @param target
	 * @param linkName
	 */
	private void createLink(String target, String linkName) {
		Logger.getLogger(Pipeline.primaryLoggerName).info("Creating symbolic link from " + linkName + " to " + target);
		ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "ln -s " + target + " " + linkName);
		processBuilder.redirectErrorStream(true);
		
		try {
			Process proc = processBuilder.start();
			
			int exitVal = proc.waitFor();
			
			if (exitVal != 0) {
				Logger.getLogger(Pipeline.primaryLoggerName).warning("Nonzero exit value for link-creation process, could not create link: "  + linkName);
			}
		} catch (IOException e) {
			
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void initialize(NodeList inputChildren) {
		sampleID = this.getAttribute(SAMPLE);
		if (sampleID == null) {
			throw new IllegalArgumentException("No sample name given to LinkCreator");
		}
		
		
		if (this.getAttribute(WEBROOT) != null && this.getAttribute(WEBROOT).length()>1) {
			webRoot = this.getAttribute(WEBROOT);
			//test to see if this exists
			File testFile = new File(webRoot);
			if (!testFile.exists()) {
				throw new IllegalArgumentException("web root file " + testFile.getAbsolutePath() + " does not exist");
			}
		}
		if (this.getAttribute(RESULT_DIR) != null && this.getAttribute(RESULT_DIR).length()>1) {
			resultDir = this.getAttribute(RESULT_DIR);
			File testFile = new File(webRoot + "/" + resultDir);
			if (!testFile.exists()) {
				throw new IllegalArgumentException("web root results file " + testFile.getAbsolutePath() + " does not exist");
			}
		}
		
		
		for(int i=0; i<inputChildren.getLength(); i++) {
			Node iChild = inputChildren.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				
				
				if (obj instanceof BAMFile) {
					finalBam = (BAMFile)obj;
					continue;
				}
				
			}
		}
		

		
	}

}
