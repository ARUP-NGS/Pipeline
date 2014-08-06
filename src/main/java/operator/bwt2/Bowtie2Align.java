package operator.bwt2;

import java.io.File;
import java.io.IOException;
import java.util.List;

import json.JSONException;
import operator.IOOperator;
import operator.OperationFailedException;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.BAMFile;
import buffer.ReferenceFile;

public class Bowtie2Align extends IOOperator {
	public static final String BOWTIE2_DIR = "bowtie2.dir";
	public static final String SAMTOOLS_PATH = "samtools.path";
	public static final String BOWTIE2_STYLE = "bowtie2.style";
	public static final String BOWTIE2_SENSITIVITY="bowtie2.sensitivity";
	public static final String EXTRA_OPTIONS="bwt2.options";
	public static final String THREADS = "threads";

	String sample = "unknown";
	String samtoolsPath = "samtools";
	String style = "--local";
	String sensitivity = "--very-sensitive";
	String bowtie2path = null;
	String extraOpts = "";
	String threads = "4";
	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		
		String extraAttr = this.getAttribute(EXTRA_OPTIONS);
		if(extraAttr != null) {
			extraOpts = extraAttr;
		}
		String threadsAttr = this.getAttribute(THREADS);
		if(threadsAttr != null) {
			threads = threadsAttr;
		}
			
		String pathAttr = this.getPipelineProperty(BOWTIE2_DIR);
		if (pathAttr == null) {
			throw new IllegalArgumentException("No path to Bowtie2 found, please specify " + BOWTIE2_DIR);
		}
		if (! (new File(pathAttr + "bowtie2").exists())) {
			throw new IllegalArgumentException("No file found at Bowtie2 path : " + pathAttr + "bowtie2");
		}
		bowtie2path = (pathAttr + "/bowtie2");
		
		String samtoolsAttr = this.getPipelineProperty(SAMTOOLS_PATH);
		if (samtoolsAttr == null) {
			throw new IllegalArgumentException("No path to samtools found, please specify " + SAMTOOLS_PATH);
		}
		if (! (new File(samtoolsAttr).exists())) {
			throw new IllegalArgumentException("No file found at samtools path : " + samtoolsAttr);
		}
		samtoolsPath = samtoolsAttr;
		String ref = this.getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		List<FileBuffer> InputFastqs = this.getAllInputBuffersForClass(FastQFile.class);
		String Reads1 = InputFastqs.get(0).getAbsolutePath();
		String Reads2 = InputFastqs.get(1).getAbsolutePath();
		String OutputBAM = this.getInputBufferForClass(BAMFile.class).getAbsolutePath();
		String OutputSAM = OutputBAM.substring(0, OutputBAM.lastIndexOf('.')) + ".sam";
		String command = bowtie2path + " -t " + threads + " " + extraOpts + " " + style + " " + sensitivity + " -x " + ref + " -1 " + Reads1 + " -2 " + Reads2 + " -S " + OutputSAM;
		executeCommand(command);
		String Sam2Bam = samtoolsPath + " view -Sbhu -o " + OutputBAM + " " + OutputSAM;
		executeCommand(Sam2Bam);
		String rmSam = "rm " + OutputSAM;
		executeCommand(rmSam);
	}
	
}
