package operator.variant;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import util.VCFLineParser;
import buffer.BEDFile;
import buffer.IntervalsFile;
import buffer.VCFFile;

/**
 * Filters a vcf file and removes all sites not in the given BED file, writing the results to a new file
 * @author brendan
 *
 */
public class FilterVCFByBED extends IOOperator {


	@Override
	public void performOperation() throws OperationFailedException {
		BEDFile bedFile = (BEDFile) getInputBufferForClass(BEDFile.class);		
		VCFFile inVCF = (VCFFile) getInputBufferForClass(VCFFile.class);
		VCFFile outVCF = (VCFFile) getOutputBufferForClass(VCFFile.class);
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Filtering input vcf " + inVCF.getFilename() + " by bed file: " + bedFile.getFilename());
		
		if (! bedFile.isMapCreated())
			try {
				bedFile.buildIntervalsMap();
			} catch (IOException e) {
				throw new OperationFailedException("Could not create intervals map for bed file: " + bedFile.getAbsolutePath() + "\n" + e.getMessage(), this);
				
			}
		

		try {
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(outVCF.getFile()));			
			int varsRetained = doFilter(inVCF, bedFile, writer);
			
			logger.info("Done filtering input vcf " + inVCF.getFilename() + " resulting file has " + varsRetained + " variants") ;
			
		} catch (IOException e) {
			throw new OperationFailedException("Could not read / write vcf file for filtration\n" + e.getMessage(), this);
		}
		
	}
	
	
	/**
	 * Read the VCF file line by line and write only header lines or those lines contanining
	 * variants that fall into the given intervals file to the writer. Output is in VCF format
	 * @param inFile
	 * @param bedFile
	 * @param writer
	 * @throws IOException
	 */
	public static int doFilter(VCFFile inFile, IntervalsFile bedFile, Writer writer) throws IOException {
		if (! bedFile.isMapCreated()) {
			try {
				bedFile.buildIntervalsMap();
			} catch (IOException e) {
				throw new IllegalArgumentException("Could not create intervals map for bed file: " + bedFile.getAbsolutePath() + "\n" + e.getMessage());
				
			}
		}
		
		
		BufferedReader reader = new BufferedReader(new FileReader(inFile.getFile()));
		String line = reader.readLine();
		while (line != null && line.startsWith("#")) {
			writer.write(line + "\n");
			line = reader.readLine();
		}

		VCFLineParser vParser = new VCFLineParser(inFile.getFile());
		int totVars = 0;
		int varsFound = 0;
//		int varsNotFound = 0;
//		Map<String, Integer> counts = new HashMap<String, Integer>();
		while( vParser.advanceLine()) {
			totVars++;
			String contig = vParser.getContig().replace("chr", "")	;
			int pos = vParser.getPosition();
			if (bedFile.contains(contig, pos)) {
				varsFound++;
				writer.write(vParser.getCurrentLine() + "\n");
			}
		}
		
		reader.close();
		return varsFound;
	}

}
