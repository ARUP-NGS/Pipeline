package operator.annovar;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.AnnovarInputFile;
import buffer.CSVFile;
import buffer.variant.CSVLineReader;
import buffer.variant.SimpleLineReader;
import buffer.variant.VariantRec;

public class ConvertCSVAnnovar extends IOOperator {

	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		
		CSVFile input = (CSVFile) getInputBufferForClass(CSVFile.class);
		if (input == null)
			throw new OperationFailedException("No input CSV file specified", this);
		logger.info("Converting csv file " + input.getAbsolutePath() + " to annovar ");
		
		AnnovarInputFile output = (AnnovarInputFile) getOutputBufferForClass(AnnovarInputFile.class);
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(output.getAbsolutePath()));
			//CSVLineReader csvReader = new CSVLineReader(input.getFile());
			CSVLineReader csvReader = new SimpleLineReader(input.getFile());
			do {
				VariantRec rec = csvReader.toVariantRec();
				String het = "het";
				if (!rec.isHetero())
					het = "hom";
				writer.write(rec.getContig() + "\t" + 
							 rec.getStart() + "\t" + 
							 (rec.getStart() + rec.getRef().length()-1) + "\t" + 
							 rec.getRef() + "\t" +
							 //"- \t" +
							 rec.getAlt() + "\t" +
							 //"A \t" +
							 rec.getQuality() + "\t" + 
							 rec.getPropertyOrAnnotation(VariantRec.DEPTH) + "\t" + 
							 het + "\t" +
							 rec.getProperty(VariantRec.GENOTYPE_QUALITY) + "\n");
				
			} while(csvReader.advanceLine());
			writer.close();
		} catch (IOException e) {
			logger.severe("Error converting csv to annovar input: " + e.getMessage());
			e.printStackTrace();
		}
		
	}

}
