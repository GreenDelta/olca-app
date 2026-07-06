package org.openlca.app.tools.migration;

import java.util.List;

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
		var sa = labelOf(a);
		var sb = labelOf(b);
		return Strings.compareIgnoreCase(sa, sb);
	}

	/// We cannot use `Labels.name` for matched providers in the target database,
	/// as `Labels.name` may look into the current database for location suffixes
	/// etc.
	static String labelOf(ProviderInfo info) {
		if (info == null)
			return null;
		var label = info.provider() != null
			? info.provider().name
			: null;
		if (label == null)
			return "-";
		return info.location() != null && Strings.isNotBlank(info.location().code)
			? label + " - " + info.location().code
			: label;
	}
}
