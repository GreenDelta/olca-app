package org.openlca.app.tools.transfer;

import java.util.List;

record TransferPlan(
	TransferConfig config,
	List<Long> processIds,
	List<TransferMatch> matches
) {

	TransferPlan {
		processIds = processIds != null ? List.copyOf(processIds) : List.of();
		matches = matches != null ? List.copyOf(matches) : List.of();
	}

	int matchedCount() {
		int count = 0;
		for (var match : matches) {
			if (match.hasSelection()) {
				count++;
			}
		}
		return count;
	}
}
