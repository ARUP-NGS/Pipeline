package operator.oncology;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.lang.IllegalArgumentException;

import operator.IOOperator;
import operator.OperationFailedException;
import operator.StringPipeHandler;
import pipeline.Pipeline;
import util.FastaReader;
import util.bamUtil.ReadCounter;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.FastQFile;
import buffer.FastaBuffer;
import buffer.FileBuffer;
import buffer.ReferenceFile;

/*
 * @author daniel
 * Counts the number of records for fastq or sam files. 
 * Contains countLines from StackOverflow question 453018
 * 
*/
public class OncologyUtils extends IOOperator {
		
	public static final String SAMTOOLS_PATH = "samtools.path";
	public static String defaultSamPath = "samtools";
	List<FileBuffer> FastqBuffers = this.getAllInputBuffersForClass(FastQFile.class); // Should contain 4 files
	List<FileBuffer> BamBuffers = this.getAllInputBuffersForClass(BAMFile.class);
	List<FileBuffer> CustomRefBuffers = this.getAllInputBuffersForClass(FastaBuffer.class);

	@Override
	public void performOperation() throws OperationFailedException {
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Beginning utilities: Checking Arguments");
		System.out.println("Beginning utilities: Checking Arguments.");
		
		if(FastqBuffers.size() != 4) {
			throw new IllegalArgumentException("4 Fastq files required as input.");
		}
	
		
		if(BamBuffers.size() != 4) {
			throw new IllegalArgumentException("4 BAM files required as input.");
		}
		
		
		if(CustomRefBuffers.size() != 2) {
			throw new IllegalArgumentException("2 Reference files required as input.");
		}
		
		logger.info("Counting reads in Fastq Files");
		System.out.println("Counting reads in Fastq Files");
		long InFq = -1337;
		//TODO: check for the fastq file not having a numebre of lines divisible by 4
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
			Trim90Fq = countLines(FastqBuffers.get(2).getAbsolutePath())/4;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		/*
		 *  2. Count records for all 4 bam files
		 */
		logger.info("Counting records in BAM Files");
		System.out.println("Counting records in BAM Files");
		/*
		 *  From here until "3.", this is a quick fix around the lack of a countReadsByChromosome function. 
		 */
		String samtoolsPath = defaultSamPath;
		String samtoolsAttr = getPipelineProperty(SAMTOOLS_PATH);
		if(samtoolsAttr != null) {
			samtoolsPath = samtoolsAttr;
		}
		
		String command_str = samtoolsPath + " view -c " + BamBuffers.get(0).getAbsolutePath();
		long ratioMapped = Integer.parseInt(executeCommandOutputToString(command_str));
		String command_str1 = samtoolsPath + " view -c " + BamBuffers.get(1).getAbsolutePath();
		long ratioUnmapped = Integer.parseInt(executeCommandOutputToString(command_str1));
		String command_str2 = samtoolsPath + " view -c " + BamBuffers.get(2).getAbsolutePath();
		long fusionMapped = Integer.parseInt(executeCommandOutputToString(command_str2));
		String command_str3 = samtoolsPath + " view -c " + BamBuffers.get(3).getAbsolutePath();
		long fusionUnmapped = Integer.parseInt(executeCommandOutputToString(command_str3));
		long short40 = InFq - Trim40Fq;
		long short90 = UnmappedFq - Trim90Fq; 
		//Get map containing # of reads per contig
		Map<String, Long> ratioMap = ReadCounter.countReadsByChromosome((BAMFile)BamBuffers.get(0),1);
		Map<String, Long> fusionMap = ReadCounter.countReadsByChromosome((BAMFile)BamBuffers.get(1),1);
		/*
		 * 3. Get list of "chromosomes"
		 */
		FastaReader FusionRef = null;
		String[] FusionContigs = new String[0];
		try {
			FusionRef = new FastaReader(CustomRefBuffers.get(0).getFile());
			FusionContigs = FusionRef.getContigs();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		int fusionLength = FusionContigs.length;
		FastaReader RatioRef = null;
		String[] RatioContigs = new String[0];//TODO: Make size of the RatioContigs array so that I can use that for the next loop
		try {
			RatioRef = new FastaReader(CustomRefBuffers.get(1).getFile());
			RatioContigs = RatioRef.getContigs();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		int ratioLength = RatioContigs.length;
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
		 * 
		 */
		return;
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