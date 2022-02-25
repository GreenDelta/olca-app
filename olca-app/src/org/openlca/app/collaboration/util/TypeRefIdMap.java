package org.openlca.app.collaboration.util;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.openlca.core.model.ModelType;
import org.openlca.git.model.Reference;

public class TypeRefIdMap<T> {

	private final EnumMap<ModelType, Map<String, T>> map = new EnumMap<>(ModelType.class);

	public TypeRefIdMap() {
	}

	public void put(Reference ref, T value) {
		put(ref.type, ref.refId, value);
	}

	public void put(ModelType type, String refId, T value) {
		map.computeIfAbsent(type, t -> new HashMap<>()).put(refId, value);
	}

	public boolean contains(Reference ref) {
		return contains(ref.type, ref.refId);
	}

	public boolean contains(ModelType type, String refId) {
		var refIds = map.get(type);
		if (refIds == null)
			return false;
		return refIds.containsKey(refId);
	}

	public T get(Reference ref) {
		return get(ref.type, ref.refId);
	}

	public T get(ModelType type, String refId) {
		var refIds = map.get(type);
		if (refIds == null)
			return null;
		return refIds.get(refId);
	}

	public void clear() {
		map.clear();
	}

}
