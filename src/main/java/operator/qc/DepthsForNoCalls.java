package operator.qc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.CSVFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;

/**
 * Takes a "NoCallCSV", of the type produced by the CallableLoci walker / operator, and uses the GATK's
 * DepthOfCoverage tool to compute the depths for the un-callable regions
 * @author brendan
 *
 */
public class DepthsForNoCalls extends IOOperator {

	public static final String JVM_ARGS="jvmargs";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	
	private CSVFile noCalls = null;
	private ReferenceFile reference = null;
	private BAMFile inputBAM = null;
	private CSVFile noCallDepths = null;
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		Object propsPath = getPipelineProperty(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null) {
			gatkPath = propsPath.toString();
		}
		//Additional args for jvm
		String jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = (String) getPipelineProperty(JVM_ARGS);
		}
		//If it's still null then be sure to make it the empty string
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = "";
		}

		//First, we must create a new, temporary BED file by taking the nocalls and filtering out
		//all 'callable' regions....
		try {
			BEDFile noCallBED = getNoCallBED();
			
			File outputFile = new File( noCallDepths.getAbsolutePath() );
			outputFile.createNewFile();
			noCallDepths.setFile(outputFile);
			
			int noCallSize = noCallBED.getIntervalCount();
			
			//If there are zero no-call regions GATK will break, so identify this
			//and abort if necessary
			if (noCallSize < 1) {
				Logger.getLogger(Pipeline.primaryLoggerName).info("No call BED file is empty, assuming zero no-call regions and aborting.");
				return;
			}
			
				//Now run DepthOfCoverageWalker on the BED
				String command = "java -Xmx8g " + jvmARGStr + " -jar " + gatkPath;
				command = command + " -R " + reference.getAbsolutePath() + 
						" -I " + inputBAM.getAbsolutePath() + 
						" -T DepthOfCoverage" +
						" -rf BadCigar " +
						" -L " + noCallBED.getAbsolutePath() +
						" --omitDepthOutputAtEachBase " +
						" -o nocallDepths ";


				Logger.getLogger(Pipeline.primaryLoggerName).info("Running depth of coverage tool for no-call regions...");
				executeCommand(command);
			
			
			//We now want to parse "nocallDepths.sample_interval_summary"
			File intervalSummary = new File( this.getProjectHome() + "/nocallDepths.sample_interval_summary");
			if (! intervalSummary.exists()) {
				throw new OperationFailedException("No interval summary file found, an error must have occurred", this);
			}
			
			
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			
			BufferedReader noCallReader = new BufferedReader(new FileReader(noCallBED.getAbsolutePath()));
			BufferedReader depthReader = new BufferedReader(new FileReader(intervalSummary.getAbsolutePath()));
			
			
			
			String noCallLine = noCallReader.readLine();
			String depthLine = depthReader.readLine();
			
			depthLine = depthReader.readLine(); //Skip header line of depth file
			while(noCallLine != null && depthLine != null) {
				String[] depthToks = depthLine.split("\t");
				String mean = depthToks[2];
				writer.write(noCallLine.trim().replace(" ", "\t") + "\t" + mean + "\n");
				noCallLine = noCallReader.readLine();
				depthLine = depthReader.readLine();
			}
			
			writer.close();
			
			noCallReader.close();
			depthReader.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new OperationFailedException("Exiting pipeline.", this); 
		}
		
	}
	
	/**
	 * Create a new BED file containing
	 * @return
	 * @throws IOException
	 */
	private BEDFile getNoCallBED() throws IOException {
		String tempBedName = "nocalls.tmp.bed";
		File tempBedFile = new File( this.getProjectHome() + "/" + tempBedName);
		BufferedReader reader = new BufferedReader(new FileReader(noCalls.getFile()));
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(tempBedFile));
		
		String line = reader.readLine();
		while(line != null) {
			String[] toks = line.split(" ");
			int size = Integer.MAX_VALUE;
			if (toks.length > 3) {
				size = Integer.parseInt(toks[2]) - Integer.parseInt(toks[1]);
			}
			if (size > 0 && (!line.contains("CALLABLE"))) {
				writer.write(line + "\n");
			}
			line = reader.readLine();
		}
		
		reader.close();
		writer.close();
		
		return new BEDFile(tempBedFile);
	}
	
	
	public void initialize(NodeList children) {
		super.initialize(children);
		
		FileBuffer noCallCSV = getInputBufferForClass(CSVFile.class);
		if (noCallCSV == null) {
			throw new IllegalArgumentException("DepthsForNoCalls requires a no-call CSV file as input");
		}
		noCalls = (CSVFile) noCallCSV;
		
		
		FileBuffer bam = getInputBufferForClass(BAMFile.class);
		if (bam == null) {
			throw new IllegalArgumentException("DepthsForNoCalls requires a BAM file as input");
		}
		inputBAM = (BAMFile) bam;
		
		reference = (ReferenceFile) getInputBufferForClass(ReferenceFile.class);
		
		
		FileBuffer noCallDepthsFile = getOutputBufferForClass(CSVFile.class);
		if (noCallDepthsFile == null) {
			throw new IllegalArgumentException("DepthsForNoCalls requires a CSV file as output");
		}
		noCallDepths = (CSVFile) noCallDepthsFile;
	}

}
