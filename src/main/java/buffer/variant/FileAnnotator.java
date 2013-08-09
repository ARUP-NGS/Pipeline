package buffer.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;

public class FileAnnotator extends PipelineObject {

	protected File inputFile;
	protected String label;
	protected int column;
	protected int refColumn;
	protected int altColumn;
	protected VariantPool variants;
	protected Map<String, String> properties = new HashMap<String, String>();
	
	public FileAnnotator(File inputFile, String propertyLabel, int column, int refColumn, int altColumn, VariantPool pool) {
		this.inputFile = inputFile;
		this.column = column;
		this.label = propertyLabel;
		this.variants = pool;
		this.refColumn = refColumn;
		this.altColumn = altColumn;
	}
	
	
	public void annotateAll() throws IOException {
		annotateAll(true);
	}
	
	/**
	 * Read all lines from the input file, parse the value at the column specified in the constructor,
	 * and add that PROPERTY to the VariantRecord at the given position in the variant pool
	 * @throws IOException 
	 */
	public void annotateAll(boolean isProperty) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		String line = reader.readLine();
		int totalVars = 0;
		int errorVars = 0;
		
		List<String> lastFewErrors = new ArrayList<String>(); //Stores info about variants not found in annotation file
		
		while(line != null) {
			if (line.length()>0) {
				String[] toks = line.split("\\t");
				totalVars++;
				
				String contig = toks[2];
				int pos = Integer.parseInt( toks[3] );
				String ref = toks[refColumn];
				String alt = toks[altColumn];
				
				VariantRec rec = findVariant(contig, pos, ref, alt); // findRecord(contig, pos);
				if (rec != null) {
					if (isProperty) {
						double score = Double.parseDouble(toks[column]);
						rec.addProperty(label, score);
					}
					else {
						rec.addAnnotation(label, toks[column]);
					}
				}
				else {
					errorVars++;
					if (lastFewErrors.size() < 10)
						lastFewErrors.add("Variant not found : " + line);
				}
			}
			line = reader.readLine();
		}
		
		if (errorVars > 0)
			Logger.getLogger(Pipeline.primaryLoggerName).info(errorVars + " of " + totalVars + " could not be associated with a variant record");
		
		if (totalVars > 100 && (errorVars > totalVars*0.04)) {
			for(String err : lastFewErrors) {
				System.err.println(err);
			}
			reader.close();
			throw new IllegalArgumentException("Too many variants not found for file annotation, " + errorVars + " of " + totalVars + " total variants");
		}
		
		reader.close();
	}

	@Override
	public void setAttribute(String key, String value) {
		properties.put(key, value);
	}
	
	public String getAttribute(String key) {
		return properties.get(key);
	}

	public Collection<String> getAttributeKeys() {
		return properties.keySet();
	}
	
	@Override
	public void initialize(NodeList children) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * There have been consistent issues with matching variants found in annovar output files to
	 * those in a variant pool. This is partly because annovar seems to be inconsistent in how it represents
	 * insertions and deletions. For instance, sometimes a variant in a vcf that looks like 1 1025  T  TA
	 * will be converted to an annovar input record of 1  1026 - A, and sometimes 1  1025 - A. Thus, to 
	 * find variants we now look at both 1026 and 1025 (in the variant pool), to see if there's an alt 
	 * allele at either one that matches the one from the file.  
	 * @param contig
	 * @param pos
	 * @param ref
	 * @param alt
	 * @return
	 */
	protected VariantRec findVariant(String contig, int pos, String ref, String alt) {
		VariantRec rec = variants.findRecord(contig, pos);
		if (rec != null)
			return rec;

		//System.out.println("Variant at contig " + contig + " pos: " + pos + " not found, searching for close variants..");
		if (ref.equals("-")) {
			int modPos = pos+1;

			rec = variants.findRecord(contig, modPos);

			if (rec == null) {
				System.out.println("Could not find record for variant at " + contig + ":" + pos);
				return null;
			}
			
			if (! rec.getAlt().equals(alt)) {
				System.out.println("Record found, but alt for record is " + rec.getAlt() + " and alt from file is " + alt);
				return null;
			}

		}
		return rec;
	}
	
}
