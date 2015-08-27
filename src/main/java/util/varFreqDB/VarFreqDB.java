package util.varFreqDB;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.vcfParser.VCFParser.GTType;
import buffer.BEDFile;
import buffer.IntervalsFile;
import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;
import buffer.variant.VariantStore;

/**
 * In memory storage for variant frequency data.
 * @author brendan
 *
 */
public class VarFreqDB {
	
	private static final String OVERALL = "overall";
	Map<String, Map<String, VariantFreqInfo>> vars = new HashMap<String, Map<String, VariantFreqInfo>>();
	
	public void readFromFile(File freqFile) throws IOException {
		vars = new HashMap<String, Map<String, VariantFreqInfo>>();
		BufferedReader reader = new BufferedReader(new FileReader(freqFile));
		
		String line = reader.readLine();
		VariantFreqInfo info = null;
		while(line != null) {
			info = parseLine(line, info);
			line = reader.readLine();
		}
		
		if (info != null) {
			addVarFreqInfo(info);
		}
		
		reader.close();
	}
	
	/**
	 * Parse position and frequency info from the given line. If it matches the provided info object, 
	 * just add the data to the object and return the object.
	 *  If the chr, pos, ref, or alt do not match, then 
	 *     a) add the info object to the vars field
	 *     b) Create a new info object using the newly parsed info
	 *     c) return the new info object 
	 * @param line
	 * @param info
	 * @return
	 */
	private VariantFreqInfo parseLine(String line, VariantFreqInfo info) {
		if (line == null || line.length()==0 || line.charAt(0) == '#') {
			return null;
		}
		String[] toks = line.split("\t");
		
		int startPos = Integer.parseInt(toks[1]);
		TestTypeFreq ttf = new TestTypeFreq();
		ttf.testType = toks[4].trim();
		ttf.samples = Integer.parseInt(toks[5]);
		ttf.hets = Integer.parseInt(toks[6]);
		ttf.homs = Integer.parseInt(toks[7]);
		
		if (info != null && info.chr.equals(toks[0]) && info.start == startPos && info.ref.equals(toks[2]) && info.alt.equals(toks[3])) {
			//Everything matches, so just add a new TestTypeFreq entry to the info object
			info.testFreqs.add(ttf);
			return info;
		} else {
			
			if (info != null) {
				addVarFreqInfo(info);
			}
			
			VariantFreqInfo newVFI = new VariantFreqInfo();
			newVFI.chr = toks[0];
			newVFI.start = Integer.parseInt(toks[1]);
			newVFI.ref = toks[2];
			newVFI.alt = toks[3];
			newVFI.testFreqs =new ArrayList<TestTypeFreq>(5);
			newVFI.testFreqs.add(ttf);
			return newVFI;
		}
		
	}

	private void addVarFreqInfo(VariantFreqInfo info) {
		Map<String, VariantFreqInfo> contigInfo = vars.get(info.chr);
		if (contigInfo == null) {
			contigInfo = new HashMap<String, VariantFreqInfo>();
			vars.put(info.chr, contigInfo);
		}
		
		VariantFreqInfo prevVal = contigInfo.put( toHashKey(info.start, info.ref, info.alt), info);
		if (prevVal != null) {
			throw new IllegalArgumentException("Whoa, there was already an existing value for this should never happen");
		}
	}
	
	/**
	 * Create a new, empty VariantFreqInfo record at the given location only if one does not already exist
	 * @param chr
	 * @param pos
	 * @param ref
	 * @param alt
	 */
	private void updateVarFreqInfo(String chr, int pos, String ref, String alt) {
		Map<String, VariantFreqInfo> contigInfo = vars.get(chr);
		if (contigInfo == null) {
			contigInfo = new HashMap<String, VariantFreqInfo>();
			vars.put(chr, contigInfo);
		}
		
		String hashKey = toHashKey(pos, ref, alt);
		if (! contigInfo.containsKey(hashKey)) {
			VariantFreqInfo info = new VariantFreqInfo();
			info.chr = chr;
			info.start = pos;
			info.ref = ref;
			info.alt = alt;
			info.testFreqs = new ArrayList<TestTypeFreq>(5);
			contigInfo.put( hashKey, info);	
		}
		
			
	}
	
	private static void incrementProperty(VariantFreqInfo vfi, String type, VariantRec var) {
		//Too bad we're not using Java 8 here... 
		//We need to
		boolean found = false;
		for(TestTypeFreq ttf : vfi.testFreqs) {
			if (ttf.testType.equals(OVERALL)) {
				ttf.samples++;
				if (var.getZygosity() == GTType.HET) {
					ttf.hets++;
				} else {
					ttf.homs++;
				}
			}
			if (ttf.testType.equals(type)) {
				ttf.samples++;
				if (var.getZygosity() == GTType.HET) {
					ttf.hets++;
				} else {
					ttf.homs++;
				}
				found = true;
			}
		}
		
		if (! found) {
			//We didn't find our test type in the list of test types, so we need to make a new one
			TestTypeFreq ttf = new TestTypeFreq();
			ttf.testType = type;
			ttf.samples = 1;
			if (var.getZygosity() == GTType.HET) {
				ttf.hets++;
			} else {
				ttf.homs++;
			}
			vfi.testFreqs.add(ttf);
		}
	}

	
	public void addPoolData(String analysisType, IntervalsFile regions, VariantStore variants) throws IOException {
		regions.buildIntervalsMap();
		
		for(String contig: variants.getContigs()) {
			for(VariantRec var : variants.getVariantsForContig(contig)) {
				updateVarFreqInfo(var.getContig(), var.getStart(), var.getRef(), var.getAlt());		
			}
		}
		
		for(String contig: vars.keySet()) {
			for(VariantFreqInfo var : vars.get(contig).values()) {

				//Is this variant targeted for this sample?
				boolean targeted = regions.contains(var.chr, var.start, false);

				if (targeted) {
					VariantRec queryVar = variants.findRecord(contig, var.start, var.ref, var.alt);
					incrementProperty(var, analysisType, queryVar);				
				}
			}
		}
	}
	
	public void write(PrintStream stream) {
		for(String contig : vars.keySet()) {
			Map<String, VariantFreqInfo> contigVars = vars.get(contig);
			for(VariantFreqInfo vfi : contigVars.values()) {
				for(TestTypeFreq ttf : vfi.testFreqs) {
					stream.println(vfi.chr + "\t" + vfi.start + "\t" + vfi.ref + "\t" + vfi.alt + "\t" + ttf.testType + "\t" + ttf.samples + "\t" + ttf.hets + "\t" + ttf.homs );
				}
			}
		}
	}

	
	private String toHashKey(int start, String ref, String alt) {
		return "" + start + ":" + ref + ":" + alt;
	}
	
	
	static class VariantFreqInfo {
		String chr;
		int start;
		String ref;
		String alt;
		List<TestTypeFreq> testFreqs;
	}
	
	
	static class TestTypeFreq {
		String testType;
		int samples;
		int hets;
		int homs;
	}
	
	
	public static void main(String[] args) throws IOException {
		File input = new File("/home/brendan/DATA3/freq_test/testDB.csv");
		VarFreqDB freqDB = new VarFreqDB();
		freqDB.readFromFile(input);
		
		
		File varsFile = new File("/home/brendan/DATA3/freq_test/testVariants.vcf");
		BEDFile bedFile = new BEDFile(new File("/home/brendan/DATA3/freq_test/testbed.bed"));
		
		freqDB.addPoolData("HHT", bedFile, new VariantPool(new VCFFile(varsFile)));
		
		File output = new File("/home/brendan/DATA3/freq_test/freqTestOutput.csv");
		PrintStream outputStream = new PrintStream(new FileOutputStream(output));
		freqDB.write(outputStream);
		outputStream.close();
	}
}
