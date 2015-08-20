package buffer.variant;

import java.util.Collection;

/**
 * A VariantStore is something that can store potentially large lists of variants. 
 * @author brendan
 *
 */
public interface VariantStore {

	/**
	 * Add this variant to the storage
	 * @param rec
	 */
	public void addRecord(VariantRec rec);
	
	/**
	 * Add all variants in the given store to this one
	 * @param store
	 */
	public void addAll(VariantStore store, boolean allowDuplicates);
	
	
	public Collection<VariantRec> getVariantsForContig(String contig);
	
	
	/**
	 * Return a list of all contigs present in the store. 
	 * @return
	 */
	public Collection<String> getContigs();
	
	/**
	 * The number of contigs present
	 * @return
	 */
	public int getContigCount();
	
	
	/**
	 * The total number of variants stored in this store
	 * @return
	 */
	public int size();

	/**
	 * Try to find a variant with the chr, pos, ref, and alt as given. Returns null if no variant found. 
	 * @param contig
	 * @param start
	 * @param ref
	 * @param alt
	 * @return
	 */
	public VariantRec findRecord(String contig, int start, String ref,
			String alt);
}
