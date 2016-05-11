package util.comparators;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import buffer.variant.VariantPool;
import buffer.variant.VariantRec;
import json.JSONException;
import util.comparators.CompareReviewDirs.ComparisonType;
import util.reviewDir.ReviewDirectory;

/** *Variant annotation comparison (from annotated.json.gz files – will require parsing / reading that data into a buffer.variant.VariantPool object)
	For each variant, check to see if the following annotations are the same, and warn / flag if not:
	Gene
	Cdot
	Pdot
	Dbsnp id (referred to as ‘rsnum’ internally)
	Pop.freq
	Exac.freq
	HGMD hit
	//Additional annotations can be added to the annotationsToCompare object.
 * @author kevin
 */
public class AnnotatedJSONComparator extends Comparator  {

	private Map<String, Integer> droppedAnnos = new HashMap<String, Integer>();
	private Map<String, Integer> gainedAnnos = new HashMap<String, Integer>();

	private Map<String, Integer> discordanceTally = new HashMap<String, Integer>();

	private int nonIntronicVariants = 0;

	private enum annotationType {
		SIMPLE, FREQ, COMPLEX
	}

	//ADD ALL NEW ANNOTATIONS TO COMPARE TO THIS MAP.
	private static final Map<String, annotationType> annotationsToCompare;
	static {
		LinkedHashMap<String, annotationType> aMap = new LinkedHashMap<String, annotationType>();
		aMap.put(VariantRec.GENE_NAME, annotationType.SIMPLE);
		aMap.put(VariantRec.NM_NUMBER, annotationType.SIMPLE);
		aMap.put(VariantRec.RSNUM, annotationType.SIMPLE);
		aMap.put(VariantRec.HGMD_HIT, annotationType.SIMPLE);
		aMap.put(VariantRec.HGMD_INFO, annotationType.SIMPLE);
		aMap.put(VariantRec.MITOMAP_ALLELE_ID, annotationType.SIMPLE);
		aMap.put(VariantRec.MITOMAP_DIS_CODING, annotationType.SIMPLE);
		aMap.put(VariantRec.MITOMAP_HETEROPLASMY, annotationType.SIMPLE);
		aMap.put(VariantRec.COSMIC_COUNT, annotationType.SIMPLE);
		aMap.put(VariantRec.COSMIC_ID, annotationType.SIMPLE);
		
		//dbnsfp
		aMap.put(VariantRec.SIFT_SCORE, annotationType.SIMPLE);
		aMap.put(VariantRec.POLYPHEN_SCORE, annotationType.SIMPLE);
		aMap.put(VariantRec.POLYPHEN_HVAR_SCORE, annotationType.SIMPLE);
		aMap.put(VariantRec.LRT_SCORE, annotationType.SIMPLE);
		aMap.put(VariantRec.MT_SCORE, annotationType.SIMPLE);
		aMap.put(VariantRec.MA_SCORE, annotationType.SIMPLE);
		aMap.put(VariantRec.GERP_NR_SCORE, annotationType.SIMPLE);
		aMap.put(VariantRec.GERP_SCORE, annotationType.SIMPLE);
		aMap.put(VariantRec.PHYLOP_SCORE, annotationType.SIMPLE);
		aMap.put(VariantRec.SIPHY_SCORE, annotationType.SIMPLE);
		aMap.put(VariantRec.PHYLOP_SCORE, annotationType.SIMPLE);
		
		
		aMap.put(VariantRec.POP_FREQUENCY, annotationType.FREQ);
		aMap.put(VariantRec.EXAC63K_OVERALL_FREQ_HET, annotationType.FREQ);
		aMap.put(VariantRec.EXOMES_FREQ, annotationType.FREQ);
		aMap.put(VariantRec.EXOMES_HOM_FREQ, annotationType.FREQ);
		aMap.put(VariantRec.ARUP_FREQ, annotationType.FREQ);
		aMap.put(VariantRec.ARUP_FREQ, annotationType.FREQ);
		aMap.put(VariantRec.MITOMAP_FREQ, annotationType.FREQ);
		
		
		aMap.put(VariantRec.CDOT, annotationType.COMPLEX);
		aMap.put(VariantRec.PDOT, annotationType.COMPLEX);
		annotationsToCompare = Collections.unmodifiableMap(aMap);
	}

	public AnnotatedJSONComparator() {
	}

	public AnnotatedJSONComparator(ReviewDirectory rd1, ReviewDirectory rd2, String analysisHeader) {
		super(rd1, rd2);
		super.summaryTable = new ComparisonSummaryTable(analysisHeader, Arrays.asList("", "", ""));
	}

	private void initializeMaps() {
		for (String annotation : annotationsToCompare.keySet()) {
			droppedAnnos.put(annotation, 0);
			gainedAnnos.put(annotation, 0);
			discordanceTally.put(annotation, 0);
			this.summaryTable.failedVariants.put(annotation, "");
		}
	}
	
	private void populateEntries() {

		for (Map.Entry<String, Integer> entry : discordanceTally.entrySet()) {
			String key     = entry.getKey();
			Integer changed= entry.getValue();
			//this.compareNumberNotes(value.doubleValue(), this.numberOfVarComparisons.doubleValue(), false, key));
			String jsonKey = key + ".discordance";
			String rowName = key;
			String dropped = String.valueOf(droppedAnnos.get(key));
			String gained  = String.valueOf(gainedAnnos.get(key));

			this.addNewEntry(jsonKey, rowName, dropped + " | " + gained, String.valueOf(changed), ComparisonType.ANNOTATIONS);
		}
	}
	
	@Override
	void performComparison() throws IOException, JSONException {
		VariantPool vp1 = rd1.getVariantsFromJSON();
		VariantPool vp2 = rd2.getVariantsFromJSON();

		this.initializeMaps();

		//Loop through Variant Pools
		for(String contig : vp1.getContigs()) {
			for(VariantRec refVar : vp1.getVariantsForContig(contig)) {
				//Find this variant in other record and them compare them...
				VariantRec compareVar = vp2.findRecord(refVar.getContig(), refVar.getStart(), refVar.getRef(), refVar.getAlt());
				if (compareVar != null) {
					super.annotationsCompared += 1;
					if (!compareVar.getAnnotation(VariantRec.VARIANT_TYPE).contains("inter") && 
							!compareVar.getAnnotation(VariantRec.VARIANT_TYPE).contains("stream") &&
							!compareVar.getAnnotation(VariantRec.VARIANT_TYPE).contains("intron")) { //intergenic or up/down stream check.
						nonIntronicVariants += 1;
					}

					for (Map.Entry<String, annotationType> entry : annotationsToCompare.entrySet()) {
						String annotationKey = entry.getKey();
						annotationType annoType = entry.getValue();

						switch (annoType) {
						case SIMPLE:
							this.compareSimpleAnnotations(refVar, compareVar, annotationKey);
							break;
						case FREQ:
							this.compareFreqAnnotations(refVar, compareVar, annotationKey);
							break;
						case COMPLEX:
							this.compareComplexAnnotation(refVar, compareVar, annotationKey);
							break;
						default:
							break;
						}
					}
				} else {
					continue;
				}				
			}
		}
		System.out.println("Sample contains " + String.valueOf(nonIntronicVariants) + "/" + String.valueOf(super.annotationsCompared) + " non intronic, intragenic, up/down stream.");
		//this.summaryTable.setColNames(Arrays.asList("Dropped | Gained","Changed (out of " + String.valueOf(super.annotationsCompared) + ")","Notes"));
		this.summaryTable.setColNames(Arrays.asList("Dropped | Gained","Changed" + String.valueOf(""),"Notes"));

		this.populateEntries();
	}

	/** Simple equals is sufficient to compare (Gene, DBSNP id, HGMD hit)
	 * @param var1
	 * @param var2
	 */
	private void compareSimpleAnnotations(VariantRec var1, VariantRec var2, String annotation) {
		String v1Annotation = var1.getAnnotation(annotation);
		String v2Annotation = var2.getAnnotation(annotation);
		boolean v1AnnotationExists = false;
		boolean v2AnnotationExists = false;

		if(v1Annotation != null && !v1Annotation.isEmpty()) {
			v1AnnotationExists = true;
		}
		if(v2Annotation != null && !v2Annotation.isEmpty()) {
			v2AnnotationExists = true;
		}

		if (v1AnnotationExists && !v2AnnotationExists) { //Dropped an annotation
			droppedAnnos.put(annotation, droppedAnnos.get(annotation) + 1);
			if (this.droppedAnnos.get(annotation) < 5) {
				this.summaryTable.droppedVariants.put(annotation, this.summaryTable.droppedVariants.get(annotation) + "\n\t" + this.annotationCompareString(var1, var2, annotation));
			}
		} else if (!v1AnnotationExists && v2AnnotationExists) { //gained an annotation
			gainedAnnos.put(annotation, gainedAnnos.get(annotation) + 1);
		} else if (v1AnnotationExists && v2AnnotationExists) {
			if (!v1Annotation.equals(v2Annotation)) {
				this.discordanceTally.put(annotation, discordanceTally.get(annotation) + 1);
				if (this.discordanceTally.get(annotation) < 5) {
					this.summaryTable.failedVariants.put(annotation, this.summaryTable.failedVariants.get(annotation) + "\n\t" + this.annotationCompareString(var1, var2, annotation));
				}
			}
		} else {
			//throw new NullPointerException();
			//both null
		}
	}
	
	/** Comparing two frequencies, where there might be slight differences (pop freq, exac Freq, ARUP freq)
	 * @param var1
	 * @param var2
	 */
	private void compareFreqAnnotations(VariantRec var1, VariantRec var2, String annotation) {

		Double v1Annotation = var1.getProperty(annotation);
		Double v2Annotation = var2.getProperty(annotation);
		boolean v1AnnotationExists = false;
		boolean v2AnnotationExists = false;

		if(v1Annotation != null) {
			v1AnnotationExists = true;
		}
		if(v2Annotation != null) {
			v2AnnotationExists = true;
		}

		if (v1AnnotationExists && !v2AnnotationExists) { //Dropped an annotation
			droppedAnnos.put(annotation, droppedAnnos.get(annotation) + 1);
			if (this.droppedAnnos.get(annotation) < 5) {
				this.summaryTable.droppedVariants.put(annotation, this.summaryTable.droppedVariants.get(annotation) + "\n\t" + this.annotationCompareString(var1, var2, annotation));
			}
		} else if (!v1AnnotationExists && v2AnnotationExists) { //gained an annotation
			gainedAnnos.put(annotation, gainedAnnos.get(annotation) + 1);
		} else if (v1AnnotationExists && v2AnnotationExists) {
			if (Math.abs(v1Annotation-v2Annotation) > 0.00001) {
				this.discordanceTally.put(annotation, discordanceTally.get(annotation) + 1);
				if (this.discordanceTally.get(annotation) < 5) {
					this.summaryTable.failedVariants.put(annotation, this.summaryTable.failedVariants.get(annotation) + "\n\t" + this.annotationCompareString(var1, var2, annotation));
				}
			}
		} else {
			//throw new NullPointerException();
			//both null
		}
	}

	/** Complex comparisons for annotations that could be drastically different in normal use (cdot, pdot due to annovar vs snpeff).
	 * @param var1
	 * @param var2
	 */
	private void compareComplexAnnotation(VariantRec var1, VariantRec var2, String annotation) {
		//AnnovarSnpEffComparator cdotCompare = new AnnovarSnpEffComparator();
		//Trying to use the comparator already written.
		//Just use a simple string comparison for now.. Only useful for comparing apples to apples (same annotators).
		compareSimpleAnnotations(var1, var2, annotation);
	}
	
	/** Function taking two variants and an annotation and producing a small summary string comparing the two. This allows for a quick view of how different the annotations are.
	 * @param var1
	 * @param var2
	 * @param annotation
	 * @return
	 */
	private String annotationCompareString(VariantRec var1, VariantRec var2, String annotation) {
		StringBuilder compareString = new StringBuilder();
		
		compareString.append(var1.getContig());
		compareString.append("|");
		compareString.append(var2.getContig());
		compareString.append("\t");
		
		compareString.append(var1.getStart());
		compareString.append("|");
		compareString.append(var2.getStart());
		compareString.append("\t");
		
		compareString.append(var1.getPropertyOrAnnotation(annotation));
		compareString.append("|");
		compareString.append(var2.getPropertyOrAnnotation(annotation));
		
		return compareString.toString();
	}
}
