package buffer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;

/**
 * This object is a container for a list of file buffers. 
 * @author brendan
 */

public class MultiFileBuffer extends FileBuffer {
	
	public final static String GUESS_CONTIG = "guesscontig"; 

	List<FileBuffer> files = new ArrayList<FileBuffer>();
	
	public MultiFileBuffer() {
		file = null;
	}
	
	public void addFile(FileBuffer buf) {
		//System.out.println("Adding file : " + buf.getFile().getAbsolutePath() + " to MultiFileBuffer " + getObjectLabel());
		files.add(buf);
	}
	
	/**
	 * Return the list of files that this buffer encapsulates
	 * @return
	 */
	public List<FileBuffer> getFiles() {
		return files;
	}
	
	public void removeFile(FileBuffer buf) {
		files.remove(buf);
	}
	
	public int getFileCount() {
		return files.size();
	}
	
	public FileBuffer getFile(int i) {
		return files.get(i);
	}

	public List<FileBuffer> getFileList() {
		return files;
	}
	
	@Override
	public void initialize(NodeList children) {
		boolean guessContig = true;
		String guessAttr = properties.get(GUESS_CONTIG);
		if (guessAttr != null) {
			guessContig = Boolean.parseBoolean(guessAttr);
		}
		
		for(int i=0; i<children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(node.getNodeName());
				if (obj instanceof FileBuffer) {
					FileBuffer buf = (FileBuffer)obj;
					 if (guessContig) {
						 String contig = guessContig( buf);
						 buf.setContig(contig);
					 }
					 
					 addFile( (FileBuffer)obj);
					
				}
				else {
					throw new IllegalArgumentException("Got non-filebuffer object as argument to MultiFile container, it has name: " + node.getNodeName());
				}
			}
		}
	}

	/**
	 * Attempt to guess the contig of the given filebuffer. If the filebuffer has the CONTIG property set, we
	 * always use that. Otherwise, we look for patterns of the form /^contig_XX.othertsuff.bam/ or
	 *   /somestuff.cX.otherstuff.bam/ where X is the contig to be parsed and returned.
	 * @param buff
	 * @return String representing the label of the contig (typically 1..21, X, or Y)
	 */
	protected String guessContig(FileBuffer buff) {
		if (buff.getContig() != null) {
			return buff.getContig();
		}
		else {

			String name = buff.getFile().getName();
			int index1 = -1;
			int index2 = -1;
			if (name.contains("contig")) {
				index1 = name.indexOf("contig")+7;
				index2 = name.indexOf(".");
			}
			else {
				index1 = name.indexOf(".c");
				index2 = name.indexOf(".", index1+1);
				index1+=2;
				
			}
			if (index1 < 0 || index2<0 || (index2 <= index1)) {
				throw new IllegalArgumentException("Could not guess contig for filename " + name);
			}
			
			String contStr = name.substring(index1, index2);
			Logger.getLogger(Pipeline.primaryLoggerName).info("Parsed contig " + contStr + " from filename " + name);
			return contStr;
		}
	}

	/**
	 * No single File is associated with this object 
	 * @return
	 */
	public File getFile() {
		return null;
	}
	
	/**
	 * Return the name of the file
	 * @return
	 */
	public String getFilename() {
		throw new IllegalStateException("Cannot read single filename from MultiFileBuffer");
	}
	
	/**
	 * Return the absolute path of the file
	 * @return
	 */
	public String getAbsolutePath() {
		throw new IllegalStateException("Cannot read single file path from MultiFileBuffer");
	}

	@Override
	public String getTypeStr() {
		return "Multi-file buffer";
	}
}
