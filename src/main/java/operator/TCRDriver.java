package operator;

import java.util.List;

import org.w3c.dom.NodeList;

import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.TextBuffer;

public class TCRDriver extends CommandOperator {

	public static final String PATH = "path";
	protected String TCRPath = "/home/dfisher/projects/TcrDriver_1.1/Apps/TcrDriver.jar";
	
	protected TextBuffer inputFile_v=null;
	protected TextBuffer inputFile_j=null;
	
	@Override
	protected String getCommand() throws OperationFailedException {
		
		Object propsPath = getPipelineProperty("tcr.PATH"); 
		if (propsPath != null)
			TCRPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			TCRPath = path;
		}
	
		List<FileBuffer> inputTextBuffer = getAllInputBuffersForClass(TextBuffer.class);
		inputFile_v = (TextBuffer) inputTextBuffer.get(0);
		inputFile_j = (TextBuffer) inputTextBuffer.get(1);
		
		FastQFile inputFastq =  (FastQFile) getInputBufferForClass(FastQFile.class);
		
		
		
		FileBuffer output = getOutputBufferForClass(TextBuffer.class);
		
		
		String command = "java -jar " + TCRPath + " -v " + inputFile_v.getAbsolutePath() + " -j " + inputFile_j.getAbsolutePath();
		command = command + " -o " + output.getAbsolutePath() +" -i " + inputFastq.getAbsolutePath();
		
		return command;
	}
	
	public void initialize(NodeList children) {
		super.initialize(children);
		
		int num = getAllInputBuffersForClass(TextBuffer.class).size();
		if (num != 2) {
			throw new IllegalArgumentException("Could not find exactly two input files of type text buffer");
		}
	}
	

}