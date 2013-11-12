package operator;

import java.io.File;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
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
		
		List<FileBuffer> output = this.getAllOutputBuffersForClass(FastQFile.class);
		File outputA = new File(getProjectHome() + "TCR_out.txt");
		output.get(0).setFile(outputA);
		
		String command = "java -jar " + TCRPath + " -v " + inputFile_v.getAbsolutePath() + " -j " + inputFile_j.getAbsolutePath();
		command = command + " -o "+outputA+" -i fastq";
		
		return command;
	}
	
	public void initialize(NodeList children) {
		super.initialize(children);
		
		Node iChild = children.item(0);
		if (iChild.getNodeType() == Node.ELEMENT_NODE) {
			PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					
			if (obj instanceof TextBuffer ) {
				if (inputFile_v == null) {
					inputFile_v = (TextBuffer) obj;
				}
			}
		}
		
		iChild = children.item(1);
		if (iChild.getNodeType() == Node.ELEMENT_NODE) {
			PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					
			if (obj instanceof TextBuffer ) {
				if (inputFile_j == null) {
					inputFile_j = (TextBuffer) obj;
				}
			}
		}		
				
		inputFile_v = (TextBuffer)super.getInputBufferForClass(TextBuffer.class);
		inputFile_j = (TextBuffer)super.getInputBufferForClass(TextBuffer.class);
		
	}

}