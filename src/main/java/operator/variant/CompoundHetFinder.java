package operator.variant;

import gene.Gene;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
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
import buffer.variant.GenePool;
import buffer.variant.VariantFilter;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class CompoundHetFinder extends Operator {

	private VariantPool kidPool = null;
	private VariantPool parent1Pool = null;
	private VariantPool parent2Pool = null;
	private CSVFile outputFile = null;
	private VariantPoolWriter outputFormatter = new MedDirWriter();
	
	GeneList genes = null;
	
	
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
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		List<CompoundHetHit> hits = null;
		if (parent1Pool == null && parent2Pool == null) {
			throw new OperationFailedException("Both parent pools are null, cannot compute compound hets", this);
		}
		if (parent1Pool != null && parent2Pool != null) {
			logger.info("CompoundHetFinder : Assuming " + kidPool.getObjectLabel() + " contains offspring variants \n " + parent1Pool.getObjectLabel() + " contains parent 1 variants \n " + parent2Pool.getObjectLabel() + " contains parent 2 variants");
			hits = computeCompoundHets(kidPool, parent1Pool, parent2Pool, genes);
		}
		else {
			if (parent1Pool != null) {
				logger.info("CompoundHetFinder : Using one-parent mode, assuming " + kidPool.getObjectLabel() + " contains offspring variants \n " + parent1Pool.getObjectLabel() + " contains parent variants and no additional parent variants provided");
				hits = computeCompoundHetsOnePar(kidPool, parent1Pool, genes);
			}
			if (parent2Pool != null) {
				logger.info("CompoundHetFinder : Using one-parent mode, assuming " + kidPool.getObjectLabel() + " contains offspring variants \n " + parent2Pool.getObjectLabel() + " contains parent variants and no additional parent variants provided");
				hits = computeCompoundHetsOnePar(kidPool, parent2Pool, genes);
			}
		}
		
		Collections.sort(hits, new HitRankComparator());
				
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
		
		writeHeader(outputStream);
		
		Collections.sort(hits, new HitRankComparator());
		
		for(CompoundHetHit hit : hits) {
			writeHit(outputStream, hit);
		}		
		
		outputStream.flush();
	}

	private void writeHeader(PrintStream out) {
		//out.println("gene\t var1.pdot \t var1.cdot \t var1.quality \t var1.depth \t var2.pdot \t var2.cdot \t var2.quality \t var2.depth \t dpnsfp.disease \t dbnsfp.mimdisease \thgmd.info" );
	}
	
//	private void writeHit(PrintStream out, CompoundHetHit hit) {
//		out.print(hit.gene.getName() + "\t" );
//		out.print(hit.kidVar1.getPropertyOrAnnotation(VariantRec.PDOT) + "\t" + hit.kidVar1.getPropertyOrAnnotation(VariantRec.CDOT) + "\t" + hit.kidVar1.getQuality() + "\t" + hit.kidVar1.getPropertyOrAnnotation(VariantRec.DEPTH) + "\t");
//		out.print(hit.kidVar2.getPropertyOrAnnotation(VariantRec.PDOT) + "\t" + hit.kidVar2.getPropertyOrAnnotation(VariantRec.CDOT) + "\t" + hit.kidVar2.getQuality() + "\t" + hit.kidVar2.getPropertyOrAnnotation(VariantRec.DEPTH) + "\t");
//		out.print(hit.gene.getAnnotation(Gene.DBNSFP_DISEASEDESC) + "\t");
//		out.print(hit.gene.getAnnotation(Gene.DBNSFP_MIMDISEASE) + "\t");
//		out.print(hit.gene.getAnnotation(Gene.HGMD_INFO) + "\n");
//	}
	
	private void writeHit(PrintStream out, CompoundHetHit hit) {
		out.println("\n Gene : \t" + hit.gene.getName() + "\t Score:\t" + hit.kidVar1.getGene().getProperty(Gene.GENE_RELEVANCE) + "\tDisease info: " + hit.gene.getAnnotation(Gene.DBNSFP_DISEASEDESC) );
		String par1Var1Zyg = "?"; //Zygosity of variant 1 in parent 1
		String par1Var2Zyg = "?"; //Zygosity of variant 2 in parent 1
		
		String par2Var1Zyg = "?"; //Zygosity of variant 1 in parent 2
		String par2Var2Zyg = "?"; //Zygosity of variant 2 in parent 2
		
		
		if (parent1Pool != null) {
			VariantRec par1Var1 = parent1Pool.findRecordNoWarn(hit.kidVar1.getContig(), hit.kidVar1.getStart());
			VariantRec par1Var2 = parent1Pool.findRecordNoWarn(hit.kidVar2.getContig(), hit.kidVar2.getStart());
			//If par1Var1 exists (variant 1 is in parent 1), then create a string with the zygosity in parent 1
			if (par1Var1 != null) {
				if (par1Var1.isHetero()) {
					par1Var1Zyg = "het";
				}
				else {
					par1Var1Zyg = "hom";
				}
			}
			
			if (par1Var2 != null) {
				if (par1Var2.isHetero()) {
					par1Var2Zyg = "het";
				}
				else {
					par1Var2Zyg = "hom";
				}
			}
		}
		
		
		if (parent2Pool != null) {
			VariantRec par2Var1 = parent2Pool.findRecordNoWarn(hit.kidVar1.getContig(), hit.kidVar1.getStart());
			VariantRec par2Var2 = parent2Pool.findRecordNoWarn(hit.kidVar2.getContig(), hit.kidVar2.getStart());
			
			//If par1Var1 exists (variant 1 is in parent 1), then create a string with the zygosity in parent 1
			if (par2Var1 != null) {
				if (par2Var1.isHetero()) {
					par2Var1Zyg = "het";
				}
				else {
					par2Var1Zyg = "hom";
				}
			}
			
			if (par2Var2 != null) {
				if (par2Var2.isHetero()) {
					par2Var2Zyg = "het";
				}
				else {
					par2Var2Zyg = "hom";
				}
			}
		}
		
			
		out.println("Variant 1: \t" + hit.kidVar1.getPropertyOrAnnotation(VariantRec.PDOT) + "\t" + hit.kidVar1.getPropertyOrAnnotation(VariantRec.CDOT) + "\t" + par1Var1Zyg + "\t" + par2Var1Zyg + "\t" + hit.kidVar1.getPropertyOrAnnotation(VariantRec.DEPTH) + "\t" + hit.kidVar1.getContig() + "\t" + hit.kidVar1.getStart() + "\t" + hit.kidVar1.getPropertyOrAnnotation(VariantRec.POP_FREQUENCY) + "\t" + hit.kidVar1.getPropertyOrAnnotation(VariantRec.ARUP_FREQ));
		out.println("Variant 2: \t" + hit.kidVar2.getPropertyOrAnnotation(VariantRec.PDOT) + "\t" + hit.kidVar2.getPropertyOrAnnotation(VariantRec.CDOT) + "\t" + par1Var2Zyg + "\t" + par2Var2Zyg + "\t" + hit.kidVar2.getPropertyOrAnnotation(VariantRec.DEPTH) + "\t" + hit.kidVar2.getContig() + "\t" + hit.kidVar2.getStart() + "\t" + hit.kidVar2.getPropertyOrAnnotation(VariantRec.POP_FREQUENCY) + "\t" + hit.kidVar2.getPropertyOrAnnotation(VariantRec.ARUP_FREQ));
	}

	
	/**
	 * Static method to perform compound het finding in cases where only one parent is known
	 * @param kidVars
	 * @param parent1Vars
	 * @param parent2Vars
	 * @param outputStream
	 */
	public static List<CompoundHetHit> computeCompoundHetsOnePar(VariantPool kidVars,
			VariantPool parentVars, 
			GeneList genes) {
		
		GeneInfoDB geneDB = GeneInfoDB.getDB();
		
		List<CompoundHetHit> hits = new ArrayList<CompoundHetHit>();
		
		GenePool kidGenes = new GenePool(kidVars.filterPool( nonSynFilter ));
		if (kidGenes.size() == 0) {
			throw new IllegalArgumentException("No genes found in kid gene list, this is probably an error");
		}
		GenePool par1Genes = new GenePool(parentVars.filterPool( nonSynFilter ));
		if (par1Genes.size() == 0) {
			throw new IllegalArgumentException("No genes found in parent gene list, this is probably an error");
		}
		
		for(String gene : kidGenes.getGenes()) {
			List<VariantRec> kidList = kidGenes.getVariantsForGene(gene);
			List<VariantRec> par1List = par1Genes.getVariantsForGene(gene);

			CompoundHetHit hit = hasCompoundHetOnePar(kidList, par1List);

			if (hit != null) {
				hit.gene = genes.getGeneByName( gene );
				if ( hit.gene == null) {
					System.err.println("Hmm, could not find gene for name : " + gene);
					String synonym = geneDB.symbolForSynonym(gene);
					if (synonym != null) {
						System.out.println("Found synonym for gene :" + gene + " -> " + synonym);
						hit.gene = genes.getGeneByName(synonym);
					}
					else {
						System.out.println("No synonym for gene :" + gene + " :(");
					}
				}
				else {
					hits.add(hit);
				}
				
			}
		}
		
		return hits;
	}
	
	/**
	 * Static method to actually perform compound het finding. This method is also used in varUtils.compoundHet
	 * @param kidVars
	 * @param parent1Vars
	 * @param parent2Vars
	 * @param outputStream
	 */
	public static List<CompoundHetHit> computeCompoundHets(VariantPool kidVars,
			VariantPool parent1Vars, 
			VariantPool parent2Vars,
			GeneList genes) {


		GeneInfoDB geneDB = GeneInfoDB.getDB();
		
		List<CompoundHetHit> hits = new ArrayList<CompoundHetHit>();
		
		GenePool kidGenes = new GenePool(kidVars.filterPool( nonSynFilter ));
		if (kidGenes.size() == 0) {
			throw new IllegalArgumentException("No genes found in kid gene list, this is probably an error");
		}
		
		GenePool par1Genes = new GenePool(parent1Vars.filterPool( nonSynFilter ));
		if (par1Genes.size() == 0) {
			throw new IllegalArgumentException("No genes found in parent #1 gene list, this is probably an error");
		}
		
		GenePool par2Genes = new GenePool(parent2Vars.filterPool( nonSynFilter ));
		if (par2Genes.size() == 0) {
			throw new IllegalArgumentException("No genes found in parent #2 gene list, this is probably an error");
		}
		
		for(String gene : kidGenes.getGenes()) {
			List<VariantRec> kidList = kidGenes.getVariantsForGene(gene);
			List<VariantRec> par1List = par1Genes.getVariantsForGene(gene);
			List<VariantRec> par2List = par2Genes.getVariantsForGene(gene);

			CompoundHetHit hit = hasCompoundHet(kidList, par1List, par2List);

			if (hit != null) {
				hit.gene = genes.getGeneByName( gene );
				if ( hit.gene == null) {
					System.err.println("Hmm, could not find gene for name : " + gene);
					String synonym = geneDB.symbolForSynonym(gene);
					if (synonym != null) {
						System.out.println("Found synonym for gene :" + gene + " -> " + synonym);
						hit.gene = genes.getGeneByName(synonym);
					}
					else {
						System.out.println("No synonym for gene :" + gene + " :(");
					}
				}
				else {
					hits.add(hit);
				}
				
			}
		}
		
		return hits;
	}
	
	
	/**
	 * Returns true if a) no list is empty (or null)
	 * b) the kid list contains at least one hetero var that is also hetero in par1, but absent from par2
	 * c) the kid list contains at least one hetero var that is also hetero in par2, but absent from par1
	 * 
	 * @param kidList
	 * @param par1List
	 * @param par2List
	 * @return
	 */
	private static CompoundHetHit hasCompoundHet(List<VariantRec> kidList,
										  List<VariantRec> par1List, 
										  List<VariantRec> par2List) {
		
		if (kidList == null || kidList.size() < 2 || kidList.size()>8) //Need at least two hets in kid list
			return null;
		if (par1List == null || par1List.size()==0)
			return null;
		if (par2List == null || par2List.size()==0)
			return null;
		
		boolean hasPar1Het = false; //True if any kid var is het and also het in par 1 and absent par2
		boolean hasPar2Het = false; 
		
		VariantRec kidHit1 = null;
		VariantRec kidHit2 = null;
		
		
		for(VariantRec rec : kidList) {
			if (rec.isHetero()) {
				//Kid variant is het, determine if this variant is present as a het in
				//one parent but not the other....
				
				VariantRec par1Var = contains(rec.getStart(), par1List);
				VariantRec par2Var = contains(rec.getStart(), par2List);
				
				boolean par1Contains = par1Var != null;
				boolean par2Contains = par2Var != null;
				
				boolean par1Het = isHetero(rec.getStart(), par1List);
				boolean par2Het = isHetero(rec.getStart(), par2List);
				
				
				if ( (!hasPar1Het) && (par1Het && (!par2Contains))) {
					kidHit1 = rec;
				}
				
				if ( (!hasPar2Het) && (par2Het && (!par1Contains))) {
					kidHit2 = rec;
				}
				
				hasPar1Het = hasPar1Het || (par1Het && (!par2Contains));
				hasPar2Het = hasPar2Het || (par2Het && (!par1Contains)); 

				if (hasPar1Het && hasPar2Het) {
					CompoundHetHit hit = new CompoundHetHit();
					hit.kidVar1 = kidHit1;
					hit.kidVar2 = kidHit2;
					return hit;
				}
			}
			
		}
		
		return null;
	}
	
	
	/**
	 * Returns true if a) no list is empty (or null)
	 * b) the kid list contains at least one hetero var that is also hetero in par1, but absent from par2
	 * c) the kid list contains at least one hetero var that is also hetero in par2, but absent from par1
	 * 
	 * @param kidList
	 * @param par1List
	 * @param par2List
	 * @return
	 */
	private static CompoundHetHit hasCompoundHetOnePar(List<VariantRec> kidList,
										  List<VariantRec> parList) {
		
		if (kidList == null || kidList.size() < 2 || kidList.size()>8) //Need at least two hets in kid list
			return null;
		if (parList == null || parList.size()==0)
			return null;
		
		boolean hasPar1Het = false; //True if any kid var is het and also het in par 1 and absent par2
		boolean hasPar2Het = false; 
		
		VariantRec kidHit1 = null;
		VariantRec kidHit2 = null;
		
		for(VariantRec rec : kidList) {
			if (rec.isHetero()) {
				boolean parHet = isHetero(rec.getStart(), parList);
				VariantRec parVar = contains(rec.getStart(), parList);
				boolean parContains = parVar != null;
				
				
				if ( (!hasPar1Het) && parHet ) {
					kidHit1 = rec;
				}
				
				if ( (!hasPar2Het) && (!parContains)) {
					kidHit2 = rec;
				}
				
				hasPar1Het = hasPar1Het || parHet ;
				hasPar2Het = hasPar2Het || (!parContains); 

				if (hasPar1Het && hasPar2Het) {
					CompoundHetHit hit = new CompoundHetHit();
					hit.kidVar1 = kidHit1;
					hit.kidVar2 = kidHit2;
					return hit;
				}
			}
			
		}
		
		return null;
	}

	/**
	 * Returns true if there is a variant at the given start position
	 * is the list AND the variant is a heterozygote 
	 * @param pos
	 * @param list
	 * @return
	 */
	public static boolean isHetero(int pos, List<VariantRec> list) {
		for(VariantRec rec : list) {
			if (rec.getStart()==pos && rec.isHetero()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if the given var list contains a variant that starts at the
	 * given position
	 * @param pos
	 * @param list
	 * @return
	 */
	public static VariantRec contains(int pos, List<VariantRec> list) {
		for(VariantRec rec : list) {
			if (rec.getStart()==pos) {
				return rec;
			}
		}
		return null;
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


	}
	
	/**
	 * Filtering class to remove all synonymous variants
	 * @author brendan
	 *
	 */
	public static class NonSynFilter implements VariantFilter {
		
		public boolean passes(VariantRec rec) {
			String func = rec.getAnnotation(VariantRec.EXON_FUNCTION);
			if (func == null)
				return true;
			if ((!func.contains("nonsynonymous")) && func.contains("synonymous")) {
				return false;
			}
			return true;
		}
		
	}
	
	public static class CompoundHetHit {
		Gene gene = null;
		VariantRec kidVar1 = null;
		VariantRec kidVar2 = null;
	}
	
	class HitRankComparator implements Comparator<CompoundHetHit> {

		@Override
		public int compare(CompoundHetHit o1, CompoundHetHit o2) {
			Gene g0 = o1.gene;
			Gene g1 = o2.gene;
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
}
