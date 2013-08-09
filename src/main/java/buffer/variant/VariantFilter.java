package buffer.variant;

/**
 * Generic interface for things that can filter VariantRec's
 * @author brendan
 *
 */
public interface VariantFilter {

	/**
	 * Whether the given record passes this filter 
	 * @param rec
	 * @return
	 */
	public boolean passes(VariantRec rec);
}
