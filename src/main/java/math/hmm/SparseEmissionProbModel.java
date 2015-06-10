package math.hmm;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.distribution.AbstractRealDistribution;

/**
 * Maps arrays of emission probability functions to specific integer locations in sparse (hashmap-backed)
 * fashion. 
 * @author brendan
 *
 */
public class SparseEmissionProbModel implements EmissionProbModel {

	private Map<Integer, AbstractRealDistribution[]> funcs = new HashMap<Integer, AbstractRealDistribution[]>();
	private final int dimension; 
	
	public SparseEmissionProbModel(int dimension) {
		this.dimension = dimension;
	}
	
	/**
	 * Add a list of distributions associated with the given position. It's an error if the length
	 * of the dists is not equal to the dimension of the model
	 * @param position
	 * @param dists
	 */
	public void addFuncsAtPos(int position, AbstractRealDistribution[] dists) {
		if (dists.length != this.dimension) {
			throw new IllegalArgumentException("Length of array must be equal to model dimension");
		}
		funcs.put(position, dists);
	}
	
	/**
	 * True if there's an entry defined at the given position
	 * @param pos
	 * @return
	 */
	public boolean hasDistsAtPos(int pos) {
		return funcs.containsKey(pos);
	}
	
	@Override
	public AbstractRealDistribution[] getEmissionProbsForIndex(int position) {
		return funcs.get(position);
	}

	@Override
	public int getDimension() {
		return dimension;
	}

}
