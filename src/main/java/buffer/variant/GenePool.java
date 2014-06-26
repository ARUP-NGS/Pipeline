package buffer.variant;

import gene.Gene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.Operator;
import operator.variant.VariantPoolWriter;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;

/**
 * A GenePool contains a bunch of variant records but organizes them by
 * gene, not genomic position. Generally speaking, variants without a GENE_NAME annotation
 * are ignored. 
 * @author brendan
 *
 */
public class GenePool extends Operator {

	//Map is keyed by gene and values are a (possibly empty, but non-null) list of variant records
	Map<String, List<VariantRec>> pool = new HashMap<String, List<VariantRec>>();
	
	public GenePool() {
		//Create an empty pool
	}
	
	/**
	 * Create a GenePool using the variant pool given. Variants without the annotation
	 * GENE_NAME are ignored
	 */
	public GenePool(VariantPool variants) {
		int count = 0;
		for(String contig : variants.getContigs()) {
			for(VariantRec rec : variants.getVariantsForContig(contig)) {
				boolean added = addRecordNoWarn(rec);
				if (added)
					count++;
			}
		}
		//System.out.println("Added " + count + " records to gene pool");
	}
	
	public GenePool(List<VariantRec> variants) {
		int count = 0;
		for(VariantRec rec : variants) {
			boolean added = addRecordNoWarn(rec);
				if (added)
					count++;
		}
		//System.out.println("Added " + count + " records to gene pool");
	}
	
	/**
	 * Reads the given file and adds the gene names found to this pool.
	 * @param inputFile
	 * @throws IOException 
	 */
	public GenePool(File inputFile) throws IOException {
		readGenesFromFile(inputFile);
	}
	
	/**
	 * Read in all genes in the given text file. We assume there's one gene name per line and ignore
	 * blank lines and lines starting with #
	 * 
	 * @param inputFile
	 * @throws IOException 
	 */
	public void readGenesFromFile(File inputFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		String line = reader.readLine();
		while (line != null) {
			if (line.startsWith("#")) {
				line = reader.readLine();
				continue;
			}
			String[] toks = line.split("\t");
			String geneName = toks[0].trim();
			if (geneName.length()>0) {
				//System.out.println("Adding gene " + geneName);
				addGene(geneName);
			}
			line = reader.readLine();
		}
		reader.close();
	}

	/**
	 * Get the list of variants for a given gene name. May return an empty or 
	 * NULL list 
	 * @param geneName
	 * @return
	 */
	public List<VariantRec> getVariantsForGene(String geneName) {
		return pool.get(geneName);
	}
	
	/**
	 * Return a collection containing all gene names used as keys in this pool
	 * @return
	 */
	public Collection<String> getGenes() {
		return pool.keySet();
	}
	
	/**
	 * Returns true if there is an entry for the given gene in this gene pool (even
	 * if the list of variants associated with the gene is empty)
	 * @param geneName
	 * @return
	 */
	public boolean containsGene(String geneName) {
		return pool.containsKey(geneName);
	}
	
	public void removeVariant(VariantRec variant) {
		String geneName = variant.getAnnotation(VariantRec.GENE_NAME);
		if (geneName == null)
			throw new IllegalArgumentException("Variant does not have a GENE_NAME");
		List<VariantRec> list = pool.get(geneName);
		boolean removed = list.remove(variant);
		if (!removed) {
			throw new IllegalArgumentException("Variant was not in pool");
		}
	}
	
	/**
	 * Add a new single gene to the pool, with no variants associated. 
	 * Has no effect if there is already a list of variants associated with the gene
	 * @param geneName
	 */
	public void addGene(String geneName) {
		List<VariantRec> vars = pool.get(geneName);
		if (vars == null) {
			vars = new ArrayList<VariantRec>();
			pool.put(geneName, vars);
		}
	}
	
	/**
	 * Add all variants in the given pool to this gene pool, but don't warn if some variants
	 * aren't in a gene. Returns total number of variants successfully added
	 * @param pool
	 * @return
	 */
	public int addPool(VariantPool pool) {
		int count = 0;
		for(String contig : pool.getContigs()) {
			for(VariantRec var : pool.getVariantsForContig(contig)) {
				boolean added = addRecordNoWarn(var);
				if (added)
					count++;
			}
		}
		return count;
	}
	
	/**
	 * Returns the number of genes in this gene pool
	 * @return
	 */
	public int size() {
		return pool.size();
	}
	
	/**
	 * Attempt to add the given record to this pool, but don't warn
	 * if the record does not contain the GENE_NAME annotation
	 * returns true if the record was added.
	 * @param rec
	 */
	public boolean addRecordNoWarn(VariantRec rec) {		
		String geneName = rec.getAnnotation(VariantRec.GENE_NAME);
		
		//If gene name is null, try looking at the gene object
		if (geneName == null) {
			Gene g = rec.getGene();
			if (g != null)
				geneName = g.getName();
		}
		
		if (geneName != null) {
			List<VariantRec> vars = pool.get(geneName);
			if (vars == null) {
				vars = new ArrayList<VariantRec>();
				pool.put(geneName, vars);
			}
			vars.add(rec);
			return true;
		}
		return false;
	}
	
	/**
	 * Counts the number of UNIQUE 'sources' (from VariantRec.SOURCE) found among
	 * all variants associated with the given gene
	 * @param gene
	 * @return
	 */
	public int countSources(String gene) {
		List<VariantRec> vars = pool.get(gene);
		Set<String> sources = new HashSet<String>();
		for(VariantRec var : vars) {
			sources.add(var.getAnnotation(VariantRec.SOURCE));
		}
		
		if (vars.size() > 2*sources.size()) {
			return 0;
		}
		
		return sources.size();
	}
	
	
	public void listAll(PrintStream out) {
		for(String key : pool.keySet()) {
			List<VariantRec> vars = pool.get(key);
			int sourceCount =  countSources(key);
			out.println("GENE:" + key + " : " + vars.size() + "\t" + sourceCount);
			for(VariantRec var : vars) {
				out.println("\t" + var.getAnnotation(VariantRec.SOURCE) + "\t " + var.toSimpleString() + "\t" + var.getAnnotation(VariantRec.EXON_FUNCTION) + "\t" + var.getPropertyOrAnnotation(VariantRec.POP_FREQUENCY) + "\t" + var.getAnnotation(VariantRec.CDOT) + "\t" + var.getAnnotation(VariantRec.PDOT));
			}

			out.println();
		}
	}

	public void listGenesWithMultipleVars(PrintStream out, int cutoff, VariantPoolWriter writer) {
		List<List<VariantRec>> geneList = new ArrayList<List<VariantRec>>();
		writer.writeHeader(out);
		
		for(String key : pool.keySet()) {
			List<VariantRec> vars = pool.get(key);
			int sourceCount =  countSources(key);
			if (sourceCount >= cutoff ) {
				List<VariantRec> varsList = new ArrayList<VariantRec>();
				//out.println("GENE:" + key + " : " + vars.size() + "\t" + sourceCount + "\t" + computeMeanProd(vars));
				for(VariantRec var : vars) {
					varsList.add(var);
					//out.println(var.getAnnotation(VariantRec.SOURCE) + "\t" + var.toSimpleString() + "\t" + var.getPropertyString(annoKeys));
				}
				geneList.add(varsList);
			}
		}		
		
		Collections.sort(geneList, new MeanEffectComparator());
		for(List<VariantRec> vars : geneList) {
			Collections.sort(vars, VariantRec.getPositionComparator());
			String key = vars.get(0).getAnnotation(VariantRec.GENE_NAME);
			out.print(key + "\t Mean Effect:" +  computeMeanProd(vars) + "\n");
			for(VariantRec var : vars) {
				out.print(var.getAnnotation(VariantRec.SOURCE) + "\t");
				writer.writeVariant(var, out);
			}
		
		}
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		if (geneListFile == null) 
			return ; // Just make an empty pool- should actually never happen since we demand that filename be specified
		

		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Building gene pool from variants in file " + geneListFile.getAbsolutePath());
		
		try {
			readGenesFromFile(geneListFile);
		} catch (IOException e) {
			
			e.printStackTrace();
			throw new OperationFailedException("Could not read genes in file : " + geneListFile.getAbsolutePath(), this);
		}
				
		logger.info("Created gene pool with " + this.size() + " total genes");
	}

	@Override
	public void initialize(NodeList children) {
		//We just look to see if there was a filename specified as an attribute. We will then read the
		//file in performOperation()
		String inputPath = properties.get(FILENAME);
		if (inputPath == null)
			throw new IllegalArgumentException("No filename specified to create gene list");
		
		File file = new File(inputPath);
		if (! file.exists()) {
			throw new IllegalArgumentException("Gene list input file : " + file.getAbsolutePath() + " does not exist");
		
		}
	}
	
	/**
	 * Computes the mean go_effect_product of the variants in the list
	 * @param vars
	 * @return
	 */
	public static double computeMeanProd(List<VariantRec> vars) {
		double sum = 0;
		for(VariantRec rec : vars) {
			
			//Uncomment me for normal, sort-by-variant-effect behavior
			//Double prod = rec.getProperty(VariantRec.GO_EFFECT_PROD);
			
			//Hack for compatibility with old, variant-only annotation scheme
			Double prod = rec.getProperty(Gene.GENE_RELEVANCE);
			if (prod != null) {
				if (prod >= 0) //Treat negative vals as equal to zero so means dont get very skewed by some low-effect vars
					sum += prod;
				else
					sum += 0;

			}
		}
		return sum / vars.size();
	}
	
	class MeanEffectComparator implements Comparator<List<VariantRec>> {

		@Override
		public int compare(List<VariantRec> arg0, List<VariantRec> arg1) {
			double m0 = computeMeanProd(arg0);
			double m1 = computeMeanProd(arg1);
			if (m1 == m0)
				return 0;

			if (m0 < m1)
				return 1;
			else
				return -1;

		}
		
	}
	
	public static final String FILENAME = "filename";
	private File geneListFile = null;
}

