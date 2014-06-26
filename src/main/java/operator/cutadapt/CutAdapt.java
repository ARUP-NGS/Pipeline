package operator.cutadapt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import operator.MultiOperator;
import operator.OperationFailedException;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.TextBuffer;

/**
 * Runs 'cutadapt' to remove adapter sequences from fastq files
 * @author brendan
 *
 */
public class CutAdapt extends MultiOperator {

	public static final String CUTADAPT_PATH = "cutadapt.path";
	private String cutAdaptPath = null;
	private String adapterString = null; //Read in from a text file (textbuffer) to initialize
	
	@Override
	protected String[] getCommand(FileBuffer inputBuffer) throws OperationFailedException {
		if (cutAdaptPath == null) {
			cutAdaptPath = this.getPipelineProperty(CUTADAPT_PATH);
			if (cutAdaptPath == null)
				throw new OperationFailedException("No path to cutadapt found", this);
		}
		
		if (! (inputBuffer instanceof FastQFile)) {
			return null;
		}
		
		if (adapterString == null) {
			try {
				FileBuffer adapterBuf = this.getInputBufferForClass(TextBuffer.class);
				if (adapterBuf == null)
					throw new OperationFailedException("No adapter file specified, cannot cut adapters", this);
				BufferedReader reader = new BufferedReader(new FileReader(adapterBuf.getFile()));
				StringBuffer adapterStr = new StringBuffer();
				String line = reader.readLine();
				while(line != null) {
					adapterStr.append(" -a " + line.trim());
					line = reader.readLine();
				}
				reader.close();
				adapterString = adapterStr.toString();
			}
			catch (IOException ex) {
				throw new OperationFailedException("Error reading adapter sequences : " + ex.getMessage(), this);
			}
		}
		
		String outputFilename = "adaptercut-" + inputBuffer.getFilename();
		FastQFile outputFile = new FastQFile(new File(getProjectHome() + outputFilename));
		super.addOutputFile(outputFile);
		String command = cutAdaptPath + adapterString + " -o " + outputFile.getAbsolutePath() + " " + inputBuffer.getAbsolutePath();
		
		return new String[]{command};
	}

}
