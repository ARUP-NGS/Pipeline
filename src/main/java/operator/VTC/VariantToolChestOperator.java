/**
 * 
 */
package operator.VTC;

import buffer.ReferenceFile;
import operator.CommandOperator;
import operator.OperationFailedException;

/**
 * @author markebbert
 *
 */
public class VariantToolChestOperator extends CommandOperator {

	private final static String VTCPath = "vtc.path";
	private final static String VTCTool = "vtc.tool"; // Which tool to use
	private final static String INPUT_FILES = "input.files"; // A space-separated list of input files
	private final static String MEM_OPTIONS = "mem.options";
	private final static String JVM_ARGS= "jvmargs";
	private final static String POST_ADDRESS = "post.address";

    /* Specify path to the analysis types file. This file specifies all analysis
     * types to include in the frequency calculations
     */
	private final static String ANALYSIS_TYPES_FILE = "analysis.types.file"; 
	
	/* What root directory to begin traversing looking for sampleManifest files.
	 * Only applicable to the ARUPFrequencyCalculator tool
	 */
	private final static String ROOT_DIR = "root.dir";

	/**
	 * 
	 */
	public VariantToolChestOperator() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see operator.CommandOperator#getCommand()
	 */
	@Override
	protected String getCommand() throws OperationFailedException {
		Object vtcPathProp = getPipelineProperty(VTCPath);
		
		String vtcPath = "";
		if(vtcPathProp != null){
			vtcPath = vtcPathProp.toString();
		}
		else{
			throw new OperationFailedException("'vtc.path' not defined in pipeline properties.", this);
		}
		
		String vtcToolString = properties.get(VTCTool);
		String inputFileString = properties.get(INPUT_FILES);
		String rootDir = properties.get(ROOT_DIR);
		String analysisTypesFile = properties.get(ANALYSIS_TYPES_FILE);
		String postAddress = properties.get(POST_ADDRESS);
		String reference = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		
		String memOptions = " -Xms2g -Xmx16g ";
		String memOptionsString = properties.get(MEM_OPTIONS);
		if(memOptionsString != null){
			memOptions = memOptionsString;
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
		if (!jvmARGStr.contains("java.io.tmpdir"))
				jvmARGStr =jvmARGStr + " -Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir");
		
		StringBuilder sb = new StringBuilder();
		sb.append("java");
		sb.append(memOptions);
		sb.append(jvmARGStr);
		sb.append(" -jar ");
		sb.append(vtcPath);
		sb.append(" ");
		sb.append(vtcToolString);
		
		if("AFC".equals(vtcToolString) || "ARUPFrequencyCalculator".equals(vtcToolString)){
			sb.append(" -a ");
			sb.append(analysisTypesFile);
			if(postAddress != null){
				sb.append(" -n ");
				sb.append(postAddress);
			}
			sb.append(" ");
			sb.append(reference);
			sb.append(" ");
			sb.append(rootDir);
		}
		return sb.toString();
	}

}
