package buffer;

import java.io.File;
import java.util.logging.Logger;

import pipeline.Pipeline;

public class FileTypeGuesser {

	public static FileBuffer GuessFileType(File file) {
		if (file.getName().endsWith("bam")) {
			return new BAMFile(file);
		}
		if (file.getName().endsWith("sam")) {
			return new SAMFile(file);
		}
		if (file.getName().endsWith(".bed")) {
			return new BEDFile(file);
		}
		if (file.getName().endsWith(".csv")) {
			return new CSVFile(file);
		}
		if (file.getName().endsWith(".fastq") || file.getName().endsWith(".fq") || file.getName().endsWith("fastq.gz") || file.getName().endsWith(".fq.gz")) {
			return new FastQFile(file);
		}
		if (file.getName().endsWith("fas") || file.getName().endsWith("fasta") || file.getName().endsWith("fa")) {
			return new FastaBuffer(file);
		}
		if (file.getName().endsWith("sai")) {
			return new SAIFile(file);
		}
		if (file.getName().endsWith("txt")) {
			return new TextBuffer(file);
		}
		if (file.getName().endsWith("vcf")) {
			return new VCFFile(file);
		}
		if (file.getName().contains("fastq") ) {
			return new FastQFile(file);
		}
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.warning("Could not find file type associated with file : " + file.getAbsolutePath() + " guesser is returning UnknownFileType");
		return new UnknownFileBuffer(file);
	}
}
