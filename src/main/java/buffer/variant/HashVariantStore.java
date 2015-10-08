package buffer.variant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A type of variant store that keeps everything in a Map instead of an ArrayList. This is less memory-efficient
 * than a traditional VariantPool, but allows for much faster findRecords() and addRecords()
 * @author brendan
 *
 */
public class HashVariantStore implements VariantStore {

	Map<String, Map<String, VariantRec>> store = new HashMap<String, Map<String, VariantRec>>(25);
	
	public HashVariantStore() {
		//blank on purpose, create an empty store
	}
	
	public HashVariantStore(VariantLineReader reader) throws IOException {
		do {
			VariantRec rec = reader.toVariantRec();
			
			if (rec == null) {
				if (reader.getCurrentLine() != null && reader.getCurrentLine().length()>0)
					System.err.println("Warning, could not import variant from line: " + reader.getCurrentLine() );
			}
			else {
				this.addRecord(rec);
			}
		} while (reader.advanceLine());
		
	}
	
	@Override
	public void addRecord(VariantRec rec) {
		Map<String, VariantRec> contig = store.get(rec.getContig());
		if (contig == null) {
			contig = new HashMap<String, VariantRec>(1024, 0.5f);
			store.put(rec.getContig(), contig);
		}
		contig.put( toHashKey(rec), rec);
	}

	
	private String toHashKey(int start, String ref, String alt) {
		return "" + start + ":" + ref + ":" + alt;
	}
	
	private String toHashKey(VariantRec rec) {
		return toHashKey(rec.getStart(), rec.getRef(), rec.getAlt());
	}

	@Override
	public void addAll(VariantStore toAdd, boolean allowDuplicates) {
		if (allowDuplicates == true) {
			throw new IllegalArgumentException("HashVariantStores do not permit duplicate entries");
		}
		
		for(String contig : toAdd.getContigs()) {
			Map<String, VariantRec> storeVars = store.get(contig);
			for(VariantRec var : toAdd.getVariantsForContig(contig)) {
				if (storeVars == null) {
					addRecord(var);
					storeVars = store.get(contig);
					continue;
				} else {		
				 if (!storeVars.containsKey( toHashKey(var))) {
					addRecord(var);
				 }
				}
			}
		}
	}

	@Override
	public Collection<VariantRec> getVariantsForContig(String contig) {
		Map<String, VariantRec> vars = store.get(contig);
		if (vars == null) {
			return new ArrayList<VariantRec>();
		} else {
			return vars.values();
		}
		
	}

	@Override
	public Collection<String> getContigs() {
		return store.keySet();
	}

	@Override
	public int getContigCount() {
		return store.size();
	}

	@Override
	public int size() {
		int total = 0;
		for(Map<String, VariantRec> entry : store.values()) {
			total+=entry.size();
		}
		return total;
	}

	@Override
	public VariantRec findRecord(String contig, int start, String ref, String alt) {
		Map<String, VariantRec> vars = store.get(contig);
		if (vars == null) {
			return null;
		}
		return vars.get( toHashKey(start, ref, alt));
	}

}
