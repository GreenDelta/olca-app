package org.openlca.app.tools.migration;

import java.util.List;

import org.openlca.app.util.Labels;
import org.openlca.commons.Strings;
import org.openlca.io.olca.migration.ProviderInfo;
import org.openlca.io.olca.migration.ProviderMatch;

class Util {

	private Util() {
	}

	static List<ProviderMatch> sortedMatches(List<ProviderMatch> matches) {
		if (matches == null)
			return List.of();
		if (!matches.isEmpty()) {
			matches.sort(Util::compareMatches);
		}
		return matches;
	}

	static List<ProviderInfo> sortedInfos(List<ProviderInfo> infos) {
		if (infos == null)
			return List.of();
		if (!infos.isEmpty()) {
			infos.sort(Util::compareInfos);
		}
		return infos;
	}

	private static int compareMatches(ProviderMatch a, ProviderMatch b) {
		if (a == b) return 0;
		if (a == null) return -1;
		if (b == null) return 1;
		int c = compareInfos(a.source(), b.source());
		return c == 0
			? compareInfos(a.selected(), b.selected())
			: c;
	}

	private static int compareInfos(ProviderInfo a, ProviderInfo b) {
		if (a == b) return 0;
		if (a == null) return -1;
		if (b == null) return 1;
		var sa = Labels.name(a.provider());
		var sb = Labels.name(b.provider());
		return Strings.compareIgnoreCase(sa, sb);
	}
}
