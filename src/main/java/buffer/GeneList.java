package buffer;

import gene.Gene;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;

public class GeneList extends Operator {
	
	private Map<String, Gene> genes = new HashMap<String, Gene>();
	TextBuffer geneListFile = null;
	
	/**
	 * Create an empty gene list
	 */
	public GeneList() {
		
	}
	
	/**
	 * Create a gene list from the given list of genes
	 * @param genes
	 */
	public GeneList(List<Gene> genes) {
		genes.addAll(genes);
	}
	
	/**
	 * Obtain all genes in this list as a list
	 * @return
	 */
	public List<Gene> getAllGenes() {
		List<Gene> all = new ArrayList<Gene>();
		for(String name : genes.keySet()) {
			all.add( genes.get(name) );
		}
		return all;
	}
	
	/**
	 * Create a new record associated with the given name.
	 * Throws an exception if the list already contains a record for the given name
	 * @param name
	 * @return
	 */
	public void addGene(String name) {
		if (genes.containsKey(name)) {
			throw new IllegalArgumentException("Gene list already contains entry for " + name);
		}
		Gene g = new Gene(name);
		genes.put(g.getName(), g);
	}
	
	
	
	/**
	 * True if any of the genes in this list has the given name
	 * @param geneName
	 * @return
	 */
	public boolean containsGene(String geneName) {
		return genes.containsKey(geneName);
	}
	
	/**
	 * Obtain the gene with the given name, or null if there is no such gene
	 * @param geneName
	 * @return
	 */
	public Gene getGeneByName(String geneName) {
		return genes.get(geneName);
	}
	
	public Collection<String> getGeneNames() {
		return genes.keySet();
	}
	
	/**
	 * The number of genes in the list
	 * @return
	 */
	public int size() {
		return genes.size();
	}

	@Override
	public void performOperation() throws OperationFailedException {
		
		if (geneListFile != null) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(geneListFile.getAbsolutePath()));
				
				String line = reader.readLine();
				while(line != null) {
					if (!line.startsWith("#") && line.trim().length()>1) {
						addGene(line.trim());
					}
					line = reader.readLine();
				}
				
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				throw new OperationFailedException("Could not open gene list input file", this);
			}
		}
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("Read in " + this.size() + " genes from file " + geneListFile.getFilename());
	}

	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof TextBuffer) {
					geneListFile = (TextBuffer)obj;
				}
			}
		}
	}
	


}
