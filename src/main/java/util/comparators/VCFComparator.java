package util.comparators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import buffer.variant.VariantPool;
import buffer.variant.VariantRec;
import buffer.variant.VarFilterUtils;
import json.JSONException;
import util.comparators.CompareReviewDirs.ComparisonType;
import util.reviewDir.ReviewDirectory;


/** Class which compares two vcfs of two given review directories. It basically focuses on comparing variant existence and not
 *  so much the QC or other information for them... Could easily be extended to do so though.
 *  
 * @author kevin
 *
 */
public class VCFComparator extends Comparator {

	public VCFComparator(ReviewDirectory rd1, ReviewDirectory rd2, String analysisHeader) {
		super(rd1, rd2);
		super.summaryTable = new ComparisonSummaryTable(analysisHeader, Arrays.asList("TRUTH", "TEST", "Notes"));
	}

	@Override
	void performComparison() throws IOException, JSONException {

		//Read the two vcfs into VariantPools and then lets intersect the variantpools.
		VariantPool vp1 = rd1.getVariantsFromVCF();
		VariantPool vp2 = rd2.getVariantsFromVCF();
		
		this.compareVariants(vp1, vp2);
		this.compareIntersection(vp1, vp2);
	}
	
	private void compareVariants(VariantPool vp1, VariantPool vp2) {
		String vp1Size = String.valueOf(vp1.size());
		String vp1SNPS = String.valueOf(vp1.countSNPs());
		String vp1Ins = String.valueOf(vp1.countInsertions());
		String vp1Del = String.valueOf(vp1.countDeletions());
		Double vp1HetPercent = (double) 100.0*(vp1.countHeteros()/vp1.size());
		Double vp1AvgQual    = vp1.meanQuality();

		String vp2Size = String.valueOf(vp2.size());
		String vp2SNPS = String.valueOf(vp2.countSNPs());
		String vp2Ins = String.valueOf(vp2.countInsertions());
		String vp2Del = String.valueOf(vp2.countDeletions());
		Double vp2HetPercent = (double) 100*(vp2.countHeteros()/vp2.size());
		Double vp2AvgQual    = vp2.meanQuality();
		
		// Compare classified variants 
		List<VariantRec> vp1ClassifiedVariants = vp1.filterPool(VarFilterUtils.getfilterFilter());
		VariantPool classifiedVariant = new VariantPool(vp1ClassifiedVariants);
		VariantPool testCalledVariant = classifiedVariant.intersect(vp2);
		String classifiedVariantSize = String.valueOf(classifiedVariant.size());
 		String testCalledVariantSize = String.valueOf(testCalledVariant.size());
		
		
		this.addNewEntry("vp.size", "Size of VP", vp1Size, vp2Size, ComparisonType.EXACTNUMBERS);
		this.addNewEntry("vp.classified.variants", "Classified Variants", classifiedVariantSize, testCalledVariantSize, ComparisonType.EXACTNUMBERS);
		this.addNewEntry("snps", "SNPs", vp1SNPS, vp2SNPS, ComparisonType.EXACTNUMBERS);
		this.addNewEntry("insertions", "Insertions", vp1Ins, vp2Ins, ComparisonType.EXACTNUMBERS);
		this.addNewEntry("deletions", "Deletions", vp1Del, vp2Del, ComparisonType.EXACTNUMBERS);
		this.addNewEntry("het.percent", "Het Percentage", String.format("%.1f", vp1HetPercent), String.format("%.1f", vp2HetPercent), ComparisonType.TWONUMBERS);
		this.addNewEntry("average.quality", "Average Quality", String.format("%.1f", vp1AvgQual), String.format("%.1f", vp2AvgQual), ComparisonType.TWONUMBERS);
	}

	void compareIntersection(VariantPool vp1, VariantPool vp2) {

		//Break unique variants out by type because maybe that is helpful.
		VariantPool vp1Sub2 = vp1.subtract(vp2);
		VariantPool vp2Sub1 = vp2.subtract(vp1);

		String varSNPS = String.valueOf(vp1Sub2.countSNPs() + vp2Sub1.countSNPs());
		String varINS = String.valueOf(vp1Sub2.countInsertions() + vp2Sub1.countInsertions());
		String varDEL = String.valueOf(vp1Sub2.countDeletions() + vp2Sub1.countDeletions());
		StringBuilder varTypeNotes = new StringBuilder("");
		
		if (vp1Sub2.size() > 0 || vp2Sub1.size() > 0) {
			varTypeNotes.append("SNPs: " + varSNPS + " | ");
			varTypeNotes.append("Ins: " + varINS + " | ");
			varTypeNotes.append("Del: " + varDEL);
		}
		
		//varTypeNotes.toString()
		this.addNewEntry("unique.variants", "Unique variants", String.valueOf(vp1Sub2.size()), String.valueOf(vp2Sub1.size()), ComparisonType.VARIANTS);
		vp1Sub2.addAll(vp2Sub1);
		StringBuilder sb = new StringBuilder();
		for (VariantRec rec : vp1Sub2.toList()) {
			sb.append("\t" + rec.toSimpleString() + "\n");
		}		
		this.summaryTable.failedVariants.put("Unique variants", sb.toString());
	
	}

	void intersectVariantPools() {
		return;
	}
}
