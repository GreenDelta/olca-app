package org.openlca.app.results.slca;

import org.openlca.core.model.RiskLevel;
import org.openlca.core.model.descriptors.SocialIndicatorDescriptor;

public record SocialRiskEntry(
	SocialIndicatorDescriptor indicator,
	RiskLevel level) {

	public static SocialRiskEntry of(
		SocialIndicatorDescriptor indicator, RiskLevel level) {
		return new SocialRiskEntry(indicator, level);
	}
}
