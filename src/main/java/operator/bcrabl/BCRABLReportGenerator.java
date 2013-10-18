package operator.bcrabl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.BAMFile;
import buffer.TextBuffer;
import buffer.variant.VariantPool;

public class BCRABLReportGenerator extends Operator {

	SimpleReportGenerator reportBuilder = new SimpleReportGenerator();
	BAMFile bam = null;
	VariantPool vars = null;
	TextBuffer reportFile = null;
	
	public static final String MEAN_DEPTH = "mean.depth";
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		//Parse mean depth as an attribute
		String depthAttr = this.getAttribute(MEAN_DEPTH);
		if (depthAttr == null) {
			throw new OperationFailedException("Mean depth must be specified", this);
		}
		Double meanDepth = Double.parseDouble(depthAttr);
		
		BCRABLReport report = reportBuilder.getReportForSample(meanDepth, vars, bam);
		
		//Write report to output file
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(reportFile.getAbsolutePath()));
			
			writer.write("mean.read.depth=" + report.getMeanCoverage() + "\n");
			writer.write("message=" + report.getMessage() + "\n");
			writer.write("==text==\n");
			for(String text : report.getReportText()) {
				writer.write(text + "\n");	
			}
			
			writer.close();
		}
		catch (IOException ex) {
			throw new OperationFailedException("IOException writing report file: " + ex.getLocalizedMessage(), this);
		}
		
		
	}

	@Override
	public void initialize(NodeList nodes) {
		//Need a BAM, a variant pool, and an output file
		
		for(int i=0; i<nodes.getLength(); i++) {
			Node iChild = nodes.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof TextBuffer) {
					reportFile = (TextBuffer) obj;
				}
				if (obj instanceof BAMFile) {
					bam = (BAMFile) obj;
				}
				if (obj instanceof VariantPool) {
					vars = (VariantPool) obj;
				}
				
			}
		}
		
		
		if (reportFile == null) {
			throw new IllegalArgumentException("No report file (textbuffer) specified");
		}
		if (bam == null) {
			throw new IllegalArgumentException("No BAM file specified");
		}
		if (vars == null) {
			throw new IllegalArgumentException("No variants specified");
		}
	}

}
