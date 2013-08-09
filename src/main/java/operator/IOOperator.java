package operator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;

/**
 * An Input/Output operator. These are a type of operator that works on one or more input files
 * and writes to one or more output files. Almost all non-trivial operators are subclasses of this class.
 * Input and Output are specified by FileBuffers, which are basically just wrappers for files. 
 * 
 * 
 * @author brendan
 *
 */
public abstract class IOOperator extends Operator {

	
	protected List<FileBuffer> inputBuffers = new ArrayList<FileBuffer>();
	protected List<FileBuffer> outputBuffers = new ArrayList<FileBuffer>();
		
	public void addInputBuffer(FileBuffer buff) {
		inputBuffers.add(buff);
	}
	
	public void addOutputBuffer(FileBuffer buff) {
		outputBuffers.add(buff);
	}
	
	public boolean requiresReference() {
		return false;
	}
	
	/**
	 * Returns the first input buffer of the given class
	 * @param clz
	 * @return
	 */
	public FileBuffer getInputBufferForClass(Class<?> clz) {
		for(FileBuffer buff :  inputBuffers) {
			if (clz.isAssignableFrom(buff.getClass()))
				return buff;
		}
		return null;
	}
	
	/**
	 * Returns a list of all input buffers whose class matches the given class
	 * @param clz
	 * @return
	 */
	public List<FileBuffer> getAllInputBuffersForClass(Class<?> clz) {
		List<FileBuffer> buffers = new ArrayList<FileBuffer>();
		for(FileBuffer buff :  inputBuffers) {
			if (clz.isAssignableFrom(buff.getClass()))
				buffers.add(buff);
		}
		return buffers;
	}
	
	/**
	 * Returns a list of all input buffers whose class is assignable from the given class
	 * @param clz
	 * @return
	 */
	public List<FileBuffer> getAllOutputBuffersForClass(Class<FastQFile> clz) {
		List<FileBuffer> buffers = new ArrayList<FileBuffer>();
		for(FileBuffer buff :  outputBuffers) {
			if (clz.isAssignableFrom(buff.getClass()))
				buffers.add(buff);
		}
		return buffers;
	}
	
	/**
	 * Returns the first output buffer of the given class
	 * @param clz
	 * @return
	 */
	public FileBuffer getOutputBufferForClass(Class<?> clz) {
		for(FileBuffer buff :  outputBuffers) {
			if (clz.isAssignableFrom(buff.getClass()))
				return buff;
		}
		return null;
	}
	
	protected void checkContigs(List<FileBuffer> inputFiles) {
		for(int j=1; j<24; j++) {
			String contig = "" + j;
			if (j == 23) 
				contig = "X";
			boolean found = false;
			for(int i=0; i<inputFiles.size(); i++) {
				if (inputFiles.get(i).getContig() != null && inputFiles.get(i).getContig().equals(contig)) {
					found = true;
				}
			}
			
			if (!found) {
				System.err.println("Could not find contig " + contig + " in files!");
				for(int i=0; i<inputFiles.size(); i++) {
					FileBuffer buff = inputFiles.get(i);
					System.err.println(buff.getContig() + "\t" + buff.getAbsolutePath() );
				}
				throw new IllegalArgumentException("Could not find contig " + contig + " among files!");
			}
			
		}
		
	}
	
	@Override
	public void initialize(NodeList children) {
		
		Element inputList = getChildForLabel("input", children);
		Element outputList = getChildForLabel("output", children);
		
		if (inputList != null) {
			NodeList inputChildren = inputList.getChildNodes();
			for(int i=0; i<inputChildren.getLength(); i++) {
				Node iChild = inputChildren.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj instanceof FileBuffer) {
						addInputBuffer( (FileBuffer)obj );
					}
					else {
						throw new IllegalArgumentException("Found non-FileBuffer object in input list for Operator " + getObjectLabel());
					}
				}
			}
		}
		
		if (outputList != null) {
			NodeList outputChilden = outputList.getChildNodes();
			for(int i=0; i<outputChilden.getLength(); i++) {
				Node iChild = outputChilden.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj instanceof FileBuffer) {
						addOutputBuffer( (FileBuffer)obj );
					}
					else {
						throw new IllegalArgumentException("Found non-FileBuffer object in output list for Operator " + getObjectLabel());
					}
				}
			}
		}
		
		if ( requiresReference() ) {
			ReferenceFile ref = (ReferenceFile) getInputBufferForClass(ReferenceFile.class);
			if (ref == null) {
				throw new IllegalArgumentException("Operator " + getObjectLabel() + " requires reference file, but none were found");
			}
		}
	}
	
	
	/**
	 * Execute the given system command in its own process, and wait until the process has completed
	 * to return. If the exit value of the process is not zero, an OperationFailedException in thrown
	 * @param command
	 * @throws OperationFailedException
	 */
	protected void executeCommand(final String command) throws OperationFailedException {
		Runtime r = Runtime.getRuntime();
		final Process p;

		try {
			p = r.exec(command);
			
			//Weirdly, processes that emits tons of data to their error stream can cause some kind of 
			//system hang if the data isn't read. Since BWA and samtools both have the potential to do this
			//we by default capture the error stream here and write it to System.err to avoid hangs. s
			final Thread errConsumer = new StringPipeHandler(p.getErrorStream(), System.err);
			errConsumer.start();
			
			//If runtime is going down, destroy the process so it won't become orphaned
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					//System.err.println("Invoking shutdown thread, destroying task with command : " + command);
					p.destroy();
					errConsumer.interrupt();
				}
			});

			
			
			try {
				if (p.waitFor() != 0) {
					throw new OperationFailedException("Task terminated with nonzero exit value : " + System.err.toString() + " command was: " + command, this);
				}
			} catch (InterruptedException e) {
				throw new OperationFailedException("Task was interrupted : " + System.err.toString() + "\n" + e.getLocalizedMessage(), this);
			}

			
		}
		catch (IOException e1) {
			throw new OperationFailedException("Task encountered an IO exception : " + System.err.toString() + "\n" + e1.getLocalizedMessage(), this);
		}
	}
	
}
