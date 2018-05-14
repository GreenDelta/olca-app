package org.openlca.app.db;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A simple cache for short data transfer (e.g. between two editor pages).
 */
public class AppCache {

	private Map<String, Object> map = new HashMap<>();

	public AppCache() {
	}

	public void put(String key, Object val) {
		map.put(key, val);			
	}

	/**
	 * Puts the given object into the cache. Allocates a new key and resturns
	 * this key.
	 */
	public String put(Object val) {
		String key = UUID.randomUUID().toString();
		put(key, val);
		return key;
	}

	public <T> T remove(String key, Class<T> type) {
		Object o = map.remove(key);
		return castSave(o, type);
	}
	
	private <T> T castSave(Object o, Class<T> type) {
		if (type.isInstance(o))
			return type.cast(o);
		return null;
	}

}
