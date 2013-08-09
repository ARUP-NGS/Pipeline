package operator.bcrabl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.StringPipeHandler;
import operator.annovar.Annotator;
import pipeline.Pipeline;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * Provides annotations for BCR-ABL test results, which can't use annovar since 
 * they use a crazy reference.
 * @author brendan
 *
 */
public class BCRABLannotator extends Annotator {

	public static final String ANNOTATOR_PATH = "bcrabl.annotator.path";
	public static final String REF_PATH = "bcrabl.ref.path";
	
	static final String annotationsFile = "annotated.csv";
	
	//Store c.dot and p.dot information after it's read in from file
	private Map<Integer, String> cDots;
	private Map<Integer, String> pDots;
	
	/**
	 * Execute annotation script
	 */
	protected void prepare() {
		
		String scriptPath = this.getPipelineProperty(ANNOTATOR_PATH);
		File scriptFile = new File(scriptPath);
		if (! scriptFile.exists()) {
			throw new IllegalArgumentException("BCR-ABL annotation script file " + scriptFile.getAbsolutePath() + " does not exist");
		}
		
		String refPath = this.getPipelineProperty(REF_PATH);
		File refFile = new File(refPath);
		if (! refFile.exists()) {
			throw new IllegalArgumentException("BCR-ABL reference file " + refFile.getAbsolutePath() + " does not exist");
		}
		
		
		File inputVCF = new File(this.getProjectHome() + "/bcrablinput.vcf");
		File outputCSV = new File(this.getProjectHome() + "/annotated.csv");
		try {
			writeToFile(variants, inputVCF);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new IllegalArgumentException("Error annotating bcrabl variants: " + e1.getLocalizedMessage());
		}
		
		//Actually run annotation script
		String command = scriptPath + " -a " + refPath + " -i " + inputVCF.getAbsolutePath(); //This writes to file 'annotated.csv' 
		try {
			executeCommand(command);
		} catch (OperationFailedException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Error annotating bcrabl variants: " + e.getLocalizedMessage());

		}
		
		//read results from annotation script output into memory so we can annotate variants
		try {
			readResultsIntoMap(outputCSV);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Error annotating bcrabl variants: " + e.getLocalizedMessage());
		}
		
	}
	
	

	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		if (cDots == null || pDots == null) {
			throw new OperationFailedException("Annotations not initialized.", this);
		}
		
		String cDot = cDots.get(var.getStart());
		if (cDot != null) {
			if (cDot.contains(":")) {
				String nm = cDot.substring(0, cDot.indexOf(":"));
				String newCDot = cDot.substring(cDot.indexOf(":")+1, cDot.length());
				cDot = newCDot;
				var.addAnnotation(VariantRec.NM_NUMBER, nm);
			}
			
			var.addAnnotation(VariantRec.CDOT, cDot);
			
		}
		String pDot = pDots.get(var.getStart());
		if (pDot.contains(":")) {
			pDot = pDot.substring(pDot.indexOf(":")+1, pDot.length());
		}
		var.addAnnotation(VariantRec.PDOT, pDot);
	}

	
	private void readResultsIntoMap(File outputCSV) throws IOException {
		
		cDots = new HashMap<Integer, String>();
		pDots = new HashMap<Integer, String>();
		
		BufferedReader reader;

		reader = new BufferedReader(new FileReader(annotationsFile));

		String line = reader.readLine();
		while(line != null) {
			String[] toks = line.split("\t");
			if (toks.length != 3) {
				Logger.getLogger(Pipeline.primaryLoggerName).warning("Unrecognized line in bcr-abl annotation file: " + line);
			}
			else {
				Integer pos = Integer.parseInt(toks[0]);
				String cDot = toks[1];
				String pDot = toks[2];
				cDots.put(pos, cDot);
				pDots.put(pos, pDot);
			}
			line = reader.readLine();
		}

		reader.close();

	}

	private void writeToFile(VariantPool variants, File inputVCF) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(inputVCF));
		
		for(String contig : variants.getContigs()) {
			for(VariantRec var : variants.getVariantsForContig(contig)) {
				writer.write(contig + "\t" + var.getStart() + "\t.\t" + var.getRef() + "\t" + var.getAlt() + "\n");
			}
		}
		
		writer.close();
	}
	
	
	protected void executeCommand(final String command) throws OperationFailedException {
		Runtime r = Runtime.getRuntime();
		final Process p;

		try {
			Logger.getLogger(Pipeline.primaryLoggerName).info("Executing bcr-abl annotator command: " + command);
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
