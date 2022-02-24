package org.openlca.app.collaboration.util;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import org.openlca.core.model.ModelType;
import org.openlca.git.model.Reference;

public class TypeRefIdSet {

	private final EnumMap<ModelType, Set<String>> map = new EnumMap<>(ModelType.class);

	public TypeRefIdSet() {
	}

	public TypeRefIdSet(Collection<Reference> col) {
		putAll(col);
	}

	public void putAll(Collection<Reference> col) {
		col.forEach(this::add);
	}

	public void add(Reference ref) {
		add(ref.type, ref.refId);
	}

	public void add(ModelType type, String refId) {
		map.computeIfAbsent(type, t -> new HashSet<>()).add(refId);
	}

	public boolean contains(Reference ref) {
		return contains(ref.type, ref.refId);
	}

	public boolean contains(ModelType type, String refId) {
		var refIds = map.get(type);
		if (refIds == null)
			return false;
		return refIds.contains(refId);
	}

	public void clear() {
		map.clear();
	}

	public void forEach(BiConsumer<ModelType, String> forEach) {
		map.keySet().forEach(type -> {
			var refIds = map.get(type);
			if (refIds == null)
				return;
			refIds.forEach(refId -> forEach.accept(type, refId));
		});
	}

	public interface ForEach {

		void apply(ModelType type, String refId);

	}

}
