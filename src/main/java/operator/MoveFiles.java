package operator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.GlobFileBuffer;
import buffer.MultiFileBuffer;
import buffer.VCFFile;

/**
 * Move input files to another directory, specified by a dest="path/to/destination" attribute
 * If directory does not exist, it is created. 
 * @author brendan
 *
 */
public class MoveFiles extends Operator {

	public static final String DEST = "dest";
	protected File destination = null;
	
	protected List<MultiFileBuffer> multiBuffers;
	protected List<FileBuffer> files;
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		String destinationPath = properties.get(DEST);
		if (destinationPath == null) {
			throw new OperationFailedException("No destination path specified, use dest=\"path/to/dir/\"", this);
		}
		
		String projHome = getProjectHome();
		
		if (! destinationPath.startsWith("/") && projHome != null) {
			destinationPath = projHome + destinationPath;
		}
		
		destination = new File(destinationPath);
		if (destination.exists()) {
			if (! destination.isDirectory()) {
				throw new OperationFailedException("Destination path " + destination.getAbsolutePath() + " is not a directory", this);
			}
		}
		else {
			destination.mkdir();
		}
		
		if (destination == null)
			throw new OperationFailedException("Destination directory has not been specified", this);
		
		String fileSep = System.getProperty("file.separator");
		
		//Force glob file buffers to re-find their files, and dump all file names
		//into one big list
		for(MultiFileBuffer buf : multiBuffers) {
			if (buf instanceof GlobFileBuffer) {
				((GlobFileBuffer)buf).findFiles();
			}

			for(int i=0; i<buf.getFileCount(); i++) {
				files.add( buf.getFile(i) );
				
				//Experimental : Find index files too...
				FileBuffer file = buf.getFile(i);
				if (file.getFilename().endsWith(".bam")) {
					FileBuffer index0 = new BAMFile(new File(file.getAbsolutePath().replace(".bam", ".bai")));
					FileBuffer index1 = new BAMFile(new File(file.getAbsolutePath() + ".bai"));
					
					files.add(index0);
					files.add(index1);
				}
				if (file.getFilename().endsWith("vcf")) {
					files.add(new VCFFile(new File(file.getAbsolutePath() + ".idx")));
				}
				
			}
		}
		
		for(FileBuffer file : files) {
			Logger.getLogger(Pipeline.primaryLoggerName).info("Moving file : " + file.getAbsolutePath());
			String filename = file.getFilename();
			String newPath = destination.getAbsolutePath() + fileSep + filename;
			System.out.println("Moving file " + filename + " to new path: " + newPath);
			File destinationFile = new File(newPath);
			file.getFile().renameTo(destinationFile);
			file.setFile(destinationFile);
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
