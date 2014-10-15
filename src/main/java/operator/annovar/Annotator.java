package operator.annovar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.Operator;
import operator.StringPipeHandler;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import util.Interval;
import buffer.BEDFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * Base class for things that can take a variant pool and add an annotation of some sort to
 * the variants
 * @author brendan
 *
 */
public abstract class Annotator extends Operator {

	protected VariantPool variants = null;
	protected BEDFile bedFile = null;
	

	/**
	 * Compute or obtain an annotation for the given variant and add it to the list of
	 * annotations or properties stored for the variant
	 * @param var
	 * @throws OperationFailedException 
	 */
	public abstract void annotateVariant(VariantRec var) throws OperationFailedException;
	
	/**
	 * If true, we write some progress indicators to system.out
	 * @return
	 */
	protected boolean displayProgress() {
		return false;
	}
	
	
	public VariantPool getVariants() {
		return variants;
	}
	
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("No variant pool specified", this);
		
		DecimalFormat formatter = new DecimalFormat("#0.00");
		int tot = variants.size();
		
	
		prepare();
		
		int varsAnnotated = 0;

		for(String contig : variants.getContigs()) {
			for(VariantRec rec : variants.getVariantsForContig(contig)) {
				//TODO veryify that recEnd is not incorrect for two alt alleles
				Integer recEnd = rec.getStart() + rec.getRef().length() - rec.getAlt().length();
				Interval recInterval = new Interval(rec.getStart(), recEnd);
				if (bedFile == null || bedFile.intersects(rec.getContig(), recInterval)) {
					annotateVariant(rec);
				
					varsAnnotated++;
					double prog = 100 * (double)varsAnnotated  / (double) tot;
					if (displayProgress() && varsAnnotated % 2000 == 0) {
						System.out.println("Annotated " + varsAnnotated + " of " + tot + " variants  (" + formatter.format(prog) + "% )");	
					}
				}
			}
		}
		
		cleanup();
	}

	/**
	 * Add the given annotation to the variant, appending it (after a semicolon) after the existing
	 * annotation if there is one already for the key, otherwise just creating it as usual. 
	 * @param var
	 * @param key
	 * @param value
	 */
	protected static void appendAnnotation(VariantRec var, String key, String value) {
		String existing = var.getAnnotation(key);
		if (existing == null) {
			var.addAnnotation(key, value);
		}
		else {
			var.addAnnotation(key, existing + "; " + value);
		}
	}

	
	/**
	 * This method is called prior to annotation of the variants. It's a no-op by default,
	 * but various subclasses may override if they need to do some work before 
	 * the annotation process begins (looking at you, VarBinAnnotator)
	 */
	protected void prepare() throws OperationFailedException {
		//Blank on purpose, subclasses may override 
	}
	
	protected void cleanup() throws OperationFailedException {
		//Blank on purpose, subclasses may override
	}
	
	@Override
	public void initialize(NodeList children) {
		if (children == null) {
			return;
		}
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof VariantPool) {
					variants = (VariantPool)obj;
				} else if (obj instanceof BEDFile) {
					bedFile = (BEDFile)obj;
					try {
						bedFile.buildIntervalsMap(true);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	protected void executeCommand(String command) throws OperationFailedException {
		Runtime r = Runtime.getRuntime();
		Process p;
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info(getObjectLabel() + " executing command : " + command);
		try {
			p = r.exec(command);

			try {
				if (p.waitFor() != 0) {
					logger.info("Task with command " + command + " for object " + getObjectLabel() + " exited with nonzero status");
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
	
	
	protected void executeCommandCaptureOutput(final String command, File outputFile) throws OperationFailedException {
		Runtime r = Runtime.getRuntime();
		final Process p;

		try {
			FileOutputStream outputStream = new FileOutputStream(outputFile);
			
			p = r.exec(command);
			
			//Weirdly, processes that emits tons of data to their error stream can cause some kind of 
			//system hang if the data isn't read. Since BWA and samtools both have the potential to do this
			//we by default capture the error stream here and write it to System.err to avoid hangs. s
			final Thread errConsumer = new StringPipeHandler(p.getErrorStream(), System.err);
			errConsumer.start();
			
			final Thread outputConsumer = new StringPipeHandler(p.getInputStream(), outputStream);
			outputConsumer.start();
			
			//If runtime is going down, destroy the process so it won't become orphaned
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					//System.err.println("Invoking shutdown thread, destroying task with command : " + command);
					p.destroy();
					errConsumer.interrupt();
					outputConsumer.interrupt();
				}
			});
		
			try {
				if (p.waitFor() != 0) {
					throw new OperationFailedException("Task terminated with nonzero exit value : " + System.err.toString() + " command was: " + command, this);
				}
			} catch (InterruptedException e) {
				throw new OperationFailedException("Task was interrupted : " + System.err.toString() + "\n" + e.getLocalizedMessage(), this);
			}

			outputStream.close();
		}
		catch (IOException e1) {
			throw new OperationFailedException("Task encountered an IO exception : " + System.err.toString() + "\n" + e1.getLocalizedMessage(), this);
		}
	}
}
