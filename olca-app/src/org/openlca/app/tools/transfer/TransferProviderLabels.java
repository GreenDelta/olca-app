package org.openlca.app.tools.transfer;

import org.openlca.commons.Strings;
import org.openlca.io.olca.systransfer.ProviderInfo;

final class TransferProviderLabels {

	private TransferProviderLabels() {
	}

	static String of(ProviderInfo info) {
		if (info == null)
			return null;
		var label = providerOnly(info);
		var flowName = info.flow() != null ? info.flow().name : null;
		if (!Strings.isBlank(flowName)) {
			label += " [" + flowName.strip() + "]";
		}
		return label;
	}

	static String providerOnly(ProviderInfo info) {
		if (info == null || info.provider() == null)
			return null;
		var label = Strings.isBlank(info.provider().name)
			? "Unnamed provider"
			: info.provider().name.strip();
		var locationCode = info.location() != null ? info.location().code : null;
		if (!Strings.isBlank(locationCode)) {
			label += " - " + locationCode.strip();
		}
		return label;
	}
}
