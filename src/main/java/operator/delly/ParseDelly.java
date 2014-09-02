package operator.delly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import json.JSONException;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.FileBuffer;

public class ParseDelly extends IOOperator {

	@Override
	public void performOperation() throws OperationFailedException,
			JSONException, IOException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		Boolean TranslocationDetected = false;
		File inputVCF = inputBuffers.get(0).getFile();
		PrintWriter out = new PrintWriter(this.getOutputBufferForClass(
				FileBuffer.class).getAbsolutePath());
		logger.info("Now parsing Delly VCF to check for any records passing the quality threshold.");
		try (BufferedReader br = new BufferedReader(new FileReader(inputVCF))) {
			for (String line; (line = br.readLine()) != null;) {
				String[] Values = line.split("\t");
				boolean writeLine = false;
				try {
					String QualValue = Values[6];
					if (QualValue.equals("PASS")) {
						writeLine = true;
						TranslocationDetected = true;
						logger.info("Translocation detected! See "
								+ this.getOutputBufferForClass(FileBuffer.class)
										.getAbsolutePath() + " for details.");
					}
				} 
				catch (IndexOutOfBoundsException e) {
					continue;
				}
				if(writeLine==true)
					out.write(line);
			}
		}
		catch(Exception e) {
			out.close();
			logger.info("Buffered Reader encountered an error " + e.getLocalizedMessage() + " " + e.getMessage());
		}
		out.close();
		logger.info("Delly parsing complete. Translocation detected: "
				+ TranslocationDetected.toString() + '.');
		System.out.println("Delly parsing complete. Translocation detected: "
				+ TranslocationDetected.toString() + '.');
		return;
	}
}