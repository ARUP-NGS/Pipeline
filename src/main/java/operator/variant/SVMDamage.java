package operator.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import buffer.variant.VariantRec;

public class SVMDamage extends Annotator {

	public static final String LIBSVM_PATH = "libsvm.path";
	public static final String SVM_MODEL_PATH = "svm.model.path";
	String libsvmPath = null;
	String modelPath = null;
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		// TODO Auto-generated method stub
		
	}
	
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("No variant pool specified", this);
		
		if (libsvmPath == null) {
			libsvmPath = this.getAttribute(LIBSVM_PATH);
			if (libsvmPath == null) 
				libsvmPath = this.getPipelineProperty(LIBSVM_PATH);
			if (libsvmPath == null) {
				throw new OperationFailedException("No path to libsvm found. Use libsvm.path", this);
			}
		}
		
		if (modelPath == null) {
			modelPath = this.getAttribute(SVM_MODEL_PATH);
			if (modelPath == null) {
				throw new OperationFailedException("No path to svm-model found. Use svm.model.path", this);
			}
			
			if (! (new File(modelPath)).exists() ) {
				throw new OperationFailedException("svm model at path " + modelPath + " does not exist!", this);
			}
		}
		
		DecimalFormat formatter = new DecimalFormat("#0.00");
		int tot = variants.size();
		
		
		int varsAnnotated = 0;

		//First write all data to a (tmp) file
		File data = new File(this.getProjectHome() + "/svmdata" + ("" + (10000.0*Math.random())).substring(0, 4) + ".csv");
		data.deleteOnExit();
		File chrPosFile = new File(this.getProjectHome() + "/svmdata.chrPos." + ("" + (10000.0*Math.random())).substring(0, 4) + ".csv");
		chrPosFile.deleteOnExit();
		
		try {
			PrintStream dataStream = new PrintStream(new FileOutputStream(data));
			PrintStream chrPosStream = new PrintStream(new FileOutputStream(chrPosFile));
			
			int index = 0;
			for(String contig : variants.getContigs()) {
				for(VariantRec rec : variants.getVariantsForContig(contig)) {
					
					if (rec.getAnnotation(VariantRec.EXON_FUNCTION) != null &&
							rec.getAnnotation(VariantRec.EXON_FUNCTION).contains("nonsynonymous")) {
						String dataLine = getDataLine(index, rec);
						index++;
						if (dataLine != null) {
							dataStream.println(dataLine);
							chrPosStream.println(rec.getContig() + ":" + rec.getStart() + ":" + rec.getAlt());
						}
					}
					else {
						//Variation is not a nonsynonymous SNP, compute alternate score
						computeNonMissenseScore(rec);
					}
				}
			}
			
			dataStream.close();
			chrPosStream.close();
			
			//Run libsvm predict...
			String pathToOutput = data.getAbsolutePath() + ("." + (int)(1000.0*Math.random())) + ".output";
			String command = libsvmPath + "/svm-predict" + " -b 1 " + data.getAbsolutePath() + " " + modelPath + " " + pathToOutput ;
			executeLIBSVMCommand(command);
			
			//Parse output from libsvm and the chrPos file
			BufferedReader outputReader = new BufferedReader(new FileReader(pathToOutput));
			BufferedReader chrPosReader = new BufferedReader(new FileReader(chrPosFile));
			
			String outputLine = outputReader.readLine();
			//Figure out which column contains the values associated with the +1 class
			int probIndex = 1;
			String[] toks = outputLine.split(" ");
			if (toks[1].equals("-1"))
				probIndex = 2;
			else
				probIndex = 1;
			outputLine = outputReader.readLine();
			
			String chrPos = chrPosReader.readLine();
			while(outputLine != null && chrPos != null) {
				toks = outputLine.split(" ");
				Double prob = Double.parseDouble(toks[probIndex]);
				
				String[] posToks = chrPos.split(":");
				String contig = posToks[0];
				Integer pos = Integer.parseInt(posToks[1]);
				VariantRec rec = variants.findRecord(contig, pos);
				if (rec == null) {
					throw new OperationFailedException("Yikes! Couldn't find variant at position " + contig + ":" + pos + ", even though it was there a second ago....", this);
				}
				rec.addProperty(VariantRec.SVM_EFFECT, prob);
				
				chrPos = chrPosReader.readLine();
				outputLine = outputReader.readLine();
			}
			
			File outputFile = new File(pathToOutput);
			outputFile.deleteOnExit();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}

	private void computeNonMissenseScore(VariantRec rec) {
		String exonFuncType = rec.getAnnotation(VariantRec.EXON_FUNCTION);
		double score = 0.0;
		if (exonFuncType != null) {
			if (exonFuncType.contains("frameshift")) {
				if (exonFuncType.contains("non"))
					score = 0.9;
				else
					score = 0.99;
			}
			if (exonFuncType.contains("stopgain"))
				score =  0.99;
			if (exonFuncType.contains("stoploss"))
				score = 0.95;
			if (exonFuncType.contains("splice"))
				score = 0.98;
			
			rec.addProperty(VariantRec.SVM_EFFECT, score);
		}
		
	}

	private String getDataLine(int index, VariantRec rec) {
		try {
			StringBuffer dataLine = new StringBuffer("" + index);

			Double siftStr = rec.getProperty(VariantRec.SIFT_SCORE);
			Double ppStr = rec.getProperty(VariantRec.POLYPHEN_SCORE);
			Double mtStr = rec.getProperty(VariantRec.MT_SCORE);
			Double gerpStr = rec.getProperty(VariantRec.GERP_SCORE);
			Double phylopStr = rec.getProperty(VariantRec.PHYLOP_SCORE);
			Double siphyStr = rec.getProperty(VariantRec.SIPHY_SCORE);
			Double lrtStr = rec.getProperty(VariantRec.LRT_SCORE);
			Double slrStr = rec.getProperty(VariantRec.SLR_TEST);
			Double gerpNRStr = rec.getProperty(VariantRec.GERP_NR_SCORE);
			Double ppHvar = rec.getProperty(VariantRec.POLYPHEN_HVAR_SCORE);
			Double maStr = rec.getProperty(VariantRec.MA_SCORE);

			if (siftStr == null || Double.isNaN(siftStr))
				siftStr = SIFT_DEFAULT;
			if (ppStr == null || Double.isNaN(ppStr))
				ppStr = PP_DEFAULT;
			if (ppHvar == null || Double.isNaN(ppHvar))
				ppHvar = PP_DEFAULT;
			if (mtStr == null || Double.isNaN(mtStr))
				mtStr = MT_DEFAULT;
			if (maStr == null || Double.isNaN(maStr))
				maStr = MA_DEFAULT;
			if (lrtStr == null || Double.isNaN(lrtStr))
				lrtStr = LRT_DEFAULT;
			if (slrStr == null || Double.isNaN(slrStr))
				slrStr = SLR_DEFAULT;
			
			if (siftStr == null
					|| ppStr == null
					|| mtStr == null
					|| gerpStr == null
					|| phylopStr == null
					|| siphyStr == null
					|| lrtStr == null
					|| slrStr == null
					|| gerpNRStr == null
					|| ppHvar == null
					|| maStr == null) {
				return null;
			}
			
			dataLine.append( "\t1:" + siftStr + "\t2:" + ppStr + "\t3:" + mtStr + "\t4:" + gerpStr
					+ "\t5:" + phylopStr + "\t6:" + siphyStr + "\t7:" + lrtStr
					+ "\t8:" + slrStr + "\t9:" + gerpNRStr + "\t10:" + ppHvar + "\t11:" + maStr );

			String str = dataLine.toString();
			if (str.contains("null") || str.contains("NaN")) {
				return null;
			}
			return dataLine.toString();
		}
		catch(NumberFormatException nfe) {
			//Some of these are expected, not every variant will have all annotations
			return null;
		}
		catch(Exception ex) {
			//May be some null ptr exceptions too
			return null;
		}
	}

	
	protected void executeLIBSVMCommand(String command) {
		ProcessBuilder procBuilder = new ProcessBuilder("bash", "-c", command);

		System.out.println("LIBSVM tool executing command : " + command);
		
		try {
			Process proc = procBuilder.start();
			int exitVal = proc.waitFor();
			if (exitVal != 0) {
				System.err.println("Warning: libsvm process executing command " + command + " reported an error");
			}
		}
		catch (InterruptedException ex) {
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static final double SIPHY_DEFAULT = 5; // Used when PP values are missing
	public static final double SIFT_DEFAULT = 0.5; // Used when PP values are missing
	public static final double PP_DEFAULT = 0.5; // Used when PP values are missing
	public static final double MT_DEFAULT = 0.5; // Used when MT values are missing
	public static final double MA_DEFAULT = 0.5; // Used when MT values are missing
	public static final double LRT_DEFAULT = 0.5; // Used when MT values are missing
	public static final double SLR_DEFAULT = 0.0; // Used when MT values are missing
	
}
