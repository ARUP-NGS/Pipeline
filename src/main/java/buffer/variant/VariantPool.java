package buffer.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import ncbi.GeneInfoDB;
import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import util.VCFLineParser;
import buffer.BEDFile;
import buffer.CSVFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;

/**
 * Base class for things that maintain a collection of VariantRecs
 * @author brendan
 *
 */
public class VariantPool extends Operator  {
	
	public static final String ALL_GENES = "all.genes"; //If specified as attribute, include all genes one variant per gene

	protected Map<String, List<VariantRec>>  vars = new HashMap<String, List<VariantRec>>();
	private VariantRec qRec = new VariantRec("?", 0, 0, "x", "x", 0.0, false);
	private boolean operationPerformed = false; //Set to true when performOperation called, avoids loading variants multiple times
	
	/**
	 * Build a new variant pool from the given list of variants
	 * @param varList
	 */
	public VariantPool(List<VariantRec> varList) {
		for(VariantRec v : varList) {
			List<VariantRec> contig = vars.get(v.getContig());
			if (contig == null) {
				contig = new ArrayList<VariantRec>(2048);
				vars.put(v.getContig(), contig);
			}
			contig.add(v);
		}
	}
	
	/**
	 * Construct a new variant pool with variants from the given file using the
	 * provided CSVLineReader to interpret variants from the file
	 * @param file
	 * @param reader
	 * @throws IOException
	 */
	public VariantPool(VariantLineReader reader) throws IOException {
		importFromVariantReader(reader);
	}

	/**
	 * Read a variant pool from a CSV-formatted file, the first 9 columns are assumed to be:
	 * 0: contig
	 * 1: pos
	 * 2: end pos (not currently used)
	 * 3: ref
	 * 4: alt
	 * 5: qual
	 * 6: het / hom
	 * 7: depth
	 * 8: genotype quality
	 * 
	 *
	 * THIS BADLY NEEDS TO BE GENERALIZED SO THAT DIFFERENT TYPES OF CSV FILES
	 * CAN BE PARSED AND TURNED INTO VARIANT RECS - LIKE A VCFLINEPARSER, BUT FOR
	 * CSV'S AND WITH DIFFERENT SUBCLASSES
	 * 
	 * @param file
	 * @throws IOException if file cannot be read
	 */
	
	public VariantPool(CSVFile file) throws IOException {
		importFromVariantReader(new CSVLineReader(file.getFile()));
	}

	public VariantPool(VCFFile file) throws IOException {
		importFromVariantReader(new VCFLineParser(file));
	}
	
	public String toString() {
		return "Pool with " + this.size() + " variants in " + vars.size() + " contigs "; 
	}
	
	private void importFromVariantReader(VariantLineReader reader) throws IOException {
		int lineNumber = 0;
		do {
			VariantRec rec = reader.toVariantRec();
			if (rec == null) {
				if (reader.getCurrentLine().length()>0)
					System.err.println("Warning, could not import variant from line: " + reader.getCurrentLine() );
			}
			else {
				this.addRecordNoSort(rec);
			}
			lineNumber++;
		} while (reader.advanceLine());
		sortAllContigs();
	}
	
	/**
	 * Sort all variant records in each contig
	 */
	public void sortAllContigs() {
		for(String contig : getContigs()) {
			List<VariantRec> records = getVariantsForContig(contig);
			Collections.sort(records, VariantRec.getPositionComparator());
		}
	}
	/**
	 * Create a new pool with all the variants in the source pool
	 * @param sourceVars
	 */
	public VariantPool(VariantPool sourceVars) {
		addAll(sourceVars);
	}
	
	
	/**
	 * Construct new empty variant pool
	 */
	public VariantPool() {
		//blank on purpose
	}
	
	

	/**
	 * Search the 'vars' field for a VariantRec at the given contig and position
	 * @param contig
	 * @param pos
	 * @return
	 */
	public VariantRec findRecord(String contig, int pos) {
		contig = contig.replace("chr", "");
		List<VariantRec> varList = vars.get(contig);
		if (varList == null) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("AnnovarResults could not find contig: " + contig);
			return null;
		}
		
		qRec.setPosition(contig, pos, pos);
		
		int index = Collections.binarySearch(varList, qRec, VariantRec.getPositionComparator());
		if (index < 0) {
			return null;
		}
		
		return varList.get(index);
	}
	
	/**
	 * Search the 'vars' field for a VariantRec at the given contig and position with the given alt
	 * @param contig
	 * @param pos
	 * @return
	 */
	public VariantRec findRecord(String contig, int pos, String alt) {
		contig = contig.replace("chr", "");
		List<VariantRec> varList = vars.get(contig);
		if (varList == null) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("AnnovarResults could not find contig: " + contig);
			return null;
		}
		
		qRec.setPosition(contig, pos, pos);
		
		int index = Collections.binarySearch(varList, qRec, VariantRec.getPositionComparator());
		if (index < 0) {
			return null;
		}
		
		//We want to search all variants at this contig and position, so decrement index while 
		//varList.get(index).gePos() == pos
		while(index > 0 && varList.get(index-1).getStart()==pos) {
			index--;
		}
		
		while(index < varList.size() && varList.get(index).getStart()==pos) {
			if (varList.get(index).getAlt().equals(alt)) {
				return varList.get(index);
			}
			index++;
		}
		
		//No alt matches found
		return null;
	}
	
	/**
	 * Returns true if this pool contains any kind of variant at the given contig and position
	 * @param contig
	 * @param pos
	 * @return
	 */
	public boolean contains(String contig, int pos) {
		
		if (! vars.containsKey(contig)) {
			return false;
		}
		else {
			return findInsertionPosition(contig, pos) > -1;
		}
	}
	
	/**
	 * Computes the "transition / transversion ratio" (not the real thing) for all 
	 * variants where both the reference and the alt have length 1
	 * @return
	 */
	public double computeTTRatio() {
		double transitions = 0;
		double transversions = 0;
		for(String contig : getContigs()) {
			for(VariantRec rec : getVariantsForContig(contig)) {
				if (rec.isSNP()) {
					if (rec.isTransition())
						transitions++;
					if (rec.isTransversion())
						transversions++;
					
				}
			}
		}
		if (transversions == 0.0) {
			return Double.NaN;
		}
		return transitions / transversions;
	}
	
	public int countTransitions() {
		int transitions = 0;
		for(String contig : getContigs()) {
			for(VariantRec rec : getVariantsForContig(contig)) {
				if (rec.isSNP()) {
					if (rec.isTransition())
						transitions++;
					
				}
			}
		}
		return transitions;
	}
	
	public int countTransverions() {
		int transversions = 0;
		for(String contig : getContigs()) {
			for(VariantRec rec : getVariantsForContig(contig)) {
				if (rec.isSNP()) {
					if (rec.isTransversion())
						transversions++;
					
				}
			}
		}
		return transversions;
	}
	
	/**
	 * Returns the mean quality of all variants in this pool
	 * @return
	 */
	public double meanQuality() {
		double sum = 0;
		double count = 0;
		for(String contig : getContigs()) {
			for(VariantRec rec : getVariantsForContig(contig)) {
				sum += rec.getQuality();
				count++;
			}
		}
		return sum / count;
	}
	/**
	 * Attemp to find the record at the given contig and position, but don't emit a warning if 
	 * we can't find it
	 * @param contig
	 * @param pos
	 * @return
	 */
	public VariantRec findRecordNoWarn(String contig, int pos) {
		contig = contig.replace("chr", "");
		List<VariantRec> varList = vars.get(contig);
		if (varList == null) {
			//System.err.println("Contig " + contig + " not found");
			return null;
		}
		
		qRec.setPosition(contig, pos, pos);
		
		int index = Collections.binarySearch(varList, qRec, VariantRec.getPositionComparator());
		if (index < 0) {
			return null;
		}
		
		return varList.get(index);		
	}
	
	/**
	 * Returns the result of binary search for a record at the given position. If the 
	 * record is found the result will be the index of the found record. If no record is
	 * found the result is -1*insertionPoint -1 (ala Collections.binarySearch), where
	 * insertionPoint is where the record would be inserted if it was added 
	 * @param contig
	 * @param pos
	 * @return
	 */
	public int findInsertionPosition(String contig, int pos) {
		List<VariantRec> varList = vars.get(contig);
		if (varList == null) {
			throw new IllegalArgumentException("No contig with name " + contig + " found");
		}
		
		qRec.setPosition(contig, pos, pos);
		
		int index = Collections.binarySearch(varList, qRec, VariantRec.getPositionComparator());
		return index;
	}
	
	/**
	 * Add all variants from source to pool, allowing duplicate entries
	 * @param source
	 */
	public void addAll(VariantPool source) {
		addAll(source, true);
	}
	
	/**
	 * Add all variants from source to this pool
	 * @param source Variants to add to this pool
	 * @param allowDups If duplicate variants should be added
	 */
	public void addAll(VariantPool source, boolean allowDups) {
		for(String contig : source.getContigs()) {
			List<VariantRec> curVars = this.getVariantsForContig(contig);
			List<VariantRec> sourceVars = source.getVariantsForContig(contig);
			
			if (curVars == null || curVars.size() == 0) {
				List<VariantRec> newContigVars = new ArrayList<VariantRec>();
				newContigVars.addAll(sourceVars);
				vars.put(contig, newContigVars);
			}
			else {
				List<VariantRec> mergedVars = new ArrayList<VariantRec>( curVars.size() + sourceVars.size() );
				
				//awkward but hopefully fast merge-sorted-lists algo
				Iterator<VariantRec> cIt = curVars.iterator();
				Iterator<VariantRec> sIt = sourceVars.iterator();
				
				VariantRec cVar = cIt.next();
				VariantRec sVar = sIt.next();
				
				while(cVar != null && sVar != null) {
					//If we're not allowing duplicates, and sVar and cVar are at the same position,
					//then advance the source iterator and don't add anything to the mergedVars list
					if ((!allowDups) && cVar.getStart() == sVar.getStart()) {
						try {
							sVar = sIt.next();
						}
						catch(NoSuchElementException ex) {
							sVar = null;
						}
						continue;
					}
					
					if (cVar.getStart() < sVar.getStart()) {
						mergedVars.add(cVar);
						try {
							cVar = cIt.next();
						}
						catch(NoSuchElementException ex) {
							cVar = null;
						}
					}
					else {
						mergedVars.add(sVar);
						try {
							sVar = sIt.next();
						}
						catch(NoSuchElementException ex) {
							sVar = null;
						}
					}
				}
				
				//Dump remaining vars
				while(sVar != null) {
					mergedVars.add(sVar);
					try {
						sVar = sIt.next();
					}
					catch(NoSuchElementException ex) {
						sVar = null;
					}
				}
				
				while(cVar != null) {
					mergedVars.add(cVar);
					try {
						cVar = cIt.next();
					}
					catch(NoSuchElementException ex) {
						cVar = null;
					}
				}
				
				vars.put(contig, mergedVars);
//				for(VariantRec rec : source.getVariantsForContig(contig)) {
//					//this.addRecord(rec);
//				}
			}
		}
	}
	
	
	
	/**
	 * Count lines in a file, used for progress estimation when reading big vcfs 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	private static int countLines(VCFFile file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file.getAbsolutePath()));
		int lines = 0;
		String line = reader.readLine();
		while (line != null) {
			lines++;
			line = reader.readLine();
		}
		return lines;
	}
	
	/**
	 * Return total number of variants in pool
	 * @return
	 */
	public int size() {
		int count = 0;
		for(String contig : getContigs()) {
			count += this.getVariantsForContig(contig).size();
		}
		return count;
	}
	
	/**
	 * Returns the next variant in the list after the variant at the given variants
	 * @param pos
	 * @return
	 */
	public VariantRec nextVariant(VariantRec pos) {
		List<VariantRec> vars = getVariantsForContig(pos.getContig());
		if (vars == null || vars.size()==0) {
			return null;
		}
		
		
		int index = this.findInsertionPosition(pos.getContig(), pos.getStart());
		
		//Record found, but points to last variant in the list
		if (index == (vars.size()-1))
			return null;
		
		//Record found and is not last variant, so return next one
		if (index>=0) {
			int start = index+1;
			while (start < vars.size() && vars.get(start).getStart() == pos.getStart())
				start++;
			if (start == vars.size())
				return null;
			else
				return vars.get(start);
		}
		
		index = -1*index-1;
		
		//Not found, but after last item in list, return null
		if (index > (vars.size()-2))
			return null;
		
		//Not found, but not after last item, return next record
		return vars.get(index);
		
	}
	
	/**
	 * Emit a tab-separated listing of all variants to the given stream
	 * @param out
	 */
	public void listAll(PrintStream out) {
		out.println( VariantRec.getSimpleHeader() );
		for(String contig : getContigs() ) {
			for(VariantRec rec : this.getVariantsForContig(contig)) {
				//out.println(contig + "\t" + rec.getStart() + "\t . \t" + rec.ref + "\t" + rec.alt + "\t" + het + "\t" + rec.getQuality() + "\t" + rec.getProperty(VariantRec.DEPTH));
				out.println( rec.toSimpleString() );
			}
		}
	}
	
	/**
	 * Emit this variant pool in GVF format, with all annotations and properties listed
	 * @param out
	 */
	public void emitToGVF(PrintStream out) {
		for(String contig : getContigs() ) {
			for(VariantRec rec : this.getVariantsForContig(contig)) {
				String varType = rec.getAnnotation(VariantRec.VARIANT_TYPE);
				if (varType == null)
					varType = "-";
				String str = contig + "\t pipe \t" + 
							 varType + "\t" + 
						     rec.getStart() + "\t" + 
							 rec.getEnd() + "\t" + 
						     rec.getQuality() + "\t" + 
							 "." + "\t" +
						     ".";
				
				StringBuilder keyVals = new StringBuilder();
				for(String prop : rec.getPropertyKeys()) {
					keyVals.append(prop + "=" + rec.getProperty(prop) + ";");
				}
				
				for(String anno : rec.getAnnotationKeys()) {
					keyVals.append(anno + "=" + rec.getAnnotation(anno) + ";");
				}
				
				//out.println(contig + "\t" + rec.getStart() + "\t . \t" + rec.ref + "\t" + rec.alt + "\t" + het + "\t" + rec.getQuality() + "\t" + rec.getProperty(VariantRec.DEPTH));
				out.println( str  + "\t" + keyVals );
			}
		}
		
	}
	
	/**
	 * Emit a tab-separated listing of all variants, including properties associated with the given keys, to the given stream
	 * @param out
	 */
	public void listAll(PrintStream out, List<String> keys) {
		StringBuilder header = new StringBuilder( VariantRec.getSimpleHeader() );
		for(String key : keys) 
			header.append("\t" + key);
		out.println( header );
		for(String contig : getContigs() ) {
			for(VariantRec rec : this.getVariantsForContig(contig)) {		
				out.println(rec.toSimpleString() + rec.getPropertyString(keys));
			}
		}
	}
	
	/**
	 * Return a new variant pool that is the intersection of this pool and the given pool.
	 * Note that variants are not cloned - they are the variants from this pool
	 * @param varsB
	 * @return
	 */
	public VariantPool intersect(VariantPool varsB) {
		VariantPool intersect = new VariantPool();
		int size = size();
		int count = 0;
		for(String contig : getContigs()) {
			for(VariantRec rec : getVariantsForContig(contig)) {
				count++;
				VariantRec recB = varsB.findRecordNoWarn(rec.getContig(), rec.getStart());
				if (recB != null && rec.getAlt().equals(recB.getAlt())) {
					rec.addAnnotation(VariantRec.altB, recB.getAlt());
					if (recB.isHetero()) {
						rec.addAnnotation(VariantRec.zygosityB, "het");
					}
					else {
						rec.addAnnotation(VariantRec.zygosityB, "hom");
					}
					intersect.addRecord(rec);
				}
			}
		}
		return intersect;
	}
	
	/**
	 * Adjusts all indel variants in the following manner: Any indel that begins and ends with the same
	 * base, the first base is moved to the last position and 1 is subtracted from the start and end position
	 * So		: 117  - ACGTA
	 * Becomes 	: 116  - CGTAA
	 * 
	 * That's because there's ambiguity in how such indels are written (the two above forms are indistinguishable)
	 * and the GATK emits variants in the second way
	 * 
	 */
//	public void rotateIndels() {
//		for(String contig : getContigs()) {
//			for(VariantRec rec : getVariantsForContig(contig)) {
//				if (rec.isIndel())
//					rec.rotateIndel();
//			}
//		}
//		
//		sortAllContigs();
//	}
	
	
	/**
	 * Remove from this variant pool the variant at the given contig and pos whose ALT allele matches
	 * the given allele
	 * @param contig
	 * @param pos
	 * @param allele
	 * @return
	 */
	public boolean removeVariantAllele(String contig, int pos, String allele) {
		List<VariantRec> varList = vars.get(contig);
		if (varList == null) {
			return false;
		}
		
		VariantRec qRec = new VariantRec(contig, pos, pos, "x", "x", 0.0, false);
		
		int index = Collections.binarySearch(varList, qRec, VariantRec.getPositionComparator());
		//This pool does not contain a variant at the given position
		if (index < 0) {
			return false;
		}
		
		//Found a variant at this position, now search all
		VariantRec var = varList.get(index);
		while(var.getStart() == pos) {
			index--;
			if (index < 0)
				break;
			var = varList.get(index);
		}
		index++;
		var = varList.get(index);
		
		//Index now points at LAST variant with given position (yes, there may be more than one variant at this pos)
		while(var.getStart() == pos && index < varList.size()) {
			if (var.getAlt().equals(allele)) {
				varList.remove(index);
				if (index >= varList.size()) {
					break;
				}
				var = varList.get(index);
			} else {
				index++;
				if (index >= varList.size()) {
					break;
				}
				var = varList.get(index);
			}
		}
		return true;
	}
	
	/**
	 * Removes from this pool all variants at the sites of the variants in the second pool
	 * @param toRemove
	 * @return
	 */
	public int removeVariants(VariantPool toRemove) {
		int removedCount = 0;
		for(String contig : toRemove.getContigs()) {
			for(VariantRec rec : toRemove.getVariantsForContig(contig)) {
				removeVariantAllele(rec.getContig(), rec.getStart(), rec.getAlt());
			}
		}
		return removedCount;
	}
	
	/**
	 * Remove the given VariantRec from this pool - this looks BY REFERENCE, NOT VALUE!
	 * @param rec
	 * @return
	 */
	public boolean removeVariant(VariantRec rec) {
		List<VariantRec> contig = getVariantsForContig(rec.getContig());
		if (contig == null)
			return false;
		else {
			return contig.remove(rec);
		}
	}
	
	public void removeDuplicates() {
		for(String contig : getContigs()) {
			HashMap<Integer, VariantRec> map = new HashMap<Integer, VariantRec>();
			for(VariantRec rec : this.getVariantsForContig(contig)) {
				map.put(rec.getStart(), rec);
			}
			
			List<VariantRec> list = this.getVariantsForContig(contig);
			list.clear();
			for(Integer pos : map.keySet()) {
				list.add(map.get(pos));
			}
		}
		sortAllContigs();
	}
	
	/**
	 * Return total number of heterozygotes in pool
	 * @return
	 */
	public int countHeteros() {
		int count = 0;
		for(String contig : vars.keySet()) {
			Collection<VariantRec> varRecs = this.getVariantsForContig(contig);
			for(VariantRec rec : varRecs) {
				if (rec.isHetero()) 
					count++;
			}
		}

		return count;
	}
	
	/**
	 * Return total number of SNPs, in which both ref and alt have length = 1
	 * @return
	 */
	public int countSNPs() {
		int count = 0;
		for(String contig : vars.keySet()) {
			Collection<VariantRec> varRecs = this.getVariantsForContig(contig);
			for(VariantRec rec : varRecs) {
				if (rec.isSNP()) 
					count++;
			}
		}

		return count;		
	}
	
	/**
	 * Return total number of deletions
	 * @return
	 */
	public int countDeletions() {
		int count = 0;
		for(String contig : vars.keySet()) {
			Collection<VariantRec> varRecs = this.getVariantsForContig(contig);
			for(VariantRec rec : varRecs) {
				if (rec.isDeletion()) 
					count++;
			}
		}

		return count;
	}
	
	/**
	 * Return total number of insertions
	 * @return
	 */
	public int countInsertions() {
		int count = 0;
		for(String contig : vars.keySet()) {
			Collection<VariantRec> varRecs = this.getVariantsForContig(contig);
			for(VariantRec rec : varRecs) {
				if (rec.isInsertion()) 
					count++;
			}
		}

		return count;
	}
	
	
	public boolean removeRecordAtPos(String contig, int pos) {
		List<VariantRec> varList = vars.get(contig);
		if (varList == null) {
			return false;
		}
		
		VariantRec qRec = new VariantRec(contig, pos, pos, "x", "x", 0.0, false);
		
		int index = Collections.binarySearch(varList, qRec, VariantRec.getPositionComparator());
		if (index < 0) {
			return false;
		}
		
		varList.remove(index);
		return true;
	}
	
	/**
	 * Add a new record to the pool. This re-sorts the contig every time to ensure that records are sorted
	 * @param rec
	 */
	public void addRecord(VariantRec rec) {
		addRecordNoSort(rec);
		List<VariantRec> contigVars = vars.get( rec.getContig() ); 
		Collections.sort(contigVars, VariantRec.getPositionComparator());
	}
	
	/**
	 * Add all variants from given pool to this pool. Existing records are not replaced by 
	 * new records.
	 * @param otherPool
	 */
//	public void mergePool(VariantPool otherPool) {
//		for(String contig : otherPool.getContigs()) {
//			
//			List<VariantRec> existingContig = this.getVariantsForContig(contig);
//			
//			for(VariantRec var : otherPool.getVariantsForContig(contig)) {
//				int insertionPoint = this.findInsertionPosition(contig, var.getStart());
//				if (insertionPoint < 0) {
//					int actualPoint = -1*insertionPoint - 1;
//					existingContig.add(actualPoint, var);
//				}
//					
//				
//			}
//		}
//	}
	
	/**
	 * Add a new record to the pool but do not sort the contig it was added to. This is 
	 * faster if you're adding lots of variants (from a VCFFile, for instance), but
	 * requires that all contigs are sorted 
	 * @param rec
	 */
	public void addRecordNoSort(VariantRec rec) {
		List<VariantRec> contigVars = vars.get( rec.getContig() ); 
		if (contigVars == null) {
			contigVars = new ArrayList<VariantRec>(8192);
			vars.put(rec.getContig(), contigVars);
		}
		contigVars.add(rec);
	}
	
	public int getContigCount() {
		return vars.size();
	}

	public Collection<String> getContigs() {
		return vars.keySet();
	}
	
	/**
	 * Remove all variants from this pool
	 */
	public void clear() {
		vars = new HashMap<String, List<VariantRec>>();
	}
	
	/**
	 * Obtain a list of all property keys used in the variants
	 * @return
	 */
	public List<String> getPropertyKeys() {
		int max = 10000; //Don't examine more than this many records
		int count = 0;
		List<String> keys = new ArrayList<String>();

		for(String contig : getContigs()) {
			List<VariantRec> vars = getVariantsForContig(contig);
			for(int i=0; i<vars.size() && count < max; i++) {
				VariantRec rec = vars.get(i);
				for(String key: rec.getPropertyKeys()) {
					if (! keys.contains(key)) {
						keys.add( key );
					}
				}
				count++;
				if (count > max) {
					break;
				}
			}
		}
		return keys;
	}
	

	public List<VariantRec> getVariantsForContig(String contig) {
		List<VariantRec> varList = vars.get(contig);
		if (varList != null)
			return varList;
		else 
			return new ArrayList<VariantRec>();
	}

	public List<VariantRec> filterPool(VariantFilter filter) {
		List<VariantRec> passing = new ArrayList<VariantRec>(1024);
		for(String contig : vars.keySet()) {
			for(VariantRec rec : vars.get(contig)) {
				if (filter.passes(rec))
					passing.add(rec);
			}
		}
		return passing;
	}
	
	/**
	 * Returns a list of all variants that are in are in the given BED file 
	 * @param bedFile
	 * @return
	 * @throws IOException 
	 */
	public VariantPool filterByBED(BEDFile bedFile) throws IOException {
		bedFile.buildIntervalsMap();
		VariantPool pool = new VariantPool();
		for(String contig : getContigs()) {
			List<VariantRec> vars = getVariantsForContig(contig);
			for(VariantRec rec : vars) {
				if (bedFile.contains(contig, rec.getStart(), false)) {
					pool.addRecordNoSort(rec);
				}
			}
		}
		pool.sortAllContigs();
		return pool;
	}
	
	/**
	 * Remove from this variant pool all variants not in regions described by the BED file
	 * @param bedFile
	 * @throws IOException
	 */
	public void removeVariantNotInBED(BEDFile bedFile) throws IOException {
		bedFile.buildIntervalsMap();
		for(String contig : getContigs()) {
			List<VariantRec> vars = getVariantsForContig(contig);
			for(VariantRec rec : vars) {
				if (! bedFile.contains(contig, rec.getStart(), false)) {
					removeVariant(rec); 
				}
			}
		}
	}
	
	/**
	 * Incomplete: Emit this variant pool in vcf format to a printstream
	 * @param ref
	 * @param annotationKeys
	 * @param out
	 */
	public void toVCF(ReferenceFile ref, List<String> annotationKeys, PrintStream out) {
		//Make a VCF header
		out.println("##fileformat=VCFv4.1");
		
		Calendar now = Calendar.getInstance();
		out.println("##fileDate=" + now.get(Calendar.YEAR) + now.get(Calendar.MONTH) + now.get(Calendar.DAY_OF_WEEK_IN_MONTH) );
		out.println("##source=PipelineVariantPool");
		out.println("##reference=" + ref.getFilename()  );
		for(String contig : getContigs()) {
			out.println("##contig=<ID=" + contig + ",species=\"Homo sapiens\">\n" );
		}
		
		throw new IllegalStateException("Not completed yet");
	}
	
	/**
	 * Emit all variants to the given printstream in a table-style format
	 * @param annotationKeys
	 * @param out
	 */
	public void emitToTable(List<String> annotationKeys, PrintStream out) {
		StringBuilder header = new StringBuilder();
		header.append(VariantRec.getBasicHeader());
		
		for(String key : annotationKeys) {
			header.append("\t" + key);
		}
		
		out.print(header.toString() + "\n");
		
		
		for(String contig : getContigs()) {
			List<VariantRec> records = getVariantsForContig(contig);
			for(VariantRec rec : records) {
				out.print(rec.toBasicString() + "\t");
				out.print(rec.getPropertyString(annotationKeys) + "\n");
			}
		}
		out.flush();
	}
	
	
	/**
	 * Return a new list of variants that are those in the given list that pass the given filter
	 * @param filter
	 * @param list
	 * @return
	 */
	public static List<VariantRec> filterList(VariantFilter filter, List<VariantRec> list) {
		List<VariantRec> passing = new ArrayList<VariantRec>(list.size()/2+1);
		for(VariantRec rec : list) {
			if (filter.passes(rec))
				passing.add(rec);
		}
		return passing;
	}


	@Override
	public void setAttribute(String key, String value) {
		properties.put(key, value);
	}


	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof VCFFile) {
					inputVariants = (VCFFile)obj;
				}
				if (obj instanceof CSVFile) {
					inputVariants = (CSVFile)obj;					
				}
			}
		}
	}

	/**
	 * Creates a new List with all of the variants in it
	 * @return
	 */
	public List<VariantRec> toList() {
		List<VariantRec> vars = new ArrayList<VariantRec>(this.size());
		for(String contig : getContigs()){
			for(VariantRec var : getVariantsForContig(contig)) {
				vars.add(var);
			}
				
		}
		return vars;
	}

	@Override
	public void performOperation() throws OperationFailedException {
		if (operationPerformed) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("PerformOperation is being called again for variant pool " + getObjectLabel() + ", refusing to re-load variants");
		}
		operationPerformed = true;
		
		String allGenes = this.getAttribute(ALL_GENES);
		if (allGenes != null & Boolean.parseBoolean(allGenes)) {
			//Make a fake variant pool with one variant per gene
			Logger.getLogger(Pipeline.primaryLoggerName).info("Adding one record for all genes!");
			GeneInfoDB geneDB = GeneInfoDB.getDB();
			if (geneDB == null)
				try {
					geneDB= new GeneInfoDB(new File(GeneInfoDB.defaultDBPath));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			for(String symbol : geneDB.getAllGenes()) {
				VariantRec rec = new VariantRec("Z", 1, 2, "X", "X", 100.0, false);
				rec.addAnnotation(VariantRec.GENE_NAME, symbol);
				this.addRecordNoSort(rec);
			}
			Logger.getLogger(Pipeline.primaryLoggerName).info("Added " + this.size() + " variant records");
		}
		
		if (inputVariants == null) 
			return ; // Just make an empty pool
		

		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Building variant pool from variants in file " + inputVariants.getFilename());
		
		if (inputVariants instanceof VCFFile) {
			try {
				importFromVariantReader(new VCFLineParser( (VCFFile)inputVariants ) );
			} catch (IOException e) {
				e.printStackTrace();
				throw new OperationFailedException("IO error reading file: " + inputVariants.getAbsolutePath(), this);
			}
		}
		
		if (inputVariants instanceof CSVFile) {
			try {
				importFromVariantReader(new CSVLineReader( ((CSVFile)inputVariants).getFile() ));
				//importFromVariantReader( new SimpleLineReader(inputVariants.getFile()));
			} catch (IOException e) {
				e.printStackTrace();
				throw new OperationFailedException("IO error reading file: " + inputVariants.getAbsolutePath(), this);
			}
		}
				
		
		logger.info("Created variant pool with " + getContigs().size() + " contigs and " + this.size() + " total variants");
	}
	
	/**
	 * Returns true if .performOperation has been called at least once
	 * @return
	 */
	public boolean isOperationPerformed() {
		return operationPerformed;
	}


	private FileBuffer inputVariants = null;
	



	
}
