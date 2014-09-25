package operator.gatk;

import gene.ExonLookupService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.BEDFile;
import buffer.DOCMetrics;
import buffer.DOCMetrics.FlaggedInterval;
import buffer.FileBuffer;
import buffer.ReferenceFile;

public class DepthOfCoverage extends IOOperator {

	public final String defaultMemOptions = " -Xms2g -Xmx8g";
	public static final String PATH = "path";
	public static final String THREADS = "threads";
	public static final String JVM_ARGS="jvmargs";
	public static final String EXON_MIN_DEPTH="exon.min.depth";
	protected String defaultGATKPath = "~/GenomeAnalysisTK-1.6/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	final static int[] cutoffs = new int[]{5, 8, 10, 15, 20, 50, 500, 1000};
	private DOCMetrics metrics = null;
	
	public boolean requiresReference() {
		return true;
	}
	
	
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		Object propsPath = getPipelineProperty(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		Integer minExonDepth = null;
		String exonMinDepthAttr = properties.get(EXON_MIN_DEPTH);
		{
			minExonDepth = Integer.parseInt(exonMinDepthAttr);
		}
		
		//Additional args for jvm
		String jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = (String) getPipelineProperty(JVM_ARGS);
		}
		//If it's still null then be sure to make it the empty string
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = "";
		}
		
		
		
		
		FileBuffer inputBuffer = getInputBufferForClass(BAMFile.class);
		FileBuffer referenceFile = getInputBufferForClass(ReferenceFile.class);
		FileBuffer metricsFile = getOutputBufferForClass(DOCMetrics.class);
		if (metricsFile == null) {
			throw new OperationFailedException("No DOC metrics output file found", this);
		}
		metrics = (DOCMetrics) metricsFile;
		
		String inputPath = inputBuffer.getAbsolutePath();
			
		FileBuffer bedFile = getInputBufferForClass(BEDFile.class);
		String bedFilePath = "";
		if (bedFile != null) {
			bedFilePath = bedFile.getAbsolutePath();
		}
		
		String outputPrefix = inputBuffer.getFile().getName();
		outputPrefix = outputPrefix.replace("_final", "");
		outputPrefix = outputPrefix.replace(".bam", ".DOC");
		String projHome = getProjectHome();
		if (projHome != null && projHome.length() > 0) {
			outputPrefix = projHome + outputPrefix;
		}
		

		logger.info("Computing depth of coverage for file : " + inputBuffer.getAbsolutePath());
		
		String bedPath = "null";
		if (bedFile != null)
			bedPath = bedFile.getAbsolutePath();
		logger.info("DepthOfCoverage operator " + getObjectLabel() + " is calculating using bedfile: " + bedPath + " and output prefix: " + outputPrefix);
		
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		command = command + " -R " + referenceFile.getAbsolutePath() + 
				" -I " + inputPath + 
				" -T DepthOfCoverage" +
				" -rf BadCigar " +	//Dont fail on reads with bad cigar strings, which SNAP sometimes produces
				" --omitDepthOutputAtEachBase ";
		for(int i=0; i<cutoffs.length; i++) {
			command = command + " -ct " + cutoffs[i];
		}
		command = command + " -o " + outputPrefix;
		
		if (inputBuffer.getContig() != null) {
			command = command + " -L " + inputBuffer.getContig() + " ";
		}
		if (inputBuffer.getContig() == null && bedFile != null)
			command = command + " -L:intervals,BED " + bedFilePath;
		
		
		executeCommand(command);
		
		//Read output from summary file
		File summaryFile = new File(outputPrefix + ".sample_summary");
		metrics.setSourceFile( inputBuffer.getFile().getName() );
		try {
			BufferedReader reader = new BufferedReader(new FileReader(summaryFile));
			String first = reader.readLine();
			String second = reader.readLine();
			String[] toks = second.split("\t");
			double mean = Double.parseDouble(toks[2]);
			metrics.setMeanCoverage(mean);
			double[] fractions = new double[cutoffs.length];
			
			for(int i=0; i<cutoffs.length; i++) {
				double fraction = Double.parseDouble(toks[i+6]);
				fractions[i] = fraction;
			}
			metrics.setFractionAboveCutoff(fractions);
			metrics.setCutoffs(cutoffs);
			
			//Read output from interval summary file
			reader.close();
			File intervalSummary = new File(outputPrefix + ".sample_interval_summary");
			reader = new BufferedReader(new FileReader(intervalSummary));
			String line = reader.readLine();
			line = reader.readLine(); //Skip first line
			List<FlaggedInterval> problemIntervals = new ArrayList<FlaggedInterval>();
			while(line != null) {
				toks = line.split("\t");
				String interval = toks[0];
				Double meanCov = Double.parseDouble( toks[2] );
				Double percentOK = Double.parseDouble( toks[ toks.length - 3].trim() );
				
				if (minExonDepth != null && meanCov < minExonDepth) {
					FlaggedInterval fInt = new FlaggedInterval();
					fInt.info = interval;
					fInt.mean = meanCov;
					fInt.frac = percentOK;
					
					String[] posToks = interval.split("(:)|(-)"); 
					fInt.chr = posToks[0];
					fInt.start = Integer.parseInt(posToks[1]);
					fInt.end = Integer.parseInt(posToks[2]);
					
					problemIntervals.add(fInt);
				}
				
				
				line = reader.readLine();
			}
			
			metrics.setFlaggedIntervals(problemIntervals);
			
			//Annotate the problem interval regions so we know what they are
			ExonLookupService featureLookup = new ExonLookupService();
			String featureFile = getPipelineProperty("feature.file");
			if(featureFile == null){
				throw new IOException("PipelineProperty 'feature.file' not defined.");
			}
			File features = new File(featureFile);
			if (!features.exists()) {
				throw new IOException("Feature file " + features.getAbsolutePath() + " does not exist!");
			}
			featureLookup.buildExonMap(features);
			
			for(FlaggedInterval lowCovExon : metrics.getFlaggedIntervals()) {
				Object[] overlappingFeatures = featureLookup.getIntervalObjectsForRange(lowCovExon.chr, lowCovExon.start, lowCovExon.end);
				for(Object feat : overlappingFeatures) {
					lowCovExon.info = lowCovExon.info + feat.toString() + "; ";
				}
			}
			
			
			
			//Read output from interval summary file. Right now we assume that the second line of the
			//file is the proportion of reads with coverage greater than i, where i is tab-delimited
			//index number
			reader.close();
			File intervalCoverageProps = new File(outputPrefix + ".sample_cumulative_coverage_proportions");
			reader = new BufferedReader(new FileReader(intervalCoverageProps));
			line = reader.readLine();
//			toks = line.split("\t");
//			Integer[] depths = new Integer[toks.length];
//			for(int i=0; i<depths.length; i++) {
//				depths[i]  = i;
//			}
			
			line = reader.readLine();
			toks = line.split("\t");
			double[] prop = new double[toks.length];
			for(int i=1; i<prop.length; i++) {
				prop[i] = Double.parseDouble(toks[i]);
			}
			metrics.setCoverageProportions(prop);
			
			
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new OperationFailedException("Error reading output file : " + summaryFile.getAbsolutePath(), this);
		}
		
	}

}
