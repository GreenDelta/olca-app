package org.openlca.app;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple cache for short data transfer (e.g. between two editor pages).
 */
public class AppCache {

	private Map<String, Object> map = new HashMap<>();

	AppCache() {
	}

	public void put(String key, Object val) {
		map.put(key, val);
	}

	public <T> T remove(String key, Class<T> type) {
		Object o = map.remove(key);
		return castSave(o, type);
	}

	public <T> T get(String key, Class<T> type) {
		Object o = map.get(key);
		return castSave(o, type);
	}

	private <T> T castSave(Object o, Class<T> type) {
		if (type.isInstance(o))
			return type.cast(o);
		return null;
	}

}
