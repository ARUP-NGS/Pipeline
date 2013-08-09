package operator;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import operator.qc.QCReport;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import pipeline.WranglerStatusWriter;
import buffer.BAMFile;
import buffer.CSVFile;
import buffer.VCFFile;

/**
 * This class writes information to a StatusLogger that summarizes the final results
 * of a run. Most importantly, it makes symbolic links to some of the results files 
 * (bams, vcfs, etc.) so that they can be accessed by an http server
 * @author brendan
 *
 */
public class StatusFinalizer extends Operator {

	public static final String CREATE_LINKS = "create.links";
	public static final String SAMPLE = "sample";
	public static final String WEBROOT = "web.root";
	public static final String RESULT_DIR = "result.dir";
	public static final String SERVER_URL = "server.url";
	
	private String webRoot = "/usr/local/apache2/htdocs/";
	private String resultDir = "results/";
	private String serverURL = "http://10.90.68.168/";
	
	StatusLogger statusLogger = null;
	QCReport qcMetrics = null;
	VCFFile vcfFile = null;
	BAMFile finalBam = null;
	CSVFile annotatedVars = null;
	private boolean createLinks = true;
	private String sampleID = null;
	
	public String getSampleID() {
		return sampleID;
	}
	
	public String getQCLink() {
		String linkDir = getSampleID() + "-QC";
		return resultDir + linkDir + "/qc-report/qc-metrics.html";
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		String createLinksAttr = this.getAttribute(CREATE_LINKS);
		if (createLinksAttr != null && createLinksAttr.length()>0) {
			createLinks = Boolean.parseBoolean(createLinksAttr);
		}
		
		
		WranglerStatusWriter writer = statusLogger.getWriter();
		if (writer == null)
			throw new OperationFailedException("WrangerStatusWriter not initialized, apparently", this);
		
		if (qcMetrics != null) {
			
			String linkName = "quality-metrics-" +sampleID + ".html";
			String linkDir = sampleID + "-QC"; //Can't have trailing slash!
			String linkTarget = qcMetrics.getOutputDir().getAbsolutePath() + "/qc-metrics.html";
			writer.addMessage("QC report",  serverURL + getQCLink());
			
			if (createLinks) {
				createLink(qcMetrics.getOutputDir().getAbsolutePath(), webRoot + resultDir + linkDir);
				createLink(linkTarget, webRoot + resultDir + linkDir + "/" + linkName );
			}
		}
		
		if (vcfFile != null) {

			String linkName = sampleID + ".final.vcf"; 
			String linkTarget = vcfFile.getAbsolutePath();
			
			writer.addMessage("Final VCF", serverURL + resultDir + linkName );
			if (createLinks) {
				createLink(linkTarget, webRoot + resultDir + linkName );
			}
		}
		
		if (finalBam != null) {
			
			String linkName = sampleID + ".final.bam"; 
			String linkTarget = finalBam.getAbsolutePath();

			writer.addMessage("Final BAM",  serverURL + resultDir + linkName );
			
			if (createLinks) {
				createLink(linkTarget, webRoot +  resultDir + linkName );
				createLink(linkTarget + ".bai", webRoot +  resultDir + linkName + ".bai" );
			}
		}
		
		if (annotatedVars != null) {
			String linkName = sampleID + ".annotated.csv"; 
			String linkTarget = annotatedVars.getAbsolutePath();
			
			writer.addMessage("Annotated variants", serverURL + resultDir + linkName );
			if (createLinks) {
				createLink(linkTarget, webRoot + resultDir + linkName );
			}			
		}
		
		
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
			sampleID = "sample-" + ("" + System.currentTimeMillis()).substring(5);
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
		if (this.getAttribute(SERVER_URL) != null && this.getAttribute(SERVER_URL).length()>1) {
			serverURL = this.getAttribute(SERVER_URL);
		}
		
		for(int i=0; i<inputChildren.getLength(); i++) {
			Node iChild = inputChildren.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				
				if (obj instanceof QCReport) {
					qcMetrics = (QCReport)obj;
					continue;
				}
				
				if (obj instanceof VCFFile) {
					vcfFile = (VCFFile)obj;
					continue;
				}
				
				if (obj instanceof BAMFile) {
					finalBam = (BAMFile)obj;
					continue;
				}
				
				if (obj instanceof StatusLogger) {
					statusLogger = (StatusLogger)obj;
					continue;
				}
				
				if (obj instanceof CSVFile) {
					annotatedVars = (CSVFile)obj;
					continue;
				}
				
				throw new IllegalArgumentException("Unknown input file type:" + obj.getObjectLabel() + " is class: " + obj.getClass().getCanonicalName());
			}
		}
		
		if (statusLogger == null) {
			throw new IllegalArgumentException("No StatusLogger provided to StatusFinalizer");
		}

		
	}

}
