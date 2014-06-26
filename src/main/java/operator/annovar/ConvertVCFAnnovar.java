package operator.annovar;

import operator.CommandOperator;
import pipeline.PipelineXMLConstants;

/**
 * Uses annovar to convert an input file (often in .vcf format) into one suitable for input
 * to annovar's "annotate_variation" program 
 * @author brendan
 *
 */
public class ConvertVCFAnnovar extends CommandOperator {

	public static final String FORMAT = "format";
	protected String annovarPath = "~/annovar/";
	protected String format = "vcf4"; //Default assumed input format
	
	
	@Override
	protected String getCommand() {
		Object path = getPipelineProperty(PipelineXMLConstants.ANNOVAR_PATH);
		if (path != null)
			annovarPath = path.toString();
		
		//User can override path specified in properties
		String userPath = properties.get(PipelineXMLConstants.PATH);
		if (userPath != null) {
			annovarPath = userPath;
		}
		
		String formatStr = properties.get(FORMAT);
		if (formatStr != null)
			format = formatStr;
		
		String inputPath = inputBuffers.get(0).getAbsolutePath();
		String outputPath = outputBuffers.get(0).getAbsolutePath();
		
		String command = "perl " + annovarPath + "convert2annovar.pl -format " + format + " " + inputPath + " --outfile " + outputPath; 
		return command;	
	}

}
