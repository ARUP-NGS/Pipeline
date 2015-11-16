package util.comparators;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import buffer.variant.VariantPool;
import buffer.variant.VariantRec;
import json.JSONException;
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
public class AnnotatedJSONComparator extends ReviewDirComparator  {

	private Integer numberOfVarComparisons = 0;
	
	private Map<String, Integer> simpleDiscordanceTally = new HashMap<String, Integer>();
	
	private Map<String, Integer> discordanceTally = new HashMap<String, Integer>();
	private Map<String, Double> discordanceMax = new HashMap<String, Double>();
	private Map<String, Double> freqDiscordanceTotals = new HashMap<String, Double>();

	public AnnotatedJSONComparator() {
	}
	
	public AnnotatedJSONComparator(ReviewDirectory rd1, ReviewDirectory rd2, String analysisHeader) {
		super(rd1, rd2, analysisHeader);
	}

	@Override
	void performComparison() throws IOException, JSONException {
		VariantPool vp1 = rd1.getVariantsFromJSON();
		VariantPool vp2 = rd2.getVariantsFromJSON();

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
		
		for (Map.Entry<String, Integer> entry : simpleDiscordanceTally.entrySet()) {
		    String key = entry.getKey();
		    Integer value = entry.getValue();
		  //this.compareNumberNotes(value.doubleValue(), this.numberOfVarComparisons.doubleValue(), false, key));
		    this.addNewSummaryEntry(key + ".discordance", "\"" + key + "\" discordance", String.valueOf(value), this.compareNumberNotes(value.doubleValue(), this.numberOfVarComparisons.doubleValue(), false, key, true));
		}
		
		for (Map.Entry<String, Integer> entry : discordanceTally.entrySet()) {
		    String key = entry.getKey();
		    Integer value = entry.getValue();
		    this.addNewSummaryEntry(key + ".discordance", "\"" + key + "\" discordance", String.valueOf(value), this.compareNumberNotes(value.doubleValue(), this.numberOfVarComparisons.doubleValue(), false, key, false));
		}
		
		for (Map.Entry<String, Double> entry : freqDiscordanceTotals.entrySet()) {
		    String key = entry.getKey();
		    Double value = entry.getValue();
			this.addNewSummaryEntry(key + ".discordance", "\"" + key + "\" avg. discordance", String.valueOf(value/this.numberOfVarComparisons), "");
		}
	}
	
	/** Simple equals is sufficient to compare (Gene, DBSNP id, HGMD hit)
	 * @param var1
	 * @param var2
	 */
	private void compareSimpleAnnotations(VariantRec var1, VariantRec var2, String annotation) {
		if (this.simpleDiscordanceTally.get(annotation) == null) {
			this.simpleDiscordanceTally.put(annotation, 0);
		}
		try {
			if (!var1.getAnnotation(annotation).equals(var2.getAnnotation(annotation))) {
				this.simpleDiscordanceTally.put(annotation, discordanceTally.get(annotation) + 1);
			}
		} catch (NullPointerException e) {
			e.getMessage();
		}
	}
	
	/** Comparing two frequencies, where there might be slight differences (pop freq, exac Freq, ARUP freq)
	 * @param var1
	 * @param var2
	 */
	private void compareFreqAnnotations(VariantRec var1, VariantRec var2, String annotation) {
		if (this.discordanceMax.get(annotation) == null) {
			this.discordanceMax.put(annotation, 0.0);
			this.freqDiscordanceTotals.put(annotation, 0.0);
		}
		
		try {
			Double freqDiff = Math.abs(var1.getProperty(annotation) - var2.getProperty(annotation));
			
			this.discordanceMax.put(annotation, Math.max(discordanceMax.get(annotation), freqDiff));
			
			this.freqDiscordanceTotals.put(annotation, this.freqDiscordanceTotals.get(annotation) + freqDiff);
		} catch (NullPointerException e) {
			e.getMessage();
		}
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
