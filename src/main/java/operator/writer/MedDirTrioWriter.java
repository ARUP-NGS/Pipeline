package operator.writer;


import gene.Gene;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.logging.Logger;

import ncbi.GeneInfoDB;
import operator.OperationFailedException;
import operator.variant.VariantPoolWriter;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class MedDirTrioWriter extends VariantPoolWriter {

	public static final String PARENT_VARS = "ParentalVars";
	private GeneInfoDB geneInfo = null;
	
	public final static String[] defaultKeys = new String[]{
		VariantRec.GENE_NAME,
		 VariantRec.NM_NUMBER,
		 VariantRec.CDOT,
		 VariantRec.PDOT,
		 "zygosity",
		 "chrom",
		 "pos",
		 VariantRec.DEPTH,
		 "quality",
		 VariantRec.EXON_NUMBER,
		 VariantRec.VARIANT_TYPE, 
		 VariantRec.EXON_FUNCTION,
		 "parent.1.zygosity",
		 "parent.2.zygosity",
		 VariantRec.EFFECT_PREDICTION2,
		 VariantRec.POP_FREQUENCY,
		 VariantRec.AMR_FREQUENCY,
		 VariantRec.EXOMES_FREQ,
		 VariantRec.RSNUM, 
		 Gene.GENE_RELEVANCE,
		 VariantRec.OMIM_ID,
		 VariantRec.HGMD_HIT,
		 Gene.HGMD_INFO,
		 Gene.PUBMED_HIT,
		 VariantRec.SIFT_SCORE, 
		 VariantRec.POLYPHEN_SCORE, 
		 VariantRec.PHYLOP_SCORE, 
		 VariantRec.MT_SCORE,
		 VariantRec.GERP_SCORE,
		 VariantRec.LRT_SCORE,
		 VariantRec.SIPHY_SCORE};
	
	private VariantPool parent1Vars = null;
	private VariantPool parent2Vars = null;

	public MedDirTrioWriter() {
		this.setComparator(new EffectProdSorter());
	}
	
	@Override
	public void writeHeader(PrintStream outputStream) {
		StringBuilder builder = new StringBuilder();
		String[] keys = getKeys();
		builder.append(keys[0]);
		for(int i=1; i<keys.length; i++) {
			String val = keys[i];
			if (keys[i].equals(Gene.HGMD_INFO)) {
				val = "hgmd.gene.match";
			}
			
			if (keys[i].equals(VariantRec.HGMD_HIT)) {
				val = "hgmd.exact.match";
			}
			builder.append("\t " + val);
		}

		if (genes == null)
			outputStream.println( "No gene information supplied, some annotations will not be written");
		
		outputStream.println(builder.toString());
		
		try {
			if (parent1Vars != null && (!parent1Vars.isOperationPerformed()))
				parent1Vars.performOperation();
			if (parent2Vars != null && (!parent2Vars.isOperationPerformed())) {
				parent2Vars.performOperation();
			}
		}
		catch (OperationFailedException ex) {
			throw new IllegalStateException(ex.getMessage());
		}
	}

	/**
	 * Obtain a list of keys to write, i.e. VariantRec.GENE, VariantRec.EXON_NUM, etc etc
	 * @return
	 */
	public String[] getKeys() {
		return defaultKeys;
	}
	
	
	
	@Override
	public void writeVariant(VariantRec rec, PrintStream outputStream) {
		StringBuilder builder = new StringBuilder();
		
		String[] keys = getKeys();
		
		builder.append( createGeneHyperlink(rec.getAnnotation(keys[0])) );
		
		Gene g = rec.getGene();
		if (g == null && genes != null) {
			String geneName = rec.getAnnotation(VariantRec.GENE_NAME);
			if (geneName != null)
				g = genes.getGeneByName(geneName);
		}
		
		for(int i=1; i<keys.length; i++) {
			String val = "?";
			
			val = rec.getPropertyOrAnnotation(keys[i]).trim();

			if (keys[i].equals("zygosity")) {
				if (rec.isHetero())
					val = "het";
				else 
					val = "hom";
			}
			
			
			if (keys[i].equals("parent.1.zygosity")) {
				if (parent1Vars != null) {
					val = getParentZygStr(rec, parent1Vars);
				}
			}
			
			if (keys[i].equals("parent.2.zygosity")) {
				if (parent2Vars != null) {
					val = getParentZygStr(rec, parent2Vars);
				}
			}
			
			if (keys[i].equals(Gene.GENE_RELEVANCE)) {
				if (g!=null)
					val = "" + g.getProperty(Gene.GENE_RELEVANCE);
			}
			
			if (keys[i].equals("chrom")) {
				val = rec.getContig();
			}
			
			if (keys[i].equals("pos")) {
				val = "" + rec.getStart();
			}
			
			if (keys[i].equals(Gene.PUBMED_HIT)) {
				if (g!=null)
					val = g.getAnnotation(Gene.PUBMED_HIT);
			}
			
			if (keys[i].equals(Gene.HGMD_INFO)) {
				if (g!=null)
					val = g.getAnnotation(Gene.HGMD_INFO);
			}
			
			if (keys[i].equals(VariantRec.GENE_NAME)) {
				if (val.length() > 1) {
					val = createGeneHyperlink(val);
				}
					
			}
			
			if (keys[i].equals(VariantRec.NM_NUMBER)) {
				if (val.length() < 3)
					val = "-";
			}
			
			if (keys[i].equals(VariantRec.CDOT)) {
				if (val.length() < 3)
					val = "-";
			}
			
			if (keys[i].equals(VariantRec.PDOT)) {
				if (val.length() < 3)
					val = "-";
			}
			
			if (keys[i].equals(VariantRec.RSNUM)) {
				if (val.length() < 3)
					val = "-";
				else {
					val = createRSNumHyperlink(val);
				}
			}
			
			if (val == null)
				val = "-";
			builder.append("\t" + val);
		}

		
		
		
		outputStream.println(builder.toString());
	}
	
	protected String createGeneHyperlink(String val) {
		if (val == null)
			return null;
		
		if (geneInfo == null)
			geneInfo = GeneInfoDB.getDB();
		
		if (geneInfo == null)
			return val;
		
		String ncbiGeneId = geneInfo.idForSymbolOrSynonym(val);
		
		if (ncbiGeneId != null)
			return "=HYPERLINK(\"http://www.ncbi.nlm.nih.gov/gene/" + ncbiGeneId + "\", \"" + val + "\")";
		else {
			System.err.println("Could not find gene id for symbol: " + val);
			return val;
		}
	}


	protected String createRSNumHyperlink(String rsnum) {
		if (rsnum == null || rsnum.length() < 3) {
			return rsnum;
		}
		else {
			return "=HYPERLINK(\"http://www.ncbi.nlm.nih.gov/projects/SNP/snp_ref.cgi?searchType=adhoc_search&type=rs&rs=" + rsnum + "\", \"" + rsnum + "\")";
		}
	}
	
//	public void writeVariantOld(VariantRec rec, PrintStream outputStream) {
//		StringBuilder builder = new StringBuilder();
//		builder.append( rec.getPropertyOrAnnotation(keys[0]).trim() );
//		for(int i=1; i<keys.length; i++) {
//			String val = rec.getPropertyOrAnnotation(keys[i]).trim();
//			builder.append("\t" + val);
//		}
//
//		Gene g = rec.getGene();
//		if (g == null && genes != null) {
//			String geneName = rec.getAnnotation(VariantRec.GENE_NAME);
//			if (geneName != null)
//				g = genes.getGeneByName(geneName);
//		}
//		
//		if (g == null) {
//			builder.append("\n\t (no gene information found)");
//		}
//		else {
//			builder.append("\n\t Disease associations:\t" + g.getAnnotation(Gene.DBNSFP_DISEASEDESC));
//			builder.append("\n\t OMIM #s:\t" + g.getAnnotation(Gene.DBNSFP_MIMDISEASE));
//			builder.append("\n\t Summary:\t" + g.getAnnotation(Gene.SUMMARY));
//		}
//		outputStream.println(builder.toString());
//	}
	
	/**
	 * Returns a String describing whether or not this variant is in the given variant pool
	 * @param rec
	 * @param parVars
	 * @return
	 */
	protected String getParentZygStr(VariantRec rec, VariantPool parVars) {
		VariantRec par1Var = parVars.findRecordNoWarn(rec.getContig(), rec.getStart());
		if (par1Var == null)
			return "ref";
		else {
			if (par1Var.getAlt().equals(rec.getAlt())) {
				if (par1Var.isHetero()) {
					return "het";
				}
				else 
					return "hom";
			}
			else {
				return par1Var.getAlt();
			}
		}
	}
	
	@Override
	public void initialize(NodeList children) {
		super.initialize(children);
		
		//Look for parent variants object
		Element parVarsEl = getChildForLabel(PARENT_VARS, children);
		if (parVarsEl == null) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("No parental variants object found");
			return;
		}
		
		NodeList pVarChildren = parVarsEl.getChildNodes();
		for(int j=0; j<pVarChildren.getLength(); j++) {
			Node pVarChild = pVarChildren.item(j);
			if (pVarChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject pVar = getObjectFromHandler(pVarChild.getNodeName());
				if (pVar != null) {
					if (parent1Vars == null)
						parent1Vars = (VariantPool)pVar;
					else
						parent2Vars = (VariantPool)pVar;
				}
				
			}
		}
		
		if (parent1Vars != null) {
			Logger.getLogger(Pipeline.primaryLoggerName).info("Found parental variants object : " + parent1Vars.getObjectLabel() + " with " + parent1Vars.size() + " variants");
		}
		if (parent1Vars != null) {
			Logger.getLogger(Pipeline.primaryLoggerName).info("Found parental variants object : " + parent2Vars.getObjectLabel() + " with " + parent2Vars.size() + " variants");
		}
		
	}
	
	
	/**
	 * Compares variants by their EFFECT_RELEVANCE_PRODUCT property
	 * @author brendan
	 *
	 */
	class EffectProdSorter implements Comparator<VariantRec> {

		@Override
		public int compare(VariantRec v0, VariantRec v1) {
			Double s0 = v0.getProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT);
			Double s1 = v1.getProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT);
			
			if (s0 == null && s1 == null)
				return 0;
			if (s0 == null && s1 != null) {
				return 1;
			}
			if (s0 != null && s1 == null) {
				return -1;
			}
			
			return s0 < s1 ? 1 : -1;
			
		}
		
	}
	
}
