package org.openlca.app.collaboration.util;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.openlca.core.model.ModelType;
import org.openlca.git.model.Reference;

public class ModelTypeRefIdMap extends EnumMap<ModelType, Set<String>> {

	private static final long serialVersionUID = 3653014653471400763L;

	public ModelTypeRefIdMap() {
		super(ModelType.class);
	}

	public <V> ModelTypeRefIdMap(Collection<V> col, Function<V, Reference> converter) {
		this();
		putAll(col, converter);
	}

	public <V> ModelTypeRefIdMap(Collection<Reference> col) {
		this();
		putAll(col);
	}

	public <V> void putAll(Collection<V> col, Function<V, Reference> converter) {
		putAll(col.stream().map(converter).toList());
	}

	public void putAll(Collection<Reference> col) {
		col.forEach(ref -> computeIfAbsent(ref.type, t -> new HashSet<>()).add(ref.refId));
	}

	public boolean contains(Reference ref) {
		var refIds = get(ref.type);
		if (refIds == null)
			return false;
		return refIds.contains(ref.refId);
	}

}
