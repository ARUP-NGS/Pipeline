package buffer.variant;

import gene.Gene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import json.JSONArray;
import json.JSONObject;
import util.smallMap.SmallMap;
import util.vcfParser.VCFParser.GTType;

/**
 * A class to store some basic information about a single variant
 * @author brendan
 *
 */
public class VariantRec {

	String contig;
	int start;
	int end;
	String ref;
	String alt;
	Double qual;
	String GT;
	protected GTType zygosity;
	protected Map<String, Double> props = new SmallMap<String, Double>(); 
	protected Map<String, Integer> ints = new SmallMap<String, Integer>(); 
	protected Map<String, String> annotations = new SmallMap<String, String>(); 
	protected Map<String,JSONArray> jsonobj = new SmallMap<String, JSONArray>();

	Gene gene;
	
	public VariantRec(String contig, 
			int start, 
			int end, 
			String ref, 
			String alt) {
		this.contig = contig;
		this.start = start;
		this.end = end;
		this.ref = ref;
		this.alt = alt;
		this.qual = 1000.0;
		this.GT = "-/-";
		this.zygosity = GTType.UNKNOWN;
	}

	public VariantRec(String contig, 
			int start, 
			int end, 
			String ref, 
			String alt,
			Double qual,
			GTType zygosity) {
		this.contig = contig;
		this.start = start;
		this.end = end;
		this.ref = ref;
		this.alt = alt;
		this.qual = qual;
		this.zygosity = zygosity;
	}
	
	public VariantRec(String contig, 
							int start, 
							int end, 
							String ref, 
							String alt, 
							Double qual,
							String GT,
							GTType zygosity) {
		this.contig = contig;
		this.start = start;
		this.end = end;
		this.ref = ref;
		this.alt = alt;
		this.qual = qual;
		this.GT = GT;
		this.zygosity = zygosity;
	}
	
	public synchronized void addProperty(String key, Double val) {
		props.put(key, val);
	}
	
	public synchronized void addAnnotation(String key, String anno) {
		annotations.put(key, anno);
	}
	public synchronized void addPropertyInt(String key, Integer num) {
		ints.put(key, num);
	}
	
	public synchronized void addAnnotationJSON(String key, JSONArray masterlist) {
		jsonobj.put(key, masterlist);
	}
	
	public void setQuality(Double quality) {
		this.qual = quality;
	}
	
	public String getRef() {
		return ref;
	}
	
	/**
	 * Set a reference to the Gene object associated with this variant
	 * @param g
	 */
	public void setGene(Gene g) {
		String geneNameAnno = getAnnotation(VariantRec.GENE_NAME);
		if (geneNameAnno != null) {
			if (! g.getName().equals(geneNameAnno)) {
				System.err.println("Warning : Assigning gene with name " + g.getName() + " to variant with gene name already assigned to : " + geneNameAnno);
			}
		}
		this.gene = g;
	}

	/**
	 * Obtain the gene associated with this variant, if one has been set 
	 * @return
	 */
	public Gene getGene() {
		return gene;
	}
	
	/**
	 * Returns true if there is more than one alt allele at this site 
	 * @return
	 */
	public boolean isMultiAllelic() {
		return alt.contains(",");
	}
	
	/**
	 * Returns all alt alleles at this site in array form 
	 * @return
	 */
	public String[] getAllAlts() {
		return alt.split(",");
	}
	
	/**
	 * Count the number of leading bases that are identical between ref and alt
	 * @return
	 */
	public int countInitialMatchingBases() {
		int count = 0;
		int min = Math.min(ref.length(), alt.length());
		while(count < min && ref.charAt(count)==alt.charAt(count)) {
			count++;
		}
		return count;
	}
	
	/**
	 * True if any of the alt alleles in this variant rec matches the given alt
	 * the 'alt alleles' are those returned by getAllAlts(), which splits the usual getAlt() on ','
	 * @param alt
	 * @return
	 */
	public boolean containsAlt(String alt) {	
		String[] alts = getAllAlts();
		for(int i=0; i<alts.length; i++) {
			if (alt.equals(alts[i])) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Set the start and end positions for this variant
	 * @param start
	 * @param end
	 */
	public void setPosition(String contig, int start, int end) {
		this.contig = contig;
		this.start = start;
		this.end = end;
	}
	
	/**
	 * Returns true if both the ref and alt allele have length 1 and neither is '-'
	 * @return
	 */
	public boolean isSNP() {
		return (ref.length()==1 && alt.length()==1 && ref.charAt(0) != '-' && alt.charAt(0) != '-');
	}
	
	/**
	 * Returns true if ref and alt are different lengths
	 * @return
	 */
	public boolean isIndel() {
		return isInsertion() || isDeletion();
	}
	
	/**
	 * Returns the length of the insertion / deletion, or 0 if this
	 * is not an insertion or deletion
	 * @return
	 */
	
	
	public int getIndelLength() {
		if (isInsertion())
			return alt.length();
		if (isDeletion())
			return ref.length();
		return 0;
	}
	
	/**
	 * True if the ref length is 1 and the alt length is strictly greater than one
	 * @return
	 */
	public boolean isInsertion() {
		return ref.equals("-") && (!alt.equals("-"));
	}
	
	/**
	 * True if ref length > 1 and alt length is equal to 1
	 * @return
	 */
	public boolean isDeletion() {
		return alt.equals("-") && (!ref.equals("-"));
	}
	
	public boolean isTransition() {
		if (ref == null || alt == null)
			throw new IllegalArgumentException("Ref or alt is null");
		if (ref.equals("-") || alt.equals("-")) {
			throw new IllegalArgumentException("Ref or alt not defined");
		}
		if (ref.equals(alt)) {
			System.err.println("WARNING : ref is equal to alt, not a variant");
			return false;
			//throw new IllegalArgumentException("Ref is equal to alt, not a variant");
		}
		
		if ( (ref.equals("A") && alt.equals("G"))
			  || (ref.equals("G") && alt.equals("A"))
			  || (ref.equals("T") && alt.equals("C"))
			  || (ref.equals("C") && alt.equals("T"))) {
			return true;
		}
		return false;
	}
	
	public boolean isTransversion() {
		if (ref == null || alt == null)
			throw new IllegalArgumentException("Ref or alt is null");
		if (ref.equals("-") || alt.equals("-")) {
			throw new IllegalArgumentException("Ref or alt not defined");
		}
		if (ref.equals(alt)) {
			System.err.println("WARNING : ref is equal to alt, not a variant");
			return false;
			//throw new IllegalArgumentException("Ref is equal to alt, not a variant");
		}
		
		if (ref.equals("A") || ref.equals("G")) {
			if (alt.equals("C") || alt.equals("T"))
				return true;
			else
				return false;			
		}
		
		if (ref.equals("T") || ref.equals("C")) {
			if (alt.equals("A") || alt.equals("G"))
				return true;
			else
				return false;			
		}
		
		return false;
	}
	
	/**
	 * Returns true if alt is not equal in value to ref
	 * @return
	 */
	public boolean isVariant() {
		return !getAlt().equals(getRef());
	}
	
	public String getAlt() {
		return alt;
	}
	
	public Double getProperty(String key) {
		return props.get(key);
	}
	
	public JSONArray getjsonProperty(String key){
		return jsonobj.get(key);
	}
	
	public Integer getPropertyInt(String key){
		return ints.get(key);
	}
	
	/**
	 * Returns the property associated with the given key, but if there
	 * is no such property, returns the annotation with the given key, and
	 * if there's no annotation either returns "-";
	 * @param key
	 * @return
	 */
	public String getPropertyOrAnnotation(String key) {
		if (key.equals("quality")) {
			return "" + getQuality();
		}
		
		Double val = getProperty(key);
		if (val != null)
			return "" + val;
		
		String anno = getAnnotation(key);
		if (anno != null)
			return anno;
		
		JSONArray json = getjsonProperty(key);
		if (json != null)
			return "" + json;
		
		Integer intvar = getPropertyInt(key);
		if(intvar != null)
			return "" +intvar;
		
		return "-";
	}
	
	/**
	 * Obtain a Collection containing all keys used to describe properties
	 * @return
	 */
	public Collection<String> getPropertyKeys() {
		return props.keySet();
	}
	
	/**
	 * Collection of all keys used for annotations
	 * @return
	 */
	public Collection<String> getAnnotationKeys() {
		return annotations.keySet();
	}
	
	/**
	 * Collection of all keys used for ints
	 * @return
	 */
	public Collection<String> getIntKeys() {
		return ints.keySet();
	}
	
	/**
	 * Collection of all keys used for json annotations
	 * @return
	 */
	public Collection<String> getJsonobjKeys() {
		return jsonobj.keySet();
	}
	
	public String getAnnotation(String key) {
		return annotations.get(key);
	}
	
	public boolean hasProperty(String key) {
		return props.get(key)!=null;
	}
	
	/**
	 * Returns a tab-separated string containing properties (but not annotation) 
	 * values indexed by the list of keys given
	 * @param propKeys
	 * @return
	 */
	public String getPropertyString(List<String> propKeys) {
		StringBuffer buf = new StringBuffer();
		for(String key : propKeys) {
			buf.append( "\t" +  getPropertyOrAnnotation(key) );
//			Double val = props.get(key);
//			if (val != null)
//				buf.append("\t" + val);
//			else {
//				String anno = annotations.get(key);
//				if (key == VariantRec.RSNUM && anno != null) {
//					buf.append("\t =HYPERLINK(\"http://www.ncbi.nlm.nih.gov/snp/?term=" + anno + "\")");
//				}
//				else {
//					if (anno != null)
//						buf.append("\t" + anno);
//					else
//						buf.append("\t NA");
//				}
//			}
		}
		return buf.toString();
	}
	
	public String getContig() {
			return contig;
	}
	
	public int getStart() {
		return start;
	}
	
	public int getEnd() {
		return end;
	}
	
	public Double getQuality() {
		return qual;
	}
	
	public String getGenotype() {
		return GT;
	}
	
	public GTType getZygosity() {
		return zygosity;
	}
	
	
	public static String getColumnHeaders() {
		return "contig \t start \t end \t gene \t variant.type \t exon.func \t pop.freq \t het \t qual \t sift \t polyphen \t mt \t phylop";  
	}
	
	/**
	 * Obtain a header string for the column emitted by toBasicString
	 * @return
	 */
	public static String getBasicHeader() {
		return "#contig \tstart \tend \tgene \tvariant.type \texon.function \tzygosity";
	}
	
	/**
	 * Obtain a string containing the following information about this variant record:
	 * 1. contig
	 * 2. start
	 * 3. end
	 * 4. genotype
	 * 5. gene name
	 * 6. variant type
	 * 7. exon function (- if not an exon)
	 * 8. hetero/homo
	 * @return
	 */
	public String toBasicString() {
/**	
		String gene = getAnnotation(VariantRec.GENE_NAME);
		if (gene == null)
			gene = "-";
	
		String variantType = "-";
		String vType = getAnnotation(VariantRec.VARIANT_TYPE);
		if (vType != null)
			variantType = vType;

		String exFunc = "-";
		String exType = getAnnotation(VariantRec.EXON_FUNCTION);
		if (exType != null)
			exFunc = exType;
**/			
		String het = "het";
		if (getZygosity() == GTType.HOM) {
			het = "hom";
		} else if (getZygosity() == GTType.HEMI) {
			het = "hemi";
		} else if (getZygosity() == GTType.UNKNOWN){
			het = "unknown";
		}
	
		//return contig + "\t" + start + "\t" + end + "\t" + GT + "\t" + gene + "\t" + variantType + "\t" + exFunc + "\t" + het ;  
		return contig + "\t" + start + "\t" + end + "\t" + GT + "\t" + het ;  
	}
	
	/**
	 * Return a header row that describes the toSimpleString() columns
	 * @return
	 */
	public static String getSimpleHeader() {
		return "#contig\tstart \t end \t ref \t alt \t quality \t depth \t zygosity \t genotype.quality \t " + VariantRec.VAR_DEPTH;
	}
	
	/**
	 * Return a string with the following columns:
	 * 1. Contig
	 * 2. start
	 * 3. end
	 * 4. ref
	 * 5. alt
	 * 6. variant quality
	 * 7. total read depth
	 * 8. variant read depth
	 * 9. het / hom
	 * 10. genotype quality
	 * @return
	 */
	public String toSimpleString() {
		String het = "het";
		if (getZygosity() == GTType.HOM) {
			het = "hom";
		} else if (getZygosity() == GTType.HEMI) {
			het = "hemi";
		} else if (getZygosity() == GTType.UNKNOWN) {
			het = "unknown";
		}
		
		Double depth = getProperty(VariantRec.DEPTH);
		String depthStr = "-";
		if (depth != null) 
			depthStr = "" + depth;
		
		Double genotypeQual = getProperty(VariantRec.GENOTYPE_QUALITY);
		String gqStr = "-";
		if (genotypeQual != null)
			gqStr = "" + genotypeQual;
		
		Double varDepth = getProperty(VariantRec.VAR_DEPTH);
		String varDepthStr = "-";
		if (varDepth != null)
			varDepthStr = varDepth + "";
		
/*		String badreg = getAnnotation("bad.region");
		if (badreg == null) {
			badreg = "NA";
		} else if (badreg.equals("-")) {
			badreg = "F";
		} else if (badreg.equals("TRUE")) {
			badreg = "T";
		}*/
		
		return contig + "\t" + start + "\t" + end + "\t" + getRef() + "\t" + getAlt() + "\t" + getQuality() + "\t" + depthStr + "\t" + het + "\t" + gqStr + "\t" + varDepthStr;  
	}
	
	public void setAlt(String newAlt) {
		this.alt = newAlt;
	}
	
	/**
	 * Adjusts all indel variants in the following manner: Any indel that begins and ends with the same
	 * base, the first base is moved to the last position and 1 is subtracted from the start and end position
	 * So		: 117  - ACGTA
	 * Becomes 	: 116  - CGTAA
	 */
//	public void rotateIndel() {
//		if (isInsertion()) {
//			int count = 0;
//			while (count < 20 && alt.charAt(0) == alt.charAt(alt.length()-1)) {
//				alt = alt.substring(1) + alt.charAt(0);
//				start--;
//				end--;
//				count++;
//			}
//		}
//		
//		if (isDeletion()) {
//			int count = 0;
//			while (count < 20 && ref.charAt(0) == ref.charAt(ref.length()-1)) {
//				ref = ref.substring(1) + ref.charAt(0);
//				start--;
//				end--;
//				count++;
//			}
//		}
//	}
	
	public String toString() {
		return toSimpleString();
	}
	
	public String annotationsToString(){
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for(String key : annotations.keySet()){
			if(i == 0){
                sb.append(key + ": " + annotations.get(key));
                i++;
			}
			else{
                sb.append(", " + key + ": " + annotations.get(key));
			}
		}
		return sb.toString();
	}
	
	/**
	 * Obtain an object that compares two variant records for start site
	 * @return
	 */
	public static PositionComparator getPositionComparator() {
		return new PositionComparator();
	}
	
	/**
	 * Obtain an object that compares two variant records for the property
	 * associated with the given key
	 * @return
	 */
	public static PropertyComparator getPropertyComparator(String key) {
		return new PropertyComparator(key);
	}
	
	public static IntervalComparator getIntervalComparator() {
		return new IntervalComparator();
	}

	public static ArrayList<String> equals(VariantRec r1, VariantRec r2){
		
		ArrayList<String> errors = new ArrayList<String>();
		
		/* Check if same chr */
		if(!r1.getContig().equals(r2.getContig())){
			errors.add("Records not on same chrom. Rec 1: " + r1.getContig()
					+ "\tRec 2: " + r2.getContig());
			return errors; /* no need to continue with more unnecessary errors */
		}
		/* Check for same start if same chr */
		else if(r1.getStart() != r2.getStart()){
			errors.add("Records do not begin at same position. Rec 1: " + r1.getStart()
					+ "\tRec 2: " + r2.getStart());
			return errors; /* no need to continue with more unnecessary errors */
		}
		
		/* Check for same end */
		if(r1.getEnd() != r2.getEnd()){
			errors.add("Records do not end at same position. Rec 1: " + r1.getEnd()
					+ "\tRec 2: " + r2.getEnd());
			return errors; /* no need to continue with more unnecessary errors */
		}
		
		/* Check for same ref */
		if(!r1.getRef().equals(r2.getRef())){
			errors.add("Records do not have the same reference. Rec 1: " + r1.getRef()
					+ "\tRec 2: " + r2.getRef());
			return errors; /* no need to continue with more unnecessary errors */
		}

		/* Check for same alts */
		TreeSet<String> r1Alts = new TreeSet<String>(Arrays.asList(r1.getAllAlts()));
		TreeSet<String> r2Alts = new TreeSet<String>(Arrays.asList(r2.getAllAlts()));
		if(!r1Alts.equals(r2Alts)){
			errors.add("Records do not have the same alternate alleles. Rec 1: " + r1Alts.toString()
					+ "\tRec 2: " + r2Alts.toString());
			return errors; /* no need to continue with more unnecessary errors */
		}
		
		/* Check for same gene name */
		Gene r1Gene = r1.getGene();
		Gene r2Gene = r2.getGene();
		if(r1Gene != null && r2Gene != null){
            if(!r1Gene.getName().equals(r2Gene.getName())){
                errors.add("Records do not have the same gene name. Rec 1: " + r1Gene.getName()
                        + "\tRec 2: " + r2Gene.getName());
            }
		}
		else if(r1Gene == null && r2Gene == null){
			// This is fine. They equal.
		}
		else{ /* One must be null and the other isn't */
			String r1GeneString = null, r2GeneString = null;
			if(r1Gene == null){
				r1GeneString = "null";
            }
			else{
				r1GeneString = r1Gene.getName();
			}

			if(r2Gene == null){
				r2GeneString = "null";
            } 
			else{
				r2GeneString = r2Gene.getName();
			}
            errors.add("Records do not have the same gene name. Rec 1: " + r1GeneString
                    + "\tRec 2: " + r2GeneString);
		}
		
		/* Check the annotations equal */
		Collection<String> r1AnnoKeys = r1.getAnnotationKeys();
		Collection<String> r2AnnoKeys = r2.getAnnotationKeys();
		
		/* Check the to records have the same annotations set (same keys exist) */
		if(!r1AnnoKeys.equals(r2AnnoKeys)){
            errors.add("Records do not have the same annotation keys. Rec 1: " + r1AnnoKeys.toString()
                    + "\tRec 2: " + r2AnnoKeys.toString());
		}
		/* If they have the same keys, see if the values equal */
		else{ 
			String r1Anno, r2Anno;
			for(String key : r1AnnoKeys){
				r1Anno = r1.getAnnotation(key);
				r2Anno = r2.getAnnotation(key);
				if(!r1Anno.equals(r2Anno)){
                    errors.add("Record annotation " + key + " does not have matching value. Rec 1: " + r1Anno
                            + "\tRec 2: " + r2Anno);
				}
			}
		}
			
		
        return errors;
//		String[] r1Alts = r1.getAllAlts();
//		String[] r2Alts = r2.getAllAlts();
//		/* Check if same number of alts */
//		if(r1Alts.length != r2Alts.length){
//			
//		}
//		/* If same number of alts, check that the sets equal */
//		else{
//			boolean contains;
//			for(int i = 0; i < r1Alts.length; i++){
//				contains = false;
//				for(int j = 0; j < r2Alts.length; i++){
//					if(r1Alts[i].equals(r2Alts[j])){
//						contains = true;
//						break;
//					}
//				}
//				if(!contains){
//					
//				}
//			}
//		}
	}
	
	public static class PositionComparator implements Comparator<VariantRec> {

		@Override
		public int compare(VariantRec o1, VariantRec o2) {
			if (o1 == o2 || (o1.equals(o2))) {
				int result = o1.getRef().compareTo(o2.getRef());
				if (result == 0) {
					result = o1.getAlt().compareTo(o2.getAlt());
				}
				return result;
			}

			return o1.start - o2.start;
		}
	}
	
	public static class IntervalComparator implements Comparator<VariantRec> {

		@Override
		public int compare(VariantRec o1, VariantRec o2) {
			VariantRec first;
			VariantRec second;
			if (o1.getStart() <= o2.getStart()) {
				first = o1;
				second = o2;
			}
			else {
				first = o2;
				second = o1;
			}
			
			boolean disjoint = first.getStart() + first.getAlt().length() -1 < second.getStart();
			if (! disjoint) {
				return 0;
			}

			return o1.start - o2.start;
		}
	}
	
	
	/**
	 * A generic comparator that retrieves the property with the given key and compares
	 * two variant records to see which value is greater. An exception is thrown if
	 * one record does not contain a property associated with the key
	 * @author brendan
	 *
	 */
	static class PropertyComparator implements Comparator<VariantRec> {

		final String key;
		
		public PropertyComparator(String key) {
			this.key= key;
		}
		
		@Override
		public int compare(VariantRec o1, VariantRec o2) {
			Double val1 = o1.getProperty(key);
			Double val2 = o2.getProperty(key);
			if (val1 == null)
				throw new IllegalArgumentException("Cannot compare variant record for key " + key + " because it has not been assigned to var1");
			if (val2 == null)
				throw new IllegalArgumentException("Cannot compare variant record for key " + key + " because it has not been assigned to var2");
			
			return val1 < val2 ? -1 : 1;
		}
		
	}
	
	//A few oft-used property / annotation keys
	public static final String GENOTYPE = "genotype";
	public static final String VCF_POS = "vcf.position";
	public static final String VCF_REF = "vcf.ref";
	public static final String VCF_ALT = "vcf.variant";
	public static final String VCF_FILTER = "vcf.filter";
	public static final String EFFECT_PREDICTION = "effect.prediction";
	public static final String EFFECT_PREDICTION2 = "effect.prediction2";
	public static final String POP_FREQUENCY = "pop.freq";
	public static final String AMR_FREQUENCY = "amr.freq";
	public static final String EUR_FREQUENCY = "eur.freq";
	public static final String AFR_FREQUENCY = "afr.freq";
	public static final String ASN_FREQUENCY = "asn.freq";
	public static final String CG69_FREQUENCY = "cg69.freq";
	public static final String SIFT_SCORE = "sift.score";
	public static final String SIFT_PRED = "sift.pred";
	public static final String POLYPHEN_HVAR_SCORE = "pp.score";
	public static final String POLYPHEN_SCORE = "pp.hdiv.score";
	public static final String POLYPHEN_HVAR_PRED = "pp.pred";
	public static final String MA_SCORE = "mut.assessor.score";
	public static final String MA_PRED = "mut.assessor.pred";
	public static final String MT_SCORE = "mt.score";
	public static final String MT_PRED = "mt.pred";
	public static final String GERP_NR_SCORE = "gerp.nr.score";
	public static final String GERP_SCORE = "gerp.score";
	public static final String SLR_TEST = "slr.score";
	public static final String PHYLOP_SCORE = "phylop.score";
	public static final String LRT_SCORE = "lrt.score";
	public static final String SIPHY_SCORE = "siphy.score";
	public static final String SVM_EFFECT = "svm.effect.prediction";
	public static final String EXON_FUNCTION = "exon.function";
	public static final String FS_SCORE = "strand.bias.score";
	public static final String LOGFS_SCORE = "log.fs";
	public static final String DEPTH = "depth";
	public static final String VAR_CALLER = "var.call";
	public static final String RAW_GT = "raw.gt";

	//snpEff .2 and .3 annotations only apply to arupBedFile usage with multiple transcripts in a bed line (column 4, "|" separated)
	public static final String CDOT = "cdot";
	public static final String PDOT = "pdot";
	public static final String EXON_NUMBER = "exon.number";
	public static final String NM_NUMBER = "nm.number";
	public static final String GENE_NAME = "gene";
	public static final String VARIANT_TYPE = "variant.type";
	public static final String CDOT2 = "cdot.2";
	public static final String PDOT2 = "pdot.2";
	public static final String EXON_NUMBER2 = "exon.number.2";
	public static final String NM_NUMBER2 = "nm.number.2";
	public static final String GENE_NAME2 = "gene.2";
	public static final String VARIANT_TYPE2 = "variant.type.2";
	public static final String CDOT3 = "cdot.3";
	public static final String PDOT3 = "pdot.3";
	public static final String EXON_NUMBER3 = "exon.number.3";
	public static final String NM_NUMBER3 = "nm.number.3";
	public static final String GENE_NAME3 = "gene.3";
	public static final String VARIANT_TYPE3 = "variant.type.3";

	public static final String zygosityB = "zygB"; //When performing intersections, zygosity of variant in other pool
	public static final String altB = "altB"; //Alternate allele in other pool when performing intersections
	public static final String RSNUM = "rsnum"; // rs# from dbSNP
	public static final String OMIM_ID = "omim.id";
	public static final String GO_EFFECT_PROD = "go.effect.prod";
	public static final String GENOTYPE_QUALITY = "genotype.quality";
	public static final String SOURCE = "source.file";
	public static final String VAR_DEPTH = "var.depth";
	public static final String VAR2_DEPTH = "var2.depth";
	public static final String FALSEPOS_PROB = "fp.prob";
	public static final String TAUFP_SCORE = "taufp.score";
	public static final String VQSR = "vqsr.score";
	public static final String EXOMES_FREQ = "exomes6500.frequency";
	public static final String EXOMES_HOM_FREQ = "exomes6500.homalt.frequency";
	public static final String EXOMES_FREQ_EA = "exomes6500.EA.frequency";
	public static final String EXOMES_FREQ_AA = "exomes6500.AA.frequency";
	
	public static final String EXOMES_EA_HOMREF = "exomes6500.EA.homref";
	public static final String EXOMES_EA_HET = "exomes6500.EA.het";
	public static final String EXOMES_EA_HOMALT = "exomes6500.EA.homalt";
	
	public static final String EXOMES_AA_HOMREF = "exomes6500.AA.homref";
	public static final String EXOMES_AA_HET = "exomes6500.AA.het";
	public static final String EXOMES_AA_HOMALT = "exomes6500.AA.homalt";

	public static final String HGMD_INFO = "hgmd.info";
	public static final String HGMD_VCF_HIT = "hgmd.exact.hit";
	public static final String HGMD_VCF_CLASS = "hgmd.class";
	public static final String HGMD_VCF_DISEASE = "hgmd.disease";
	public static final String HGMD_HIT = "hgmd.hit";

	public static final String SAMPLE_COUNT = "sample.count";	
	public static final String EFFECT_RELEVANCE_PRODUCT = "effect.rel.product";
	public static final String ARUP_FREQ = "ARUP.freq";
	public static final String ARUP_TOT = "ARUP.tot";
	public static final String ARUP_OVERALL_FREQ = "ARUP.overall.freq";
	public static final String ARUP_FREQ_DETAILS = "ARUP.freq.details";
	public static final String VARBIN_BIN = "varbin.bin";
	public static final String CLINVAR_TYPE = "dbsnp.clinvar.type";
	public static final String CLINVAR_VALIDATED = "dbsnp.clinvar.validated";
	public static final String RP_SCORE = "readpos.score";
	public static final String HALOPLEX_PANEL_FREQ = "haloplex.freq";
	public static final String SPLICING_ALL = "splicing.all";
	public static final String SPLICING_TOPNM = "splicing.topnm";
	public static final String SPLICING_TOPNMDIFF = "splicing.topnmdiff";
	public static final String HOTSPOT_ID = "hotspot.id";
	public static final String NON_PREFERRED_TRANSCRIPT = "non.preferred.transcript";
	public static final String PFAM_AC = "pfam.ac";
	public static final String PFAM_ID = "pfam.id";
	public static final String PFAM_DESC = "pfam.desc";
	public static final String SCOP_DOMAIN = "scop.domain";
	public static final String COSMIC_ID = "HotSpot.ID"; //funky capitalization for consistency with IonTorrent stuff, don't change it
	public static final String COSMIC_COUNT = "cosmic.count";
	public static final String MITOMAP_FREQ = "mitomap.db.freq";
	public static final String MITOMAP_ALLELE_ID = "mitomap.allele.id";
	public static final String MITOMAP_DIS_CODING = "mitomap.coding.disease";
	//public static final String MITOMAP_DIS_tRNArRNA = "mitomap.tRNArRNA.disease";
	public static final String MITOMAP_HETEROPLASMY = "mitomap.heteroplasmy";//added just in case (currently not implemented)
	public static final String UK10K_ALLELE_FREQ= "uk10k.frequency";
	
	public static final String AF = "AF";
	public static final String POP_ALT = "pop.alt";
	//added for ScSNV
	public static final String scSNV_ada = "scSNV.ada_score";
	public static final String scSNV_rf = "scSNV.rf_score";

	//added for clinvar
	public static final String CLNSIG = "clinvar.clnsig"; 			//(clinical significance)
	public static final String CLNDBN = "clinvar.clndbn"; 			//(disease name)
	public static final String CLNDSDBID = "clinvar.clndsdbid"; 	//(database id)
	public static final String CLNREVSTAT = "clinvar.clnrevstat"; 	//(review status )
	public static final String CLNDSDB = "clinvar.clndsdb"; 		//(clinical database)

	public static final String ARUP_HET_COUNT = "arup.het.count";
	public static final String ARUP_HOM_COUNT = "arup.hom.count";
	public static final String ARUP_SAMPLE_COUNT = "arup.sample.count";


	//added for IonTorrentParser
	public static final String VAR_FREQ = "Var.Freq";

	
	
	public static final String EXAC63K_OVERALL_FREQ_HET = "exac63K.overall.het.freq";
	public static final String EXAC63K_OVERALL_FREQ_HOM = "exac63K.overall.hom.freq";
	public static final String EXAC63K_OVERALL_FREQ_HEMI = "exac63K.overall.hemi.freq";
	public static final String EXAC63K_OVERALL_HET_HOM   = "exac63K.overall.het.hom";
	public static final String BAD_REGION = "bad.region";
	public static final String LOW_COMPLEX_REGION = "low.complex";
	
	//ExAC annotations
	public static final String EXAC63K_VERSION = "exac63K.version";
	
	//overall
	public static final String EXAC63K_OVERALL_ALLELE_COUNT = "exac63K.overall.allele.count";
	public static final String EXAC63K_OVERALL_ALLELE_NUMBER = "exac63K.overall.allele.number";
	public static final String EXAC63K_OVERALL_HOM_COUNT = "exac63K.overall.hom.count";
	public static final String EXAC63K_OVERALL_ALLELE_FREQ = "exac63K.overall.allele.freq";
	public static final String EXAC63K_OVERALL_HEMI_COUNT = "exac63K.overall.hemi.count";
	
	
	//African
	public static final String EXAC63K_AFRICAN_ALLELE_COUNT = "exac63K.african.allele.count";
	public static final String EXAC63K_AFRICAN_ALLELE_NUMBER = "exac63K.african.allele.number";
	public static final String EXAC63K_AFRICAN_HOM_COUNT = "exac63K.african.hom.count";
	public static final String EXAC63K_AFRICAN_ALLELE_FREQ = "exac63K.african.allele.freq";
	public static final String EXAC63K_AFRICAN_HEMI_COUNT = "exac63K.african.hemi.count";


	//Latino
	public static final String EXAC63K_LATINO_ALLELE_COUNT = "exac63K.latino.allele.count";
	public static final String EXAC63K_LATINO_ALLELE_NUMBER = "exac63K.latino.allele.number";
	public static final String EXAC63K_LATINO_HOM_COUNT = "exac63K.latino.hom.count";
	public static final String EXAC63K_LATINO_ALLELE_FREQ = "exac63K.latino.allele.freq";
	public static final String EXAC63K_LATINO_HEMI_COUNT = "exac63K.latino.hemi.count";


	//East Asian
	public static final String EXAC63K_EASTASIAN_ALLELE_COUNT = "exac63K.eastasian.allele.count";
	public static final String EXAC63K_EASTASIAN_ALLELE_NUMBER = "exac63K.eastasian.allele.number";
	public static final String EXAC63K_EASTASIAN_HOM_COUNT = "exac63K.eastasian.hom.count";
	public static final String EXAC63K_EASTASIAN_ALLELE_FREQ = "exac63K.eastasian.allele.freq";
	public static final String EXAC63K_EASTASIAN_HEMI_COUNT = "exac63K.eastasian.hemi.count";


	//Finnish
	public static final String EXAC63K_EUR_FINNISH_ALLELE_COUNT = "exac63K.eur-finnish.allele.count";
	public static final String EXAC63K_EUR_FINNISH_ALLELE_NUMBER = "exac63K.eur-finnish.allele.number";
	public static final String EXAC63K_EUR_FINNISH_HOM_COUNT = "exac63K.eur-finnish.hom.count";
	public static final String EXAC63K_EUR_FINNISH_ALLELE_FREQ = "exac63K.eur-finnish.allele.freq";
	public static final String EXAC63K_EUR_FINNISH_HEMI_COUNT = "exac63K.eur-finnish.hemi.count";


	//Non-Finnish Europeans
	public static final String EXAC63K_EUR_NONFINNISH_ALLELE_COUNT = "exac63K.eur-nonfinnish.allele.count";
	public static final String EXAC63K_EUR_NONFINNISH_ALLELE_NUMBER = "exac63K.eur-nonfinnish.allele.number";
	public static final String EXAC63K_EUR_NONFINNISH_HOM_COUNT = "exac63K.eur-nonfinnish.hom.count";
	public static final String EXAC63K_EUR_NONFINNISH_ALLELE_FREQ = "exac63K.eur-nonfinnish.allele.freq";
	public static final String EXAC63K_EUR_NONFINNISH_HEMI_COUNT = "exac63K.eur-nonfinnish.hemi.count";


	//South Asian
	public static final String EXAC63K_SOUTHASIAN_ALLELE_COUNT = "exac63K.southasian.allele.count";
	public static final String EXAC63K_SOUTHASIAN_ALLELE_NUMBER = "exac63K.southasian.allele.number";
	public static final String EXAC63K_SOUTHASIAN_HOM_COUNT = "exac63K.southasian.hom.count";
	public static final String EXAC63K_SOUTHASIAN_ALLELE_FREQ = "exac63K.southasian.allele.freq";
	public static final String EXAC63K_SOUTHASIAN_HEMI_COUNT = "exac63K.southasian.hemi.count";


	//Other populations
	public static final String EXAC63K_OTHER_ALLELE_COUNT = "exac63K.other.allele.count";
	public static final String EXAC63K_OTHER_ALLELE_NUMBER = "exac63K.other.allele.number";
	public static final String EXAC63K_OTHER_HOM_COUNT = "exac63K.other.hom.count";
	public static final String EXAC63K_OTHER_ALLELE_FREQ = "exac63K.other.allele.freq";
	public static final String EXAC63K_OTHER_HEMI_COUNT = "exac63K.other.hemi.count";
	
   public static final String SNPEFF_ALL = "snpeff.all";
   public static final String INDEL_LENGTH = "indel.length";
   public static final String SV_END = "sv.end";
   public static final String SV_IMPRECISE = "sv.imprecise";

   public static final String MNP_ALLELES = "mnp.alleles";
   
   public static final String VAL_AF = "val.af";
   public static final String MEAN_VAL_AF = "mean.val.af";
   public static final String MAX_VAL_AF = "max.val.af";

   public static final String PINDEL_ORIG_REF = "pindel.orig.ref";
   public static final String PINDEL_ORIG_ALT = "pindel.orig.alt";

   public static final String LITHIUM_SNV_SCORE = "lithium.snv.score";
   public static final String LITHIUM_INS_SCORE = "lithium.ins.score";
   public static final String LITHIUM_DEL_SCORE = "lithium.del.score";

}

