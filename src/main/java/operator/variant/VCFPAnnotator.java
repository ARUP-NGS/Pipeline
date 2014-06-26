package operator.variant;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import util.VCFLineParser;
import buffer.FileBuffer;
import buffer.GlobFileBuffer;
import buffer.MultiFileBuffer;
import buffer.VCFFile;
import buffer.variant.VariantRec;

/**
 * Computes a transformed "FP" score for all variants which reflects the probability that the variant
 * frequency was sampled from a distribution other than a simple binomial.
 *  Also, computes a log-transformed strand bias score which looks a lot more Gaussian-like than
 * the usual FS-score,  which should help for VQSR recalibration
 * @author brendan
 *
 */
public class VCFPAnnotator extends IOOperator {

	public static final double lambda = 0.30;
	
	@Override
	public void performOperation() throws OperationFailedException {
	
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		FileBuffer inputBuffer = inputBuffers.get(0);
		FileBuffer outputBuffer = outputBuffers.get(0);
		
//		if (inputBuffer.getClass() != outputBuffer.getClass()) {
//			throw new OperationFailedException("Incompatible input and output buffer types", this);
//		}
		
		VCFFile[] inputVCFs = null;
		VCFFile[] outputVCFs = null;
		
		if (inputBuffer instanceof VCFFile) {
			inputVCFs = new VCFFile[1];
			inputVCFs[0] = (VCFFile)inputBuffer;
		}
		if (inputBuffer instanceof GlobFileBuffer) {
			inputVCFs = new VCFFile[ ((GlobFileBuffer) inputBuffer).getFileCount() ];
			for(int i=0; i<inputVCFs.length; i++) {
				inputVCFs[i] = (VCFFile) ((GlobFileBuffer) inputBuffer).getFile(i);
			}
		}
		if (inputVCFs == null) {
			throw new OperationFailedException("Could not read input VCF files", this);
		}
		
		
		if (outputBuffer instanceof VCFFile) {
			outputVCFs = new VCFFile[1];
			outputVCFs[0] = (VCFFile)outputBuffer;
		}
		if (outputBuffer instanceof MultiFileBuffer) {
			outputVCFs = new VCFFile[ ((MultiFileBuffer) inputBuffer).getFileCount() ];
			for(int i=0; i<inputVCFs.length; i++) {
				String outputName = inputVCFs[i].getAbsolutePath().replace(".vcf", ".fpfs.vcf");
				outputVCFs[i] = new VCFFile(new File(outputName));
			}
		}
		
		
		
		try {
			for(int i=0; i<inputVCFs.length; i++) {
				convertFile(inputVCFs[i], outputVCFs[i]);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new OperationFailedException("Error converting VCF", this);
		}
		
	}
	
	private void convertFile(VCFFile inputVCF, VCFFile outputVCF) throws IOException, OperationFailedException {
		Logger.getLogger(Pipeline.primaryLoggerName).info("Computing new FP and log FS fields for file: " + inputVCF.getAbsolutePath() + ", results in : " + outputVCF.getAbsolutePath());

		BufferedWriter writer = new BufferedWriter(new FileWriter(outputVCF.getAbsolutePath()));
		copyHeader(inputVCF, writer);
		VCFLineParser reader = new VCFLineParser(inputVCF);
		do {
			VariantRec var = reader.toVariantRec();
			
			//Compute transformed FP score
			Double fp = FPComputer.computeFPScore(var);
			String line;
			if (! Double.isNaN(fp)) {
				double tauFP = boxCoxTransform(-1.0*fp, lambda);
				String tauFPStr = "" + tauFP;
				tauFPStr = tauFPStr.substring(0, Math.min(tauFPStr.length(), 5));
				line = appendInfoTag(reader.getCurrentLine(), "TAUFP=" + tauFP);
			}
			else {
				line = reader.getCurrentLine();
			}
			
			//Compute log of strand bias score for normality's sake
			Double fs = var.getProperty(VariantRec.FS_SCORE);
			if (fs != null && fs > 0.0 && (! Double.isNaN(fs))) {
				double logfs = Math.log(fs);
				line = appendInfoTag(line, "LOGFS=" + logfs);
			}
			
			writer.write(line + "\n");
			
		} while(reader.advanceLine());
		
		writer.close();
	}
	
	private String appendInfoTag(String vcfLine, String tag) throws OperationFailedException {
		String[] toks = vcfLine.split("\t");
		if (toks.length != 10) {
			throw new OperationFailedException("Incorrect number of tokens on line: " + vcfLine, this);
		}
		toks[7] = toks[7] + ";" + tag;
		String line = toks[0];
		for(int i=1; i<toks.length; i++) {
			line = line + "\t" + toks[i];
		}
		
		return line;
	}

	/**
	 * Returns the Box-Cox transform of the variable x with parameter lambda
	 * @param x
	 * @param lambda
	 * @return
	 */
	public static double boxCoxTransform(double x, double lambda) {
		return (Math.pow(x, lambda)-1) / lambda;
	}

	/**
	 * Writes all header / pragma / comment lines from input vcf to the given writer
	 * @param source
	 * @param writer
	 * @throws IOException
	 */
	private void copyHeader(VCFFile source, Writer writer) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(source.getAbsolutePath()));
		String line = reader.readLine();
		while(line != null && line.startsWith("#")) {
			writer.write(line + "\n");
			line = reader.readLine();
		}
		reader.close();
	}
}
