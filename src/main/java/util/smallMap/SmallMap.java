package util.smallMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is designed to be a more memory-efficient map. It uses a HashMap to store the values,
 * but doesn't instantiate the map until forced to (by a put..). WHen the map is created, the 
 * capacity and load factor are set to values that favor memory usage, but not performance. 
 * @author brendan
 *
 * @param <K>
 * @param <V>
 */
public class SmallMap<K, V> implements Map<K, V> {

	private int CAPACITY = 2; //defaults to 16, bigger values mean more memory 
	private float LOADFACTOR = 4; //defaults to 0.75, bigger values mean less memory
	
	private Map<K, V> map = null; //We instantiate only when we need to
	
		
	@Override
	public void clear() {
		map = null;
	}

	@Override
	public boolean containsKey(Object key) {
		if (map == null) {
			return false;
		} else {
			return map.containsKey(key);
		}
	}

	@Override
	public boolean containsValue(Object val) {
		if (map == null) {
			return false;
		} else {
			return map.containsKey(val);
		}
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		if (map == null) {
			return new HashSet<java.util.Map.Entry<K,V>>();
		} else {
			return map.entrySet();
		}
		
	}

	@Override
	public V get(Object key) {
		if (map == null) {
			return null;
		} else {
			return map.get(key);
		}
	}

	@Override
	public boolean isEmpty() {
		if (map == null) {
			return true;
		} else {
			return map.isEmpty();
		}
	}

	@Override
	public Set<K> keySet() {
		if (map == null) {
			return new HashSet<K>();
		} else {
			return map.keySet();
		}
	}

	@Override
	public V put(K key, V val) {
		if (map == null) {
			map = new HashMap(CAPACITY, LOADFACTOR);
		}
		
		return map.put(key, val);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> arg0) {
		if (map == null) {
			map = new HashMap(CAPACITY, LOADFACTOR);
		}
		map.putAll(arg0);
	}

	@Override
	public V remove(Object arg0) {
		if (map == null) {
			return null;
		} else {
			return map.remove(arg0);
		}
		
	}

	@Override
	public int size() {
		if (map==null) {
			return 0;
		}
		return map.size();
	}

	@Override
	public Collection<V> values() {
		if (map == null) {
			return new ArrayList<V>();
		}
		return map.values();
	}
	

}
