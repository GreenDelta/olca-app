package org.openlca.app.results.slca;

import gnu.trove.map.hash.TLongObjectHashMap;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.RiskLevel;
import org.openlca.core.model.descriptors.SocialIndicatorDescriptor;

class SocialLevelMatrix {

	private final TLongObjectHashMap<TLongObjectHashMap<RiskLevel>> data;

	SocialLevelMatrix() {
		data = new TLongObjectHashMap<>();
	}

	void put(SocialIndicatorDescriptor indicator, TechFlow techFlow, RiskLevel level) {
		if (indicator == null || techFlow == null || level == null)
			return;
		var m = data.get(techFlow.providerId());
		if (m == null) {
			m = new TLongObjectHashMap<>();
			data.put(techFlow.providerId(), m);
		}
		m.put(indicator.id, level);
	}

	RiskLevel get(SocialIndicatorDescriptor indicator, TechFlow techFlow) {
		var m = data.get(techFlow.providerId());
		return m != null
				? m.get(indicator.id)
				: null;
	}
}
