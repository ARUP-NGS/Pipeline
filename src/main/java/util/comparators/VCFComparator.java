package util.comparators;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import buffer.variant.VariantPool;
import json.JSONException;
import util.reviewDir.ReviewDirectory;


/** Class which compares two vcfs of two given review directories. It basically focuses on comparing variant existence and not
 *  so much the QC or other information for them... Could easily be extended to do so though.
 *  
 * @author kevin
 *
 */
public class VCFComparator extends ReviewDirComparator {

	public VCFComparator(ReviewDirectory rd1, ReviewDirectory rd2, String analysisHeader) {
		super(rd1, rd2, analysisHeader);
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

		this.addNewEntry("vp.size", "Size of VP", vp1Size, vp2Size, compareNumberNotes(Double.valueOf(vp1Size), Double.valueOf(vp2Size), true, "vp.size"));
		this.addNewEntry("snps", "SNPs", vp1SNPS, vp2SNPS, compareNumberNotes(Double.valueOf(vp1SNPS), Double.valueOf(vp2SNPS), true, "snps"));
		this.addNewEntry("insertions", "Insertions", vp1Ins, vp2Ins, compareNumberNotes(Double.valueOf(vp1Ins), Double.valueOf(vp2Ins), true, "insertions"));
		this.addNewEntry("deletions", "Deletions", vp1Del, vp2Del, compareNumberNotes(Double.valueOf(vp1Del), Double.valueOf(vp2Del), true, "deletions"));
		this.addNewEntry("het.percent", "Het Percentage", String.format("%.1f", vp1HetPercent), String.format("%.1f", vp2HetPercent), compareNumberNotes(vp1HetPercent, vp2HetPercent, true, "het.percent"));
		this.addNewEntry("average.quality", "Average Quality", String.format("%.1f", vp1AvgQual), String.format("%.1f", vp2AvgQual), compareNumberNotes(vp1AvgQual, vp2AvgQual, true, "average.quality"));
	}

	void compareIntersection(VariantPool vp1, VariantPool vp2) {
		this.addNewSummaryEntry("intersection", "Number of Intersecting Variants",String.valueOf(vp1.intersect(vp2).size()) + "/" + String.valueOf(Math.max(vp1.size(), vp2.size())), "");

		//Break unique variants out by type because maybe that is helpful.
		VariantPool vp1Sub2 = vp1.subtract(vp2);
		String vp1Sub2SNPS = String.valueOf(vp1Sub2.countSNPs());
		String vp1Sub2Ins = String.valueOf(vp1Sub2.countInsertions());
		String vp1Sub2Del = String.valueOf(vp1Sub2.countDeletions());
		StringBuilder vp1Sub2Note = new StringBuilder("");
		if (vp1Sub2.size() > 0) {
			vp1Sub2Note.append("SNPs: " + vp1Sub2SNPS + " | ");
			vp1Sub2Note.append("Ins: " + vp1Sub2Ins + " | ");
			vp1Sub2Note.append("Del: " + vp1Sub2Del);
		}

		this.addNewSummaryEntry("unique.truth", "Variants Unique to " + rd1.getSampleName(), String.valueOf(vp1Sub2.size()), vp1Sub2Note.toString());

		VariantPool vp2Sub1 = vp2.subtract(vp1);
		String vp2Sub1SNPS = String.valueOf(vp2Sub1.countSNPs());
		String vp2Sub1Ins = String.valueOf(vp2Sub1.countInsertions());
		String vp2Sub1Del = String.valueOf(vp2Sub1.countDeletions());
		StringBuilder vp2Sub1Note = new StringBuilder("");
		if (vp2Sub1.size() > 0) {
			vp2Sub1Note.append("SNPs: " + vp2Sub1SNPS + " | ");
			vp2Sub1Note.append("Ins: " + vp2Sub1Ins + " | ");
			vp2Sub1Note.append("Del: " + vp2Sub1Del);
		}

		this.addNewSummaryEntry("unique.test", "Variants Unique to " + rd2.getSampleName(), String.valueOf(vp2Sub1.size()), vp2Sub1Note.toString());
	}

	void intersectVariantPools() {
		return;
	}
}
