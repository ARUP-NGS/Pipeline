package operator.writer;

import gene.Gene;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.CSVFile;
import buffer.GeneList;

/**
 * Like a VariantPoolWriter, but writes information about genes not individual variants.
 * @author brendan
 *
 */
public class GeneWriter extends Operator {

	CSVFile outputFile = null;
	GeneList genes = null;
	Comparator<Gene> sorter = new RelevanceSorter();
	
	String[] keys = new String[]{Gene.GENE_RELEVANCE, 
								 Gene.DBNSFPGENE_SCORE,
								 Gene.OMIM_PHENOTYPE_SCORE,
								 Gene.EXPRESSION_SCORE,
								 Gene.GO_SCORE,
								 Gene.INTERACTION_SCORE};
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("GeneWriter attempting to write variants to file");
		
		List<Gene> allGenes = genes.getAllGenes();
		Collections.sort(allGenes, sorter);
		
		try {
			PrintStream outStream = System.out;
			if (outputFile != null) {
				logger.info("GeneWriter is writing " + genes.size() + " to file : " + outputFile.getAbsolutePath());
				outStream = new PrintStream(new FileOutputStream( outputFile.getFile()));
				
			}
			
			writeHeader(outStream);
			
			for(Gene gene : allGenes) {
				writeGene(gene, outStream);
			}
		}
		catch (IOException ex) {
			throw new OperationFailedException("Error writing GeneList to file: " + ex.getMessage(), this);
		}

		
	}

	private void writeHeader(PrintStream out) {
		out.print("gene");
		for(int i=0; i<keys.length; i++) {
			out.print("\t" + keys[i]);
		}
		out.println();
	}

	protected void writeGene(Gene gene, PrintStream out) {
		out.print(gene.getName());
		for(int i=0; i<keys.length; i++) {
			out.print("\t" + gene.getPropertyOrAnnotation(keys[i]));
		}
		out.println();
	}

	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());

				if (obj instanceof CSVFile) {
					outputFile = (CSVFile)obj;
				}

				if (obj instanceof GeneList) {
					genes = (GeneList)obj;
				}
			}
		}
		
		if (genes == null) {
			throw new IllegalArgumentException("No GeneList specified for GeneWriter " + getObjectLabel());
		}
	}


	class RelevanceSorter implements Comparator<Gene> {

		@Override
		public int compare(Gene g0, Gene g1) {
			Double r1 = g0.getProperty(Gene.GENE_RELEVANCE);
			Double r2 = g1.getProperty(Gene.GENE_RELEVANCE);
			
			if (r1 == null)
				r1 = 0.0;
			if (r2 == null) {
				r2 = 0.0;
			}
			
			return r1.compareTo(r2);
		}
		
	}

}
