package org.openlca.app.editors.processes.social;

import java.util.Objects;

import org.openlca.core.model.Process;
import org.openlca.core.model.SocialAspect;
import org.openlca.core.model.SocialIndicator;
import org.openlca.core.model.descriptors.BaseDescriptor;

class Aspects {

	private Aspects() {
	}

	static SocialAspect find(Process p, BaseDescriptor indicator) {
		if (p == null || indicator == null)
			return null;
		for (SocialAspect a : p.socialAspects) {
			if (a.indicator == null)
				continue;
			if (a.indicator.id == indicator.id)
				return a;
		}
		return null;
	}

	static SocialAspect find(Process p, SocialIndicator i) {
		if (p == null || i == null)
			return null;
		for (SocialAspect a : p.socialAspects) {
			if (Objects.equals(a.indicator, i))
				return a;
		}
		return null;
	}

	static void update(Process p, SocialAspect a) {
		if (p == null || a == null)
			return;
		SocialAspect pa = find(p, a.indicator);
		if (pa == null)
			return;
		copyValues(a, pa);
	}

	static void remove(Process p, SocialAspect a) {
		if (p == null || a == null)
			return;
		SocialAspect pa = find(p, a.indicator);
		if (pa == null)
			return;
		p.socialAspects.remove(pa);
	}

	static void copyValues(SocialAspect from, SocialAspect to) {
		if (from == null || to == null)
			return;
		to.activityValue = from.activityValue;
		to.comment = from.comment;
		to.indicator = from.indicator;
		to.quality = from.quality;
		to.rawAmount = from.rawAmount;
		to.riskLevel = from.riskLevel;
		to.source = from.source;
	}

}
