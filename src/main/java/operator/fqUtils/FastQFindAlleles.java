package operator.fqUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.TextBuffer;

/**
 * @author Jacob
 *
 */
/* Hello! */
public class FastQFindAlleles extends IOOperator {

	public static final String LEFT_MATCH = "left.match";
	public static final String RIGHT_MATCH = "right.match";
	
	
	protected String leftMatch;
	protected String rightMatch;
	
	private int totalReads;
	private int leftForwardReads;
	private int	rightForwardReads;
	private int	bothForwardReads;
	private int	leftReverseReads;
	private int	rightReverseReads;
	private int	bothReverseReads;
	private LinkedHashMap<String, Integer> sortedSeqMap;
	private LinkedHashMap<Integer, Integer> sortedFamilyMap;
	private	List<FileBuffer> fqs;
	private final int familiesToPrint = 20;
	
	@Override
	public void performOperation() throws OperationFailedException, IOException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		String leftMatchAttr = properties.get(LEFT_MATCH);
		if (leftMatchAttr != null) {
			this.leftMatch = leftMatchAttr;
		} else {
			throw new OperationFailedException("Attribute left.match required but not specified", this);
		}

		String rightMatchAttr = properties.get(RIGHT_MATCH);
		if (rightMatchAttr != null) {
			this.rightMatch = rightMatchAttr;
		} else {
			throw new OperationFailedException("Attribute right.match required but not specified", this);
		}
		
		//Grab all fastqs
		if (this.getAllInputBuffersForClass(FastQFile.class).size() > 0) {
			this.fqs = this.getAllInputBuffersForClass(FastQFile.class);
		} else {
			throw new OperationFailedException("No fastq files were specified", this);
		}
		
		FileBuffer uniqSeqFile = this.getOutputBufferForClass(TextBuffer.class);
		if (uniqSeqFile == null) {
			throw new OperationFailedException("No output unique sequence text file specified", this);
		}
		
		logger.info("Listing unique read sub-sequences and counts for fastq file(s) given bounding sequences.");
		logger.info("Left side bounding sequence: " + leftMatch);
		logger.info("Right side bounding sequence: " + rightMatch);
		for (FileBuffer fastqbuff : this.fqs) {
			logger.info("FastQ file: " + fastqbuff.getAbsolutePath());
		}
		

		calcUniqSeqs(this.fqs);
		
		String uniqueSeqSummary = makeUniqSeqSummary();
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(uniqSeqFile.getFile()));
			writer.write(uniqueSeqSummary);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new OperationFailedException("Error writing to output file: " + uniqSeqFile.getAbsolutePath(), this);
		}
		logger.info("Done computing unique sequences for input fastq file(s) ");
	}
	

	private void calcUniqSeqs(List<FileBuffer> fqList) throws OperationFailedException, IOException {
		//collect forward strand trims
		//	if both found, trim read, add to uniqMap and count passing (passed both right and left trim seq forward)
		//	if both not found, reverse complement read and try again with new counters (left, right, both reverse)
		//		
		this.totalReads = 0;
		this.leftForwardReads = 0;
		this.rightForwardReads = 0;
		this.bothForwardReads = 0;
		this.leftReverseReads = 0;
		this.rightReverseReads = 0;
		this.bothReverseReads = 0;
		Map<String, Integer> seqMap = new LinkedHashMap<String, Integer>();;
		Map<Integer, Integer> familyMap = new LinkedHashMap<Integer, Integer>();;

		for (FileBuffer fq : fqList) {
			GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(fq.getAbsolutePath()));
			BufferedReader br = new BufferedReader(new InputStreamReader(gzip));

			int fourth = 4;
			String read = null;
			String line;
			int i = 0;
			while ((line = br.readLine()) != null) {
				if (i % fourth == 0) {
					//read name line must start with "@"
					if (! line.startsWith("@")) {
						gzip.close();
						br.close();
						throw new OperationFailedException("FastQ file format error at READ NAME line number " + 	String.valueOf(i) + ": " + fq.getAbsolutePath(), this);
					}
				} else if (i % fourth == 1) {
					//read sequence line
					//no format test here, that happens below
					read = line;
				} else if (i % fourth == 2) {
					//optional line must start with "+"
					if (! line.startsWith("+")) {
						gzip.close();
						br.close();
						throw new OperationFailedException("FastQ file format error at OPTIONAL line number " + 	String.valueOf(i) + ": " + fq.getAbsolutePath(), this);
					}
				} else if (i % fourth == 3) {
					//base qualities line must be same length as most recent read
					if (line.length() != read.length()) {
						gzip.close();
						br.close();
						throw new OperationFailedException("FastQ file format error at QUALITY line number " + 	String.valueOf(i) + ": " + fq.getAbsolutePath(), this);
					} else {
						//Made it through all four lines of the record without error
						//so now process the read
						processRead(read, seqMap);
					}
				}
				i ++;
			}
			br.close();
		}

		//sort the map of unique read segments and counts
		this.sortedSeqMap = (LinkedHashMap<String, Integer>) sortByValueDesending(seqMap);
		
		//generate a histogram-like map of key=familySize, value=numberOfFamilies
		for (String key : this.sortedSeqMap.keySet()) {
			Integer famSize= this.sortedSeqMap.get(key);
			if (familyMap.containsKey(famSize)) {
				int oldVal = familyMap.get(famSize);
				familyMap.put(famSize, oldVal + 1);
			} else {
				familyMap.put(famSize, 1);
			}
		}
		
		//sort the map of family sizes by their counts
		this.sortedFamilyMap = (LinkedHashMap<Integer, Integer>) sortByValueAssending(familyMap);
	}
	
	private void processRead(String read, Map<String, Integer> seqMap) {
		// get total count
		this.totalReads++;
		// look for left.trim.seq pos and count passing (found left trim seq
		// forward)
		int leftIndex = read.indexOf(this.leftMatch);
		// look for right.trim.seq pos and count passing (passed right trim seq
		// forward)
		int rightIndex = read.indexOf(this.rightMatch);
		if (leftIndex >= 0) {
			this.leftForwardReads++;
		}
		if (rightIndex >= 0) {
			this.rightForwardReads++;
		}
		// if both found, trim read, add to uniqMap and count passing (passed
		// both right and left trim seq forward)
		if (leftIndex >= 0 && rightIndex >= 0 && (leftIndex + this.leftMatch.length()) < (rightIndex -1)) {
			this.bothForwardReads++;
			// TODO now trim read and enter into map
			String subSeq = read.substring(leftIndex + this.leftMatch.length(),
					rightIndex - 1);
			if (seqMap.containsKey(subSeq)) {
				int oldVal = seqMap.get(subSeq);
				seqMap.put(subSeq, oldVal + 1);
			} else {
				seqMap.put(subSeq, 1);
			}
		}
		// if no matches get reverse complement and repeat with reverse counters
		if (leftIndex < 0 && rightIndex < 0) {
			String revComp = null;
			try {
				revComp = reverseComplement(read);
			} catch (OperationFailedException e) {
				e.printStackTrace();
			}
			// look for left.trim.seq pos and count passing (found left trim seq
			// forward)
			leftIndex = revComp.indexOf(this.leftMatch);
			// look for right.trim.seq pos and count passing (passed right trim
			// seq forward)
			rightIndex = revComp.indexOf(this.rightMatch);
			if (leftIndex >= 0) {
				this.leftReverseReads++;
			}
			if (rightIndex >= 0) {
				this.rightReverseReads++;
			}
			// if both found, trim read, add to uniqMap and count passing
			// (passed both right and left trim seq forward)
			if (leftIndex >= 0 && rightIndex >= 0 && (leftIndex + this.leftMatch.length()) < (rightIndex -1)) {
				this.bothReverseReads++;

				// now trim reverse complement and enter into map
				String subSeq = revComp.substring(
						leftIndex + this.leftMatch.length(), rightIndex - 1);
				if (seqMap.containsKey(subSeq)) {
					int oldVal = seqMap.get(subSeq);
					seqMap.put(subSeq, oldVal + 1);
				} else {
					seqMap.put(subSeq, 1);
				}
			}
		}
	}
	

	private String makeUniqSeqSummary() {

		String lineSep = System.getProperty("line.separator");
		String summary = "Summary of Fastq reads with known flanking sequences" + lineSep;
		summary += "FastQ Files" + lineSep;
		for (FileBuffer f : this.fqs) {
			summary += f.getAbsolutePath() + lineSep;
		}
		summary += lineSep;
		summary += "Left flanking sequence: " + this.leftMatch + lineSep;
		summary += "Right flanking sequence: " + this.rightMatch + lineSep;
		summary += "Total reads: " + this.totalReads + lineSep;
		summary += "Total reads with LEFT flanking sequence in FORWARD direction: " + this.leftForwardReads + lineSep;
		summary += "Total reads with LEFT flanking sequence in REVERSE direction: " + this.leftReverseReads + lineSep;
		summary += "Total reads with RIGHT flanking sequence in FORWARD direction: " + this.rightForwardReads + lineSep;
		summary += "Total reads with RIGHT flanking sequence in REVERSE direction: " + this.rightReverseReads + lineSep;
		summary += "Total reads with BOTH flanking sequences in FORWARD direction: " + this.bothForwardReads + lineSep;
		summary += "Total reads with BOTH flanking sequences in REVERSE direction: " + this.bothReverseReads + lineSep;
		summary += lineSep;
		summary += "Total reads with BOTH flanking sequences: " + String.valueOf(this.bothForwardReads + this.bothReverseReads) + lineSep;
		summary += lineSep;
		if (this.sortedFamilyMap.size() > 0) {
			summary += "Histogram of trimmed read family sizes" + lineSep;
			summary += "FamilySize\tNumberOfFamilies" + lineSep;
			//print each entry in the map
			for (Integer key : this.sortedFamilyMap.keySet()) {
				summary += String.valueOf(key) + "\t" + String.valueOf(sortedFamilyMap.get(key)) + lineSep ;
			}
		}
		summary += lineSep;
		if (this.sortedSeqMap.size() > 0) {
			summary += "List of flanked sequences from least to most common" + lineSep;
			summary += "NumberOfReads\tSequence" + lineSep;
			//print each entry in the map
			for (String key : this.sortedSeqMap.keySet()) {
				summary += String.valueOf(sortedSeqMap.get(key)) + "\t" + key  + lineSep ;
			}
		}
		return summary;
	}
	
	private String reverseComplement(String dna) throws OperationFailedException {
		StringBuffer rev = new StringBuffer(dna).reverse();
		char[] revComp = new char[rev.length()];
		for (int i = 0 ; i < rev.length() ; i++) {
			switch(rev.charAt(i)) {
		         case 'A': revComp[i] = 'T';
		         break;
		         case 'T': revComp[i] = 'A';
		         break;
		         case 'G': revComp[i] = 'C';
		         break;
		         case 'C': revComp[i] = 'G';
		         break;
		         case 'N': revComp[i] = 'N';
		         break;
		         default: 
		 			throw new OperationFailedException("Unexpected char in fastq reads outside of the set (A,C,G,T,N)", this);
		     }
		}
		return new String(revComp);
	}
	
	private static <K, V extends Comparable<? super V>> Map<K, V> sortByValueAssending(Map<K, V> map) {
		return sortByValue(map, true);
	}

	private static <K, V extends Comparable<? super V>> Map<K, V> sortByValueDesending(Map<K, V> map) {
		return sortByValue(map, false);
	}
	
	private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue( Map<K, V> map, boolean assending) {
		List<Map.Entry<K, V>> list =
		        new LinkedList<Entry<K, V>>( map.entrySet() );
		
		if (assending) {
			Collections.sort( list, new Comparator<Map.Entry<K, V>>() {
			    @Override
			    public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 ) {
			        return (o1.getValue()).compareTo( o2.getValue() );
			    }
			} );
		} else {
			Collections.sort( list, new Comparator<Map.Entry<K, V>>() {
			    @Override
			    public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 ) {
			        return (o2.getValue()).compareTo( o1.getValue() );
			    }
			} );
		}
		
		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list)
		{
		    result.put( entry.getKey(), entry.getValue() );
		}
		return result;
	}	
	
}
