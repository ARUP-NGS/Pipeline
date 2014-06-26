package operator;

import org.w3c.dom.NodeList;

/**
 * An operator to call the db uploading script which imports vcf data into
 * @author brendan
 *
 */
public class DBUploader extends CommandOperator {

	public static final String DB_UPLOAD_SCRIPT = "db.upload.script";
	public static final String SAMPLE = "sample";
	private String scriptPath = null;
	private String sampleID = null;
	
	@Override
	protected String getCommand() throws OperationFailedException {
		String  cmd = scriptPath + " " + sampleID;
		return cmd;
	}
	
	public void initialize(NodeList children) {
		super.initialize(children);
				
		scriptPath = this.getPipelineProperty(DB_UPLOAD_SCRIPT);
		if (scriptPath == null) {
			throw new IllegalArgumentException("Path to db upload script not specified, use db.upload.script");
		}
		
		sampleID = this.getAttribute(SAMPLE);
		if (sampleID == null) {
			throw new IllegalArgumentException("SampleID not specified");
		}
	}

}
