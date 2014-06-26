package operator.qc;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import buffer.FastQFile;
import buffer.FileBuffer;
import operator.CommandOperator;
import operator.MultiOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * This guy runs FastQC on the input fastq's to assess sequence quality
 * @author brendan
 *
 */
public class FastQC extends MultiOperator {

	@Override
	protected String[] getCommand(FileBuffer inputFile) {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		String fastqcPath = "~/FastQC/fastqc ";
		Object path = getPipelineProperty(PipelineXMLConstants.FASTQC_PATH);
		if (path != null)
			fastqcPath = path.toString();
		else {
			logger.warning("No path to FastQC specified in properties, defaulting to " + fastqcPath);
		}
	
		if (! (inputFile instanceof FastQFile)) {
			logger.warning("Non-fastq file found for as input to FastQC, ignoring it and proceeding.");
			return new String[]{};
		}
		
		//Make the output directory 'fastqc'
		File qcDir = new File( getProjectHome() + "fastqc");
		if (! qcDir.exists()) {
			qcDir.mkdir();
		}
		else {
			//Destination directory already exists... is it not a directory?
			if (qcDir.isFile()) { //If not a directory, just put output files in project-home
				qcDir = new File( getProjectHome() );
			}
		}
		
		//Always use four threads no matter what
		String command = fastqcPath + " " + inputFile.getAbsolutePath() + " -o " + qcDir.getAbsolutePath() + " -t 4 ";
		return new String[]{command};
	}

}
