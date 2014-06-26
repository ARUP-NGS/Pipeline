package operator.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import operator.IOOperator;
import operator.OperationFailedException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.FileBuffer;

import pipeline.PipelineObject;

public class TestOperator extends IOOperator {

	protected File inputFile = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		if (verbose) {
			System.out.println("TestOperator is performing operation");
		}

		inputFile = inputBuffers.get(0).getFile();
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			FileBuffer output = outputBuffers.get(0);
			
			String line = reader.readLine();
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(output.getFile()));
			writer.write("Top of the new output buffer!\n");
			
			int count = 0;
			while (line != null) {
				System.out.println("Read this line from input file : " + line);
				writer.write(count + "\t " + line + "\n");
				count++;
				line = reader.readLine();
			}
			
			reader.close();
			writer.close();
		} catch (FileNotFoundException e) {
			throw new OperationFailedException(e.getCause() + "\t" + e.getMessage(), this);
		} catch (IOException e) {
			throw new OperationFailedException(e.getCause() + "\t" + e.getMessage(), this);
		}
	}



	

	
}
