package operator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import buffer.FileBuffer;

/**
 * This is a very basic operator that just counts the number of lines in an input file.
 * All of the business with finding the right input file is handled automatically, and 
 * all input files will be in a list (owned by the superclass IOOperator) called inputBuffers 
 * @author brendan
 *
 */
public class LineCounter extends IOOperator {

	/**
	 * The performOperation method is called when this operator is supposed to do its job. 
	 * Here, we examine an input file, count the number of lines, and write the result
	 * to an output file. 
	 */
	@Override
	public void performOperation() throws OperationFailedException {
		
		//First we need to get the input file. All input files are in a list
		//called 'inputBuffers', which is defined in the IOOperator super class
		FileBuffer myInputFileBuffer = inputBuffers.get(0); //Obtain a reference to the first input file buffer
		File myInputFile = myInputFileBuffer.getFile(); //A reference to the actual java file in question
		
		//We also want a reference to the output file.. this is done almost exactly the same way
		FileBuffer outputBuffer = outputBuffers.get(0);
		File outputFile = outputBuffer.getFile(); 
		
		
		//We'd like to count the lines in the input file...
		try {
			int lineCount = 0;
			BufferedReader reader = new BufferedReader(new FileReader(myInputFile));
			String line = reader.readLine();
			while(line != null) {
				lineCount++;
				line = reader.readLine();
			}
			
			//The number of lines is now in lineCount, we want to write it to the output file
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			writer.write("Example output data here!");
			writer.write("Line count is : " + lineCount);
			writer.close();
			
			
		} catch (FileNotFoundException e) {
			//All exceptions should be caught and re-thrown an OperationFailedExceptions, which are
			//all handled similarly
			throw new OperationFailedException("The input file " + myInputFile.getAbsolutePath() + " does not exist!", this);
		} catch (IOException e) {
			throw new OperationFailedException("The input file " + myInputFile.getAbsolutePath() + " could not be read!", this);
		}
		
		
	}

}
