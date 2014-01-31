package operator.writer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;

import json.JSONException;
import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import util.JSONVarsGenerator;
import buffer.FileBuffer;
import buffer.variant.VariantPool;

public class AnnotatedJsonGZWriter extends Operator {

	private VariantPool variants = null;
	private FileBuffer outputFile = null;

	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Writing annotated.json.gz vars attempting file");
		if (variants  == null) {
			logger.severe("No variant pool found, could not write variants");
			throw new OperationFailedException("Variant pool not specified", this);
		}
		
		try {
			PrintStream outStream = System.out;
			if (outputFile  != null) {
				logger.info("VariantWriter is writing to file : " + outputFile.getAbsolutePath());
			}
			
			JSONVarsGenerator.createJSONVariants(variants, outputFile.getFile());
			
			
					
			logger.info("Exported " + variants.size() + " variants in json.gz form to " + outputFile.getAbsolutePath());
			outStream.close();
		} catch (FileNotFoundException e) {
			throw new OperationFailedException("Could not write to file : " + outputFile.getFile().getAbsolutePath(), this);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new OperationFailedException("Could not export variants, json exception: " + e.getLocalizedMessage(), this);
		} catch (IOException e) {			
			e.printStackTrace();
			throw new OperationFailedException("Could not write to file : " + outputFile.getFile().getAbsolutePath(), this);
		}	
	}
	
	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof VariantPool) {
					if (variants == null)
						variants = (VariantPool)obj;
					
				}
				if (obj instanceof FileBuffer) {
					outputFile = (FileBuffer)obj;
				}

			}
		}
		
		if (outputFile == null) {
			throw new IllegalArgumentException("Output file file not specified");
		}

		if (variants == null) {
			throw new IllegalArgumentException("Variant pool not specified");
		}
		
	}

}
