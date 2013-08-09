package util.varFreqDB;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import util.VCFLineParser;
import util.reviewDir.ReviewDirInfo;
import util.reviewDir.ReviewDirParseException;
import buffer.BEDFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class CompareVarFreqs {

	//Map from analysis type to collection of variants
	public static final String SAMPLES = "samples";
	public static final String HETS = "hets";
	public static final String HOMS = "homs";
	
	List<PoolInfo> allVars = new ArrayList<PoolInfo>();
	
	public boolean addSample(ReviewDirInfo info) throws IOException {
		System.err.println("Adding sample : " + info.getSampleName() + " :" + info.getAnalysisType());
		File vcf = info.getVCF();
		File bed = info.getBED();
		String analysis = info.getAnalysisType();
		if (analysis.contains("(")) {
			analysis = analysis.substring(0, analysis.indexOf("(")).trim();
		}
		
		if (bed == null) {
			System.err.println("BED file is null for sample: " + info.getSampleName() + ", " + info.getAnalysisType());
			return false;
		}
		
		PoolInfo pInfo = new PoolInfo();
		pInfo.analysisType = info.getAnalysisType();
		pInfo.bed = new BEDFile(bed);
		pInfo.pool = new VariantPool(new VCFLineParser(vcf));
		allVars.add(pInfo);
		return true;
	}
	
	public void emitPool(PoolInfo poolInfo) {
		VariantPool pool = poolInfo.pool;
		System.out.println("##total.samples=" + poolInfo.totalSamples);
		for(String contig: pool.getContigs()) {
			for(VariantRec var : pool.getVariantsForContig(contig)) {
				System.out.println(contig + "\t" + var.getStart() + "\t" + var.getAlt() + "\t" + formatProperty(var.getPropertyOrAnnotation(HETS)) + "\t" + formatProperty(var.getPropertyOrAnnotation(HOMS)));
			}
		}
	}
	
	private static String formatProperty(String prop) {
		if (prop.equals("-")) {
			return "0.0";
		}
		else 
			return prop;
	}
	
	public void readSamplesInDir(File dir) throws ReviewDirParseException, IOException {
		File[] files = dir.listFiles();
		for(int i=0; i<files.length; i++) {
			if (files[i].isDirectory()) {
				ReviewDirInfo info = ReviewDirInfo.create(files[i].getAbsolutePath());
				addSample(info);
			}
		}
		
	}
	
	
	
	public void emitTabulated() {
		
		//Make on giant pool...
		System.err.println("Tabulating variants, this may take a moment....");
		VariantPool everything = new VariantPool();
		for(PoolInfo poolInfo : allVars) {
			everything.addAll(poolInfo.pool, false); //Do not allow duplicates
		}
		
		System.out.println("#chr\tpos\tref\talt\tsamples.queried\thets\thoms\tfreq");
		
		for(String contig: everything.getContigs()) {
			for(VariantRec var : everything.getVariantsForContig(contig)) {
				System.out.print(contig + "\t" + var.getStart() + "\t" + var.getRef() + "\t" + var.getAlt());
				
				int totSamples = 0;
				int hets = 0;
				int homs = 0;
				
				for(PoolInfo info : allVars) {
					if (! info.bed.isMapCreated()) {
						try {
							info.bed.buildIntervalsMap();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (info.bed.contains(contig, var.getStart(), false)) {
						totSamples++;
					}
					
					VariantRec queryVar = info.pool.findRecordNoWarn(contig, var.getStart());
					if (queryVar != null) {
						if (queryVar.isHetero()) {
							hets++;
						}
						else {
							homs++;
						}
					}
				} //loop over allVars
				
				double freq = (hets + 2.0*homs)/(totSamples*2.0);
				String freqStr = "" + freq;
				if (freqStr.length() > 6) {
					freqStr = freqStr.substring(0, 6);
				}
				System.out.print("\t" + totSamples + "\t" + hets + "\t" + homs + "\t" + freqStr);
				System.out.println();	
			}
			
		}
		
		
	}
	
	public static void main(String[] args) {
		CompareVarFreqs cFreqs = new CompareVarFreqs();
		
		int added = 0;
		for(int i=0; i<args.length; i++) {
			
			try {
				boolean ok = cFreqs.addSample( ReviewDirInfo.create(args[i]));
				if (ok) {
					added++;
				}
			} catch (ReviewDirParseException e) {
				System.err.println("Warning: Skipping file : " + args[i]  + " : " + e.getLocalizedMessage());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		

		System.err.println("Found " + added + " valid samples");
		cFreqs.emitTabulated();
	}
	
	
	class PoolInfo {
		int totalSamples = 0;
		String analysisType = null;
		BEDFile bed = null;
		VariantPool pool = new VariantPool();
	}
	
}
