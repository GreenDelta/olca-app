package org.openlca.app.results.slca;

import org.openlca.core.matrix.index.IndexConsumer;
import org.openlca.core.matrix.index.MatrixIndex;
import org.openlca.core.model.RiskLevel;
import org.openlca.core.model.descriptors.SocialIndicatorDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SocialRiskIndex implements MatrixIndex<SocialRiskEntry> {

	private final ArrayList<SocialRiskEntry> content;
	private final Map<SocialIndicatorDescriptor, Map<RiskLevel, Integer>> index;

	private SocialRiskIndex() {
		content = new ArrayList<>();
		index = new HashMap<>();
	}

	public static SocialRiskIndex of(List<SocialIndicatorDescriptor> ds) {
		if (ds == null || ds.isEmpty())
			return empty();
		var idx = new SocialRiskIndex();
		for (var indicator : ds) {
			for (var level : RiskLevel.values()) {
				idx.add(SocialRiskEntry.of(indicator, level));
			}
		}
		return idx;
	}

	public static SocialRiskIndex empty() {
		return new SocialRiskIndex();
	}

	public Set<SocialIndicatorDescriptor> indicators() {
		return Collections.unmodifiableSet(index.keySet());
	}

	@Override
	public int size() {
		return content.size();
	}

	@Override
	public boolean isEmpty() {
		return content.isEmpty();
	}

	@Override
	public SocialRiskEntry at(int i) {
		return i >= 0 && i < content.size()
				? content.get(i)
				: null;
	}

	@Override
	public int of(SocialRiskEntry e) {
		return e != null
				? of(e.indicator(), e.level())
				: -1;
	}

	public int of(SocialIndicatorDescriptor d, RiskLevel r) {
		if (d == null || r == null)
			return -1;
		var m = index.get(d);
		if (m == null)
			return -1;
		var v = m.get(r);
		return v != null ? v : -1;
	}

	@Override
	public boolean contains(SocialRiskEntry e) {
		return e != null && of(e) >= 0;
	}

	@Override
	public int add(SocialRiskEntry e) {
		return e != null
				? add(e.indicator(), e.level())
				: -1;
	}

	public int add(SocialIndicatorDescriptor d, RiskLevel level) {
		if (d == null || level == null)
			return -1;
		var m = index.computeIfAbsent(d, $ -> new EnumMap<>(RiskLevel.class));
		return m.computeIfAbsent(level, $ -> {
			int i = content.size();
			var entry = SocialRiskEntry.of(d, level);
			content.add(entry);
			return i;
		});
	}

	@Override
	public void each(IndexConsumer<SocialRiskEntry> fn) {
		for (int i = 0; i < content.size(); i++) {
			fn.accept(i, content.get(i));
		}
	}

	public void eachOf(
			SocialIndicatorDescriptor d, IndexConsumer<SocialRiskEntry> fn) {
		if (d == null || fn == null)
			return;
		var m = index.get(d);
		if (m == null)
			return;
		for (var v : m.values()) {
			if (v == null)
				continue;
			int i = v;
			if (i < 0 || i >= content.size())
				continue;
			fn.accept(i, content.get(i));
		}
	}

	@Override
	public Set<SocialRiskEntry> content() {
		return new HashSet<>(content);
	}

	@Override
	public MatrixIndex<SocialRiskEntry> copy() {
		var copy = new SocialRiskIndex();
		for (var e : content) {
			copy.add(e);
		}
		return copy;
	}

	@Override
	public Iterator<SocialRiskEntry> iterator() {
		return Collections.unmodifiableList(content).iterator();
	}
}
