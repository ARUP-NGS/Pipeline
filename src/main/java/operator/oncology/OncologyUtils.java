package operator.oncology;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.lang.IllegalArgumentException;

import json.JSONException;
import json.JSONObject;
import operator.IOOperator;
import operator.OperationFailedException;
import operator.StringPipeHandler;
import pipeline.Pipeline;
import util.FastaReader;
import util.bamUtil.ReadCounter;
import util.CompressGZIP;
import buffer.BAMFile;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;

/*
 * @author daniel
 * Calculates ratios and counts for alignments to multiple custom reference files. 
 * 
 * 
*/
public class OncologyUtils extends IOOperator {
		
	public static final String SAMTOOLS_PATH = "samtools.path";
	public static String defaultSamPath = "samtools";

	@Override
	public void performOperation() throws OperationFailedException, JSONException, IOException {
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Beginning utilities: Checking Arguments");
		System.out.println("Beginning utilities: Checking Arguments.");
		
		List<FileBuffer> FastqBuffers = this.getAllInputBuffersForClass(FastQFile.class); // Should contain 4 files
		List<FileBuffer> BamBuffers = this.getAllInputBuffersForClass(BAMFile.class);
		List<FileBuffer> CustomRefBuffers = this.getAllInputBuffersForClass(ReferenceFile.class);
		
		if(FastqBuffers.size() != 4) {
			System.out.println(FastqBuffers.size() + " fastq files provided.");
			throw new IllegalArgumentException("4 Fastq files required as input.");
		}
	
		
		if(BamBuffers.size() != 4) {
			System.out.println(BamBuffers.size() + " bam files provided.");
			throw new IllegalArgumentException("4 BAM files required as input.");
		}
		
		
		if(CustomRefBuffers.size() != 2) {
			System.out.println(CustomRefBuffers.size() + " fasta reference files provided.");
			throw new IllegalArgumentException("2 fasta reference files required as input.");
		}
		
		logger.info("Counting reads in Fastq Files");
		System.out.println("Counting reads in Fastq Files");
		long InFq = -1337;
		try {
			InFq = countLines(FastqBuffers.get(0).getAbsolutePath())/4;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		long Trim40Fq = -1337;
		try {
			Trim40Fq = countLines(FastqBuffers.get(1).getAbsolutePath())/4;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		long UnmappedFq = -1337;
		try {
			UnmappedFq = countLines(FastqBuffers.get(2).getAbsolutePath())/4;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		long Trim90Fq = -1337;
		try {
			Trim90Fq = countLines(FastqBuffers.get(3).getAbsolutePath())/4;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		/*
		 *  2. Count records for all 4 bam files
		 */
		logger.info("Counting records in BAM Files");
		System.out.println("Counting records in BAM Files");
		String samtoolsPath = defaultSamPath;
		String samtoolsAttr = getPipelineProperty(SAMTOOLS_PATH);
		if(samtoolsAttr != null) {
			samtoolsPath = samtoolsAttr;
		}
		
		String command_str = samtoolsPath + " view -c " + BamBuffers.get(0).getAbsolutePath();
		long ratioMapped = Integer.parseInt(executeCommandOutputToString(command_str).replaceAll("[^\\d.]", ""));
		String command_str1 = samtoolsPath + " view -c " + BamBuffers.get(1).getAbsolutePath();
		long ratioUnmapped = Integer.parseInt(executeCommandOutputToString(command_str1).replaceAll("[^\\d.]", ""));
		String command_str2 = samtoolsPath + " view -c " + BamBuffers.get(2).getAbsolutePath();
		long fusionMapped = Integer.parseInt(executeCommandOutputToString(command_str2).replaceAll("[^\\d.]", ""));
		String command_str3 = samtoolsPath + " view -c " + BamBuffers.get(3).getAbsolutePath();
		long fusionUnmapped = Integer.parseInt(executeCommandOutputToString(command_str3).replaceAll("[^\\d.]", ""));
		long short40 = InFq - Trim40Fq;
		long short90 = UnmappedFq - Trim90Fq; 
		//Get map containing # of reads per contig
		Map<String, Long> ratioMap = ReadCounter.countReadsByChromosome((BAMFile)BamBuffers.get(0),1);
		Map<String, Long> fusionMap = ReadCounter.countReadsByChromosome((BAMFile)BamBuffers.get(1),1);
		/*
		 * 3. Get list of "chromosomes"
		 */
		FastaReader FusionRef = null;
		FusionRef = new FastaReader(CustomRefBuffers.get(0).getFile());
		String[] FusionContigs = FusionRef.getContigs();
		int fusionLength = FusionContigs.length;
		System.out.println("Fusion Map has " + fusionLength + " chromosomes, as far as we have gone");
		
		FastaReader RatioRef = null;
		RatioRef = new FastaReader(CustomRefBuffers.get(1).getFile());
		String[] RatioContigs = RatioRef.getContigs();
		int ratioLength = RatioContigs.length;
		System.out.println("Ratio Map has " + ratioLength + " chromosomes, as far as we have gone");
		
		/*
		 * 4. Calculate ratios as needed
		 */
		
		double fracRatioMapped = (float) ratioMapped/InFq;
		double fracFusionMapped = (float) fusionMapped/InFq;
		double fracShort40Mapped = (float) short40/InFq;
		double fracShort90Mapped = (float) short90/InFq;
		double fracUnmapped = (float) fusionUnmapped/InFq;
		
		long[] fusionCounts = new long[fusionLength];
		long[] ratioCounts = new long[ratioLength];
		double[] fusionFrac = new double[fusionLength];
		double[] ratioFrac = new double[ratioLength];
		
		for(int i=0;i<fusionLength;i++) {
			fusionCounts[i]=fusionMap.get(FusionContigs[i]);
			fusionFrac[i]=(double)fusionCounts[i]/fusionMapped;
		}
		for(int i=0;i<ratioLength;i++) {
			ratioCounts[i]=ratioMap.get(RatioContigs[i]);
			ratioFrac[i]=(double)ratioCounts[i]/ratioMapped;
		}
		double[] ratioForRatio = new double[ratioLength/2];
		for(int i=0;i<ratioLength;i++){
			if(i%2==0){
				try {
					ratioForRatio[i/2]=(double)ratioCounts[i]/ratioCounts[i+1];
				}
				finally {
					ratioForRatio[i/2]=1000;
				}
			}
		}
		
		
		/* 5. Write results to JSON
		 * Stores results in a Hashmap (keys: "summary", "rna.ratio", & "rna.fusion" that is written to JSON
		 * @author elainegee
		 */
	    //Build summary map
		Map<String, Object> summary = new HashMap<String, Object>();
		summary.put("fraction of reads mapped to ratio reference", fracRatioMapped);
		summary.put("fraction of reads mapped to fusion reference", fracFusionMapped);
		summary.put("fraction of reads filtered out for lengths < 40", fracShort40Mapped);
		summary.put("fraction of reads unmapped to ratio reference filtered out for lengths < 90", fracShort90Mapped);
		summary.put("fration of unmapped reads", fracUnmapped);

		//Build rna ratio map
		Map<String, Object> rnaRatio = new HashMap<String, Object>();
		rnaRatio = buildFractionCountMap(RatioContigs, ratioCounts, ratioFrac);
		
		//Build rna fusion map
		Map<String, Object> rnaFusion = new HashMap<String, Object>();
		rnaFusion = buildFractionCountMap(FusionContigs, fusionCounts, fusionFrac);
		
		//Build final results map to be converted to JSON
		Map<String, Object> finalResults = new HashMap<String, Object>();
		finalResults.put( "summary", summary );
		finalResults.put( "rna.ratio", rnaRatio );
		finalResults.put( "rna.fusion", rnaFusion );

		//Convert final results to JSON
	    JSONObject json = new JSONObject(finalResults);
	    System.out.printf( "JSON: %s", json.toString(2) );
	    
		//Get the json string, then compress it to a byte array
		String str = json.toString();			
		byte[] bytes = CompressGZIP.compressGZIP(str);
		
		// Write compresssed JSON to file
		File dest = new File(getProjectHome() + "/rna_report.json.gz");
		BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(dest));
		writer.write(bytes);
		writer.close();
	    
	    
		return;
	}

	/**
	 * Synthesizes fraction & count information and return a map keyed by contig
	 * @author elainegee
	 * @return
	 */
	private Map<String, Object> buildFractionCountMap(String[] contigs, long[] counts, double[] fractions ) {
		Map<String, Object> results = new HashMap<String, Object>();
		Map<String, Object> contigResults = new HashMap<String, Object>();
		// Pull out information for each contig
		for (int i=0; i < contigs.length; i++) {
			//Store fraction info
			contigResults.put("fraction", fractions[i]);
			//Store count info
			contigResults.put("count", counts[i]);
			//Store fraction/count map into final result
			results.put(contigs[i], contigResults);	
		}
		return results;
	}
	
	
	public static long countLines(String filename) throws IOException {
	    InputStream is = new BufferedInputStream(new FileInputStream(filename));
	    try {
	        byte[] c = new byte[1024];
	        long count = 0;
	        long readChars = 0;
	        boolean empty = true;
	        while ((readChars = is.read(c)) != -1) {
	            empty = false;
	            for (int i = 0; i < readChars; ++i) {
	                if (c[i] == '\n') {
	                    ++count;
	                }
	            }
	        }
	        return (count == 0 && !empty) ? 1 : count;
	    } finally {
	        is.close();
	    }
	}
	
	protected String executeCommandOutputToString(final String command) throws OperationFailedException {
		Runtime r = Runtime.getRuntime();
		final Process p;

		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			
			p = r.exec(command);
			
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

			return outputStream.toString();
		}
		catch (IOException e1) {
			throw new OperationFailedException("Task encountered an IO exception : " + System.err.toString() + "\n" + e1.getLocalizedMessage(), this);
		}
	}	
	
}