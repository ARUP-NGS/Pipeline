package util.varFreqDB;

import java.io.IOException;

import util.varFreqDB.ComputeVarFreqs.SampleInfo;
import util.vcfParser.VCFParser.GTType;
import buffer.variant.VariantRec;
import buffer.variant.VariantStore;

public class VarCountCalculator implements Runnable {
	
	final SampleInfo info;
	final VariantStore everything;
	
	public VarCountCalculator(SampleInfo info, VariantStore everything) {
		this.info = info;
		this.everything = everything;
	}
	
	@Override
	public void run() {
		String typeKey = info.analysisType;
		VariantStore pool;
		System.err.println("Running " + info.source.getName());
		try {
			pool = info.getPool();
		} catch (IOException e) {
			System.err.println("Could not load pool");
			return;
			
		}
		for(String contig: everything.getContigs()) {
			for(VariantRec var : everything.getVariantsForContig(contig)) {

				//Is this variant targeted for this sample?
				boolean targeted = info.bed.contains(var.getContig(), var.getStart(), false);

				if (targeted) {
					incrementProperty(var, typeKey+ComputeVarFreqs.SAMPLES);
					incrementProperty(var, ComputeVarFreqs.SAMPLES);

					VariantRec queryVar = pool.findRecord(contig, var.getStart(), var.getRef(), var.getAlt());

					if (queryVar != null) {
						if (queryVar.getZygosity() == GTType.HET) {
							incrementProperty(var, typeKey+ComputeVarFreqs.HETS);
							incrementProperty(var, ComputeVarFreqs.HETS);
						}
						else {
							incrementProperty(var, typeKey+ComputeVarFreqs.HOMS);
							incrementProperty(var, ComputeVarFreqs.HOMS);
						}
					}
				}


			}
		}
		info.disposePool();
		System.err.println(info.source.getName() + " is done");
	}
	
	static synchronized void incrementProperty(VariantRec var, String key) {
		Double current = var.getProperty(key);
		if (current == null) {
			current = 0.0;
		}
		current++;
		var.addProperty(key, current);
	}
}
