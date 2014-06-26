package operator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;
import buffer.GlobFileBuffer;

/**
 * This just deletes the file or files given as arguments. Both simple filebuffers and
 * multi / glob files buffers can be listed as child nodes
 * @author brendan
 *
 */
public class RemoveFile extends Operator {

	protected List<FileBuffer> files;
	protected List<MultiFileBuffer> multiBuffers;

	@Override
	public void performOperation() throws OperationFailedException {
		
		//Force glob file buffers to re-find their files, and dump all file names
		//into one big list
		for(MultiFileBuffer buf : multiBuffers) {
			if (buf instanceof GlobFileBuffer) {
				((GlobFileBuffer)buf).findFiles();
			}
			
			for(int i=0; i<buf.getFileCount(); i++) {
				files.add( buf.getFile(i));
			}
		}
		
		for(FileBuffer file : files) {
			Logger.getLogger(Pipeline.primaryLoggerName).info("Deleting file : " + file.getAbsolutePath());
			file.getFile().delete();
		}
	}

	@Override
	public void initialize(NodeList children) {
		files = new ArrayList<FileBuffer>();
		multiBuffers = new ArrayList<MultiFileBuffer>();

		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof MultiFileBuffer) {
					MultiFileBuffer multiBuf = (MultiFileBuffer)obj;
					multiBuffers.add(multiBuf);
				}
				else {
					if (obj instanceof FileBuffer) {
						files.add( (FileBuffer)obj);
					}
				}
				
			}
		}
	}


}
