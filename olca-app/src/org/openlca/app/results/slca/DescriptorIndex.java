package org.openlca.app.results.slca;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openlca.core.matrix.index.IndexConsumer;
import org.openlca.core.matrix.index.MatrixIndex;
import org.openlca.core.model.descriptors.Descriptor;
import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TLongIntHashMap;

/**
 * Maps a set of descriptors to a matrix index. Note that the index maps the IDs
 * of the descriptors so that each descriptor needs a unique ID &gt; 0 (which is
 * the case when the related object is stored in a database).
 */
public class DescriptorIndex<D extends Descriptor> implements MatrixIndex<D> {

	/**
	 * Contains the ordered content of the index.
	 */
	protected final ArrayList<D> content = new ArrayList<>();

	/**
	 * Maps the ID of a descriptor to the position of that descriptor in the
	 * index.
	 */
	private final TLongIntHashMap index = new TLongIntHashMap(
			Constants.DEFAULT_CAPACITY,
			Constants.DEFAULT_LOAD_FACTOR,
			0L, // no entry key
			-1); // no entry value

	/**
	 * Returns the number of entries in the index.
	 */
	@Override
	public int size() {
		return content.size();
	}

	/**
	 * Returns true when this index is empty.
	 */
	@Override
	public boolean isEmpty() {
		return content.isEmpty();
	}

	/**
	 * Get the descriptor at the given position or {@code null} when there is no
	 * descriptor mapped to this position.
	 */
	@Override
	public D at(int i) {
		if (i < 0 || i >= content.size())
			return null;
		return content.get(i);
	}

	/**
	 * Get the ID of the descriptor at the given position.
	 */
	public long idAt(int i) {
		var d = at(i);
		return d == null ? 0L : d.id;
	}

	/**
	 * Returns the position of the given descriptor. If the descriptor is not
	 * contained in this index, -1 is returned.
	 */
	@Override
	public int of(D d) {
		return d != null
				? index.get(d.id)
				: -1;
	}

	/**
	 * Returns the position of the descriptor with the given ID. If the descriptor
	 * is not contained in this index, -1 is returned.
	 */
	public int of(long id) {
		return index.get(id);
	}

	/**
	 * Returns {@code true} when the given descriptor is contained in this index.
	 */
	@Override
	public boolean contains(D d) {
		return of(d) >= 0;
	}

	/**
	 * Returns {@code true} when the descriptor with the given ID is part of this
	 * index.
	 */
	public boolean contains(long id) {
		return of(id) >= 0;
	}

	/**
	 * Adds the given descriptor to this index if it is not yet contained. The
	 * position of the descriptor in this index is returned.
	 */
	@Override
	public int add(D d) {
		if (d == null)
			return -1;
		int idx = of(d);
		if (idx >= 0)
			return idx;
		idx = content.size();
		content.add(d);
		index.put(d.id, idx);
		return idx;
	}

	/**
	 * Calls the given function for each descriptor in this index.
	 */
	@Override
	public void each(IndexConsumer<D> fn) {
		if (fn == null)
			return;
		for (int i = 0; i < content.size(); i++) {
			fn.accept(i, content.get(i));
		}
	}

	/**
	 * Returns the descriptors of this index.
	 */
	@Override
	public Set<D> content() {
		return new HashSet<>(content);
	}

	@Override
	public DescriptorIndex<D> copy() {
		var copy = new DescriptorIndex<D>();
		for (var d : content) {
			copy.add(d);
		}
		return copy;
	}

	@Override
	public Iterator<D> iterator() {
		return Collections.unmodifiableList(content).iterator();
	}

	public D getForId(long id) {
		int pos = of(id);
		return pos >= 0
				? at(pos)
				: null;
	}
}
