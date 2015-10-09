package operator;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.BAMFile;
import buffer.BEDFile;

/**
 * Just creates a symbolic link from one dir to the BAM file in a results directory.
 * This is most often used to create a link from somewhere that a web server is looking
 * at, such as /var/www/html/ to a bam file so the bam can be loaded into IGV remotely. 
 * @author brendan
 *
 */
public class LinkCreator extends Operator {
	
	public static final String SAMPLE = "sample";
	public static final String WEBROOT = "web.root";
	public static final String RESULT_DIR = "result.dir";

	private String webRoot = "/var/www/html/";
	private String resultDir = "results/";
	private BAMFile finalBam = null;
	private BEDFile capture = null;
	private String sampleID = null;
	private String linkName = null;

	@Override
	public void performOperation() throws OperationFailedException {
		linkName = finalBam.getFilename().replace(".bam", "") + "-" + finalBam.getUniqueTag() + ".bam"; 
		String linkTarget = finalBam.getAbsolutePath();

		
		
		createLink(linkTarget, webRoot +  resultDir + linkName );
		createLink(linkTarget + ".bai", webRoot +  resultDir + linkName + ".bai" );
		
		if (capture != null) {
			String captureLinkName = capture.getFilename().replace(".bed", "") + "-" + capture.getUniqueTag() + ".bed";
			String captureLinkTarget = capture.getAbsolutePath();
			createLink(captureLinkTarget, webRoot +  resultDir + captureLinkName );
		}
	}
	
	/**
	 * Returns the name of the link created, relative to the 'web root'
	 * For instance, if the full symbolic link is /var/www/html/results/file.bam, this
	 * function will return results/file.bam . Use getWebRoot to obtain the web root portion 
	 * @return
	 */
	public String getLinkName() {
		return linkName;
	}
	
	public String getWebRoot() {
		return webRoot;
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
		
		//allow webroot to be in proberties
		webRoot = this.getAttribute(WEBROOT);
		if (webRoot == null) {
			webRoot = this.getPipelineProperty(WEBROOT);
		}

		if (webRoot == null) {
			throw new IllegalArgumentException("web root file is not set");
		}

		//File testFile = new File(webRoot);
		if ( !(new File(webRoot)).isDirectory() ) {
			throw new IllegalArgumentException("web root file " + webRoot + " is not valid direcory");
		}

//		if (this.getAttribute(WEBROOT) != null && this.getAttribute(WEBROOT).length()>1) {
//			webRoot = this.getAttribute(WEBROOT);
//			//test to see if this exists
//			File testFile = new File(webRoot);
//			if (!testFile.exists()) {
//				throw new IllegalArgumentException("web root file " + testFile.getAbsolutePath() + " does not exist");
//			}
//		}


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
				
				if (obj instanceof BEDFile) {
					capture = (BEDFile)obj;
					continue;
				}
				
			}
		}
		

		
	}

}
