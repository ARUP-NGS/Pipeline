package operator.examples;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.TextBuffer;

public class Example1 extends Operator {

	//This gets set in the initialize(...) method
	protected TextBuffer inputTextFile = null;
	
	@Override
	/**
	 * After initialization is performed for all PipelineObjects, this method gets called on any 
	 * PipelineObject that happens to be an Operator. 
	 */
	public void performOperation() throws OperationFailedException {
	
		//Now we can do something with the input text file, like list its contents
		
		try {
			//Get a reader for the file
			BufferedReader reader = new BufferedReader(new FileReader( inputTextFile.getFile() ));
			
			//Go through each line of the file and write it to System.out
			String line = reader.readLine();
			while(line != null) {
				System.out.println(line);
				line = reader.readLine();
			}
			
			//Don't forget to close the buffer when you're done
			reader.close();
		} catch (FileNotFoundException e) {
			//Exceptions should be wrapped as OperationFailedExceptions for better output handling
			throw new OperationFailedException("Could not find file!", this);
			
		} catch (IOException e) {
			//Exceptions should be wrapped as OperationFailedExceptions for better output handling
			throw new OperationFailedException("IOException: " + e.getLocalizedMessage(), this);
			
		}
		
	}

	@Override
	/**
	 * This method gets called for every PipelineObject before any operations are performed.
	 * The NodeList argument contains a list of all of the XML objects ('nodes').  
	 */
	public void initialize(NodeList children) {

		//This code snippet shows how to get the PipelineObject associated with the xml node
		//that was passed in. It iterators over
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			
			//Only 'element' nodes can be converted to PipelineObjects
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				
				//Now I can do something with the obj - it might be a FileBuffer or a VariantPool or anything
				//that extends from PipelineObject. Check to see if it's a TextBuffer. If it is then 
				//set the 'inputTextFile' variable to it so we can use it in performOperation() ...
				
				if (obj instanceof TextBuffer) {
					inputTextFile = (TextBuffer) obj;
				}
				
			}
		}
		
		//A bit of error checking isn't a bad idea. If we didn't find a text buffer, then warn the user
		if (inputTextFile == null) {
			throw new IllegalArgumentException("No TextBuffer object found!");			
		}
	}

}
