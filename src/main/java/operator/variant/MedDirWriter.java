package operator.variant;

import gene.Gene;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import ncbi.GeneInfoDB;

import org.apache.log4j.Logger;

import pipeline.Pipeline;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * A variant writer that writes variants in a form pleasing to medical directors (gene first, then cdot and pdot, etc.)
 *  
 * @author brendan
 *
 */
public class MedDirWriter extends VariantPoolWriter {

	public static final String CLINICAL_ONLY = "clinical.only";
	private boolean clinicalOnly = false; //If true, only write variants with HGMD, OMIM, or dbNSFP hits
	private GeneInfoDB geneInfo = null;
	
	public final static List<String> keys = new ArrayList<String>( Arrays.asList(new String[]{
		VariantRec.GENE_NAME,
		 VariantRec.NM_NUMBER,
		 VariantRec.CDOT,
		 VariantRec.PDOT,
		 "chromosome",
		 "position",
		 VariantRec.DEPTH,
		 "quality",
		 "zygosity",
		 VariantRec.EXON_NUMBER,
		 VariantRec.VARIANT_TYPE, 
		 VariantRec.EXON_FUNCTION,
		 VariantRec.POP_FREQUENCY,
		 VariantRec.AMR_FREQUENCY,
		 VariantRec.EXOMES_FREQ,
		 VariantRec.RSNUM,
		 VariantRec.ARUP_FREQ,
		 VariantRec.EFFECT_RELEVANCE_PRODUCT,
		 VariantRec.SVM_EFFECT,
		 Gene.GENE_RELEVANCE,
		 VariantRec.HGMD_HIT,
		 Gene.OMIM_DISEASES,
		 Gene.OMIM_NUMBERS,
		 Gene.OMIM_INHERITANCE,
		 Gene.OMIM_PHENOTYPES,
		 Gene.DBNSFP_DISEASEDESC,
		 Gene.SUMMARY,
		 Gene.HGMD_INFO,
		 Gene.SUMMARY_SCORE,
		 Gene.PUBMED_SCORE,
		 Gene.GO_SCORE,
		 Gene.DBNSFPGENE_SCORE,
		 Gene.INTERACTION_SCORE,
		 Gene.EXPRESSION_SCORE,
		 Gene.EXPRESSION_HITS,
		 Gene.GO_HITS,
		 Gene.PUBMED_HIT,
		 Gene.OMIM_PHENOTYPE_SCORE,
		 Gene.OMIM_PHENOTYPE_HIT,
		 VariantRec.SIFT_SCORE, 
		 VariantRec.POLYPHEN_SCORE, 
		 VariantRec.PHYLOP_SCORE, 
		 VariantRec.MT_SCORE,
		 VariantRec.GERP_SCORE,
		 VariantRec.LRT_SCORE,
		 VariantRec.SIPHY_SCORE}));
	
	public MedDirWriter() {
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		this.setComparator(new EffectProdSorter());
	}
	
	@Override
	public void writeHeader(PrintStream outputStream) {
		StringBuilder builder = new StringBuilder();
		builder.append(keys.get(0));
		
		for(String key : keys) {
			String val = key;
			if (keys.equals(Gene.HGMD_INFO)) {
				val = "hgmd.gene.match";
			}
			
			if (keys.equals(VariantRec.HGMD_HIT)) {
				val = "hgmd.exact.match";
			}
			
			if (keys.equals(VariantRec.EFFECT_RELEVANCE_PRODUCT)) {
				val = "overall.ranking.score";
			}
			
			builder.append("\t " + val);
		}

		if (genes == null)
			outputStream.println( "No gene information supplied, some annotations will not be written");
		
		if (additionalVariants != null) {
			for(VariantPool addPool : additionalVariants) {
				String varLabel = addPool.getObjectLabel() + ".zygosity";
				keys.add( varLabel );
				builder.append("\t " + varLabel);
			}
		}
		
		outputStream.println(builder.toString());
	
		
		String clinicalAttr = this.getAttribute(CLINICAL_ONLY);
		if (clinicalAttr != null) {
			if ( Boolean.parseBoolean(clinicalAttr) ) {
				clinicalOnly = true;
				Logger.getLogger(Pipeline.primaryLoggerName).info("Ignoring variants with no HGMD or OMIM info");
			}
			
		}
		
	}

	
	@Override
	public void writeVariant(VariantRec rec, PrintStream outputStream) {
		StringBuilder builder = new StringBuilder();
		
		
		builder.append( createGeneHyperlink(rec.getAnnotation(keys.get(0))) );
		
		//Find gene associated with variant
		Gene g = rec.getGene();
		
		if (g == null && genes != null) {
			String geneName = rec.getAnnotation(VariantRec.GENE_NAME);
			if (geneName != null)
				g = genes.getGeneByName(geneName);
		}
		
		if (clinicalOnly) {
			// If there's no HGMD info, OMIM info, or DBNSFP disease desc, 
			// don't report it
			if ( (rec.getAnnotation(VariantRec.HGMD_HIT) == null || rec.getAnnotation(VariantRec.HGMD_HIT).length() < 2) 
				&& (rec.getAnnotation(VariantRec.HGMD_INFO) == null || rec.getAnnotation(VariantRec.HGMD_INFO).length() < 2)
				&& g == null ) {
				return;			
			}
						
			if ( (rec.getAnnotation(VariantRec.HGMD_HIT) == null || rec.getAnnotation(VariantRec.HGMD_HIT).length() < 2) 
					&& (rec.getAnnotation(VariantRec.HGMD_INFO) == null || rec.getAnnotation(VariantRec.HGMD_INFO).length() < 2)
					&& (g.getAnnotation(Gene.DBNSFP_DISEASEDESC) == null || g.getAnnotation(Gene.DBNSFP_DISEASEDESC).length() < 2) 
					&& (g.getAnnotation(Gene.OMIM_DISEASES) == null || g.getAnnotation(Gene.OMIM_DISEASES).length() < 2)
					&& (g.getAnnotation(Gene.HGMD_INFO) == null || g.getAnnotation(Gene.HGMD_INFO).length() < 2)) {
				return;
			}
		}
		
		
		
		for(String key : keys) {
			String val = "?";
			
			val = getAnnotation(key, rec); //Searches for variant annotation, if none found the gene annotation
			
			if (key.equals("zygosity")) {
				val = rec.isHetero() ? "het" : "hom";
			}
			
			if (key.endsWith(".zygosity")) {
				if (additionalVariants.size()==0) {
					val = "-";
				}
				else {
					VariantPool addVars = poolForObjLabel(key.replace(".zygosity", ""));
					if (addVars == null) {
						val = "-";
					}
					else {
						VariantRec var = addVars.findRecordNoWarn(rec.getContig(), rec.getStart());
						if (var == null)
							val = "ref";
						else {
							if (var.isHetero()) {
								val = "het";
							}
							else {
								val = "hom";
							}
						}
					}
				}
			}			

			if (key.equals("quality")) {
				val = "" + rec.getQuality();
			}
			
			if (key.equals("chromosome")) {
				val = rec.getContig();
			}
			
			if (key.equals("position")) {
				val = "" + rec.getStart();
			}

			
			if (key.equals("chrom")) {
				val = rec.getContig();
			}
			
			if (key.equals("pos")) {
				val = "" + rec.getStart();
			}
			
			if (key.equals(VariantRec.GENE_NAME)) {
				if (val.length() > 1) {
					val = createGeneHyperlink(val);
				}		
			}
			
			if (key.equals(VariantRec.NM_NUMBER)) {
				if (val.length() < 3)
					val = "-";
			}
			
			if (key.equals(VariantRec.CDOT)) {
				if (val.length() < 3)
					val = "-";
			}
			
			if (key.equals(VariantRec.PDOT)) {
				if (val.length() < 3)
					val = "-";
			}
			
			if (key.equals(VariantRec.RSNUM)) {
				if (val.length() < 3)
					val = "-";
				else {
					val = createRSNumHyperlink(val);
				}
			}
		
			
			if (val == null || val.equals("null"))
				val = "-";
			builder.append("\t" + val);
		}

		
		
		
		outputStream.println(builder.toString());
	}
	
	private VariantPool poolForObjLabel(String label) {
		for(VariantPool pool : additionalVariants) {
			if (pool.getObjectLabel().equals(label)) {
				return pool;
			}
		}
		return null;
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


	private String createRSNumHyperlink(String rsnum) {
		if (rsnum == null || rsnum.length() < 3) {
			return "-";
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
	private String getParentZygStr(VariantRec rec, VariantPool parVars) {
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
	
	private String getAnnotation(String key, VariantRec var) {
		String val = var.getAnnotation(key);
		if (val == null) {
			val = "" + var.getProperty(key);
		}
		
		if (val == null || val.equals("null")) {
			Gene g = var.getGene();
			if (g!= null)
				val = g.getPropertyOrAnnotation(key);
		}
		
		return val;
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
				return -1;
			}
			if (s0 != null && s1 == null) {
				return 1;
			}
			
			return s0 > s1 ? -1 : 1;
			
		}
		
	}
}

