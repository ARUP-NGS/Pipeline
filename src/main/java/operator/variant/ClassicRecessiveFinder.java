package operator.variant;

import gene.Gene;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import ncbi.GeneInfoDB;
import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.CSVFile;
import buffer.GeneList;
import buffer.variant.VarFilterUtils;
import buffer.variant.VariantFilter;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * Operator that takes as input variant pools from an affected child and two unaffected parents and identifies 
 * any variant present as a homozygote in the offspring and as hets in both parents. These are then written
 * to an output file using a MedDirWriter 
 * @author brendan
 *
 */
public class ClassicRecessiveFinder extends Operator {

	private VariantPool kidPool = null;
	private VariantPool parent1Pool = null;
	private VariantPool parent2Pool = null;
	private CSVFile outputFile = null;
	private VariantPoolWriter outputFormatter = new MedDirWriter();
	
	GeneList genes = null;
	DecimalFormat formatter = new DecimalFormat("0.0###");
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);

		
		logger.info("Classic Recessive Finder : Assuming " + kidPool.getObjectLabel() + " contains offspring variants \n " + parent1Pool.getObjectLabel() + " contains parent 1 variants \n " + parent2Pool.getObjectLabel() + " contains parent 2 variants");

		
		List<VariantRec> hits = findRecessiveHomos(kidPool, parent1Pool, parent2Pool, genes);
		
//		int count = 0;
//		for(VariantRec hit : hits) {
//			Gene g = hit.getGene();
//			Double score = g.getProperty(Gene.GENE_RELEVANCE);
//			if (score != null && score > 0) {
//				count++;
//				
//			}
//		}
//		if (count == 0) {
//			logger.warning("No scores found in any classic recessive gene");
//		}
		
		//Give all gene list info to outputwriter so it can write gene-centered info
		if (genes != null)
			outputFormatter.setGenes(genes);
		
		
		PrintStream outputStream = System.out;
		if (outputFile != null) {
			try {
				outputStream = new PrintStream(new FileOutputStream(outputFile.getFile()));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new OperationFailedException("Could not open file " + outputFile.getAbsolutePath() + " for writing", this);
			}
		}
		
		outputFormatter.writeHeader(outputStream);
		
		for(VariantRec hit : hits) {
			outputFormatter.writeVariant(hit, outputStream);
		}
		
		outputStream.flush();
				
	}
	
	private static String toUserStr(Double val) {
		if (val == null || Double.isNaN(val)) {
			return "?";
		}
		return "" + val;
	}
	
	private static List<VariantRec> findRecessiveHomos(VariantPool kidVars,
			VariantPool p1Vars, VariantPool p2Vars, GeneList genes) {
		
		GeneInfoDB geneDB = GeneInfoDB.getDB();
		
		List<VariantRec> hits = new ArrayList<VariantRec>();
		
		VariantPool p1Homos = new VariantPool( p1Vars.filterPool( VarFilterUtils.getHomoFilter() ) );
		VariantPool p2Homos = new VariantPool( p2Vars.filterPool( VarFilterUtils.getHomoFilter() ) );
		
		VariantPool kidVarsFiltered = new VariantPool( kidVars.filterPool( nonSynFilter ) );
		
		kidVarsFiltered.removeVariants( p1Homos );
		kidVarsFiltered.removeVariants( p2Homos );
		
		kidVarsFiltered = new VariantPool( kidVarsFiltered.filterPool( VarFilterUtils.getHomoFilter() ) );

		
		//Do no ranking for now, just return all variants.
		hits = kidVarsFiltered.toList();
		
		
		Collections.sort(hits, new EffectPredComparator());
		
		return hits;
	}



	public void initialize(NodeList children) {	
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());

				if (obj instanceof GeneList) {
					genes = (GeneList)obj;
				}
				
				if (obj instanceof CSVFile) {
					outputFile = (CSVFile)obj;
				}
				
				if (obj instanceof VariantPool) {
					if (kidPool == null) {
						kidPool = (VariantPool) obj;
					}
					else {
						if (parent1Pool == null) {
							parent1Pool = (VariantPool) obj;
						}
						else {
							if (parent2Pool == null) {
								parent2Pool = (VariantPool) obj;
							}
						}
					}
				}

			}
		}
		
		if (kidPool == null)
			throw new IllegalArgumentException("Kid Variant pool not specified");
		if (parent1Pool == null)
			throw new IllegalArgumentException("Parent 1 Variant pool not specified");
		if (parent2Pool == null)
			throw new IllegalArgumentException("Parent 2 Variant pool not specified");

	}
	

	static class EffectPredComparator implements Comparator<VariantRec> {

		@Override
		public int compare(VariantRec arg0, VariantRec arg1) {
			Double ef1 = arg0.getProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT);
			Double ef2 = arg1.getProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT);
			
			if (ef1 == null)
				ef1 = 0.0;
			if (ef2 == null)
				ef2 = 0.0;
			
			if (ef1 == ef2)
				return 0;
			else
				return ef1 > ef2 ? -1 : 1;
						
		}	
	}
	
	
	static class GeneRelComparator implements Comparator<VariantRec> {

		@Override
		public int compare(VariantRec v1, VariantRec v2) {
			Gene g0 = v1.getGene();
			Gene g1 = v2.getGene();
			
			if (g0 == null) {
				System.err.println("No gene found for variant at : " + v1.getContig() + ":" + v1.getStart());
				return 0;
			}
			
			if (g1 == null) {
				System.err.println("No gene found for variant at : " + v2.getContig() + ":" + v2.getStart());
				return 0;
			}
			
			Double s0 = g0.getProperty(Gene.GENE_RELEVANCE);
			Double s1 = g1.getProperty(Gene.GENE_RELEVANCE);
			
			if (s0 == null) {
				System.err.println("Could not compute gene relevance score for gene " + g0.getName());
				return 0;
			}
			
			if (s1 == null) {
				System.err.println("Could not compute gene relevance score for gene " + g1.getName());
				return 0;
			}
				
			
			return s0 < s1 ? 1 : -1;
			
		}
		
	}
	
	//User to filter input variant pools for interesting variants only
		private static VariantFilter nonSynFilter = new VariantFilter() {
			public boolean passes(VariantRec var) {
				String varType = var.getAnnotation(VariantRec.VARIANT_TYPE);
				if (varType != null) {
					if (varType.contains("splic"))
						return true;
					if (varType.contains("intronic"))
						return false;
					if (varType.contains("intergenic"))
						return false;
				}
				
				String exonFunc = var.getAnnotation(VariantRec.EXON_FUNCTION);
				if (exonFunc != null) {
					if (exonFunc.contains("nonsynon"))
						return true;
					if (exonFunc.contains("delet")) {
						return true;
					}
					if (exonFunc.contains("insert")) {
						return true;
					}
					if (exonFunc.contains("stopgain")) {
						return true;
					}
					if (exonFunc.contains("stoploss")) {
						return true;
					}
				}
				
				return false;
			}
		};
}
