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
	 * Puts the given object into the cache. Allocates a new key and returns it.
	 */
	public String put(Object val) {
		String key = UUID.randomUUID().toString();
		put(key, val);
		return key;
	}

	public <T> T remove(String key, Class<T> type) {
		Object o = map.remove(key);
		if (type.isInstance(o))
			return type.cast(o);
		return null;
	}

	/**
	 * Removes and returns the object with the given ID from the cache. It makes
	 * an unsafe cast to T, so you be sure what you do here.
	 */
	@SuppressWarnings("unchecked")
	public <T> T remove(String key) {
		return (T) map.remove(key);
	}

}
