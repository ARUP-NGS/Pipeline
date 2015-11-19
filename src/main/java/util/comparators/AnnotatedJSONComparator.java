package util.comparators;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
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
 * @author kevin
 *
 */
public class AnnotatedJSONComparator extends Comparator  {

	private Integer numberOfVarComparisons = 0;
		
	private Map<String, Integer> droppedAnnos = new HashMap<String, Integer>();
	private Map<String, Integer> gainedAnnos = new HashMap<String, Integer>();
	
	private Map<String, Integer> discordanceTally = new HashMap<String, Integer>();
	//private Map<String, Double> freqDiscordanceTotals = new HashMap<String, Double>();

	public AnnotatedJSONComparator() {
	}
	
	public AnnotatedJSONComparator(ReviewDirectory rd1, ReviewDirectory rd2, String analysisHeader) {
		super(rd1, rd2, analysisHeader);
		this.summaryTable.setColNames(Arrays.asList("Dropped","Gained","Changed",""));
	}

	@Override
	void performComparison() throws IOException, JSONException {
		VariantPool vp1 = rd1.getVariantsFromJSON();
		VariantPool vp2 = rd2.getVariantsFromJSON();

		List<String> annotations = Arrays.asList(VariantRec.GENE_NAME, 
				VariantRec.RSNUM,
				VariantRec.HGMD_HIT,
				VariantRec.HGMD_INFO,
				VariantRec.POP_FREQUENCY,
				VariantRec.EXOMES_63K_FREQ,
				VariantRec.ARUP_FREQ,
				VariantRec.CDOT,
				VariantRec.PDOT);
	
		for (String annotation : annotations) {
			droppedAnnos.put(annotation, 0);
			gainedAnnos.put(annotation, 0);
			discordanceTally.put(annotation, 0);
		}
		
		//Loop through Variant Pools
		for(String contig : vp1.getContigs()) {
			for(VariantRec refVar : vp1.getVariantsForContig(contig)) {
				//Find this variant in other record and them compare them...
				VariantRec compareVar = vp2.findRecord(refVar.getContig(), refVar.getStart(), refVar.getRef(), refVar.getAlt());
				if (compareVar != null) {
					this.numberOfVarComparisons += 1;
					
					this.compareSimpleAnnotations(refVar, compareVar, VariantRec.GENE_NAME);
					this.compareSimpleAnnotations(refVar, compareVar, VariantRec.RSNUM);
					this.compareSimpleAnnotations(refVar, compareVar, VariantRec.HGMD_HIT);
					this.compareSimpleAnnotations(refVar, compareVar, VariantRec.HGMD_INFO);
					
					this.compareFreqAnnotations(refVar, compareVar, VariantRec.POP_FREQUENCY);
					this.compareFreqAnnotations(refVar, compareVar, VariantRec.EXOMES_63K_FREQ);
					this.compareFreqAnnotations(refVar, compareVar, VariantRec.ARUP_FREQ);
					
					this.compareComplexAnnotation(refVar, compareVar, VariantRec.CDOT);
					this.compareComplexAnnotation(refVar, compareVar, VariantRec.PDOT);
					
				} else {
					continue;
				}				
			}
		}
		
		//this.addNewSummaryEntry("variant.comparisons", "Variants Compared", String.valueOf(this.numberOfVarComparisons), "");
		this.populateEntries();
	}
	
	private void populateEntries() {
		
		for (Map.Entry<String, Integer> entry : discordanceTally.entrySet()) {
		    String key     = entry.getKey();
		    Integer value  = entry.getValue();
		  //this.compareNumberNotes(value.doubleValue(), this.numberOfVarComparisons.doubleValue(), false, key));
		    String jsonKey = key + ".discordance";
		    String rowName = "\"" + key + "\" discordance";
		    String dropped = String.valueOf(droppedAnnos.get(key));
		    String gained  = String.valueOf(gainedAnnos.get(key));
		    this.addNewAnnotationSummaryEntry(jsonKey, rowName, dropped, gained, String.valueOf(value), this.numberOfVarComparisons.toString(), ComparisonType.ANNOTATIONS);
		}
	}
	
	/** Simple equals is sufficient to compare (Gene, DBSNP id, HGMD hit)
	 * @param var1
	 * @param var2
	 */
	private void compareSimpleAnnotations(VariantRec var1, VariantRec var2, String annotation) {
		String v1Annotation = var1.getAnnotation(annotation);
		String v2Annotation = var2.getAnnotation(annotation);
		if (v1Annotation != null && v2Annotation == null) { //Dropped an annotation
			droppedAnnos.put(annotation, droppedAnnos.get(annotation) + 1);
		} else if (v1Annotation == null && v2Annotation != null) { //gained an annotation
			gainedAnnos.put(annotation, gainedAnnos.get(annotation) + 1);
		} else if (v1Annotation != null && v2Annotation != null) {
			if (!v1Annotation.equals(v2Annotation)) {
				this.discordanceTally.put(annotation, discordanceTally.get(annotation) + 1);
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
		if (v1Annotation != null && v2Annotation == null) { //Dropped an annotation
			droppedAnnos.put(annotation, droppedAnnos.get(annotation) + 1);
		} else if (v1Annotation == null && v2Annotation != null) { //gained an annotation
			gainedAnnos.put(annotation, gainedAnnos.get(annotation) + 1);
		} else if (v1Annotation != null && v2Annotation != null) {
			if (v1Annotation != v2Annotation) {
				this.discordanceTally.put(annotation, discordanceTally.get(annotation) + 1);
			}
		} else {
			//throw new NullPointerException();
			//both null
		}
		
		//Double freqDiff = Math.abs(var1.getProperty(annotation) - var2.getProperty(annotation));
		//this.discordanceMax.put(annotation, Math.max(discordanceMax.get(annotation), freqDiff));
		//this.freqDiscordanceTotals.put(annotation, this.freqDiscordanceTotals.get(annotation) + freqDiff);
	}
	
	/** Complex comparisons for annotations that could be drastically different in normal use (cdot, pdot due to annovar vs snpeff).
	 * @param var1
	 * @param var2
	 */
	private void compareComplexAnnotation(VariantRec var1, VariantRec var2, String annotation) {
		//AnnovarSnpEffComparator cdotCompare = new AnnovarSnpEffComparator();
		//Trying to use the comparator already written.
	}
}
