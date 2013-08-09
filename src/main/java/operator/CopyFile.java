package operator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;

/**
 * Copies one or more text files to a destination directory. No binary support implemented yet. 
 * @author brendan
 *
 */
public class CopyFile extends Operator {

	public static final String DEST_DIR = "destination.dir";
	
	List<FileBuffer> filesToCopy = null;
	File destinationDir = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		int count = 0;
		for(FileBuffer buf : filesToCopy) {
			File destFullPath = new File(destinationDir.getAbsolutePath() + System.getProperty("file.separator") + buf.getFilename());
			logger.info("Copying file " + buf.getFilename() + " to " + destFullPath.getAbsolutePath());
			try {
				copyTextFile(buf.getFile(), destFullPath);
				count++;
			} catch (IOException e) {
				logger.warning("Error copying file : "+ e.getMessage());
			}
		}
		
		logger.info("Copied " + count + " files to " + destinationDir.getAbsolutePath());
	}
	
	/**
	 * Copy the given file to the new destination - this will only work with text files
	 * @param source
	 * @param dest
	 * @throws IOException
	 */
	public static void copyTextFile(File source, File dest) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(source));
		if (! dest.exists()) {
			dest.createNewFile();
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(dest));
		String line = reader.readLine();
		while(line != null) {
			writer.write( line + "\n");
			line =reader.readLine();
		}
		
		writer.close();
		reader.close();
	}

	@Override
	public void initialize(NodeList children) {
		filesToCopy = new ArrayList<FileBuffer>();
		
		String destDirPath = this.getAttribute(DEST_DIR);
		if (destDirPath == null) {
			throw new IllegalArgumentException("No destination directory specified");
		}
		
		destinationDir = new File(this.getAttribute(DEST_DIR));
		if (! destinationDir.exists()) {
			throw new IllegalArgumentException("Destination directory " + destinationDir.getAbsolutePath() + " does not exist");
		}
		if (! destinationDir.isDirectory()) {
			throw new IllegalArgumentException("Destination directory " + destinationDir.getAbsolutePath() + " is not a directory");
		}
		
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof MultiFileBuffer) {
					throw new IllegalArgumentException("No multi-file support yet in this CopyFile operator");
					
				}
				
				if (obj instanceof FileBuffer) {
					filesToCopy.add( (FileBuffer)obj);
				}
				
			}
		}
	}

}
