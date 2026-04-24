package org.openlca.app.tools.transfer;

import java.util.List;

import org.openlca.app.util.Labels;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;

final class TransferMatch {

	private final Descriptor provider;
	private final List<ProviderCandidate> candidates;
	private ProviderCandidate selectedCandidate;

	TransferMatch(
		Descriptor provider,
		List<ProviderCandidate> candidates,
		ProviderCandidate selectedCandidate
	) {
		this.provider = provider != null ? provider.copy() : null;
		this.candidates = candidates != null ? List.copyOf(candidates) : List.of();
		this.selectedCandidate = selectedCandidate;
	}

	Descriptor provider() {
		return provider;
	}

	List<ProviderCandidate> candidates() {
		return candidates;
	}

	ProviderCandidate selectedCandidate() {
		return selectedCandidate;
	}

	void select(ProviderCandidate candidate) {
		if (candidate == null || candidates.contains(candidate)) {
			selectedCandidate = candidate;
		}
	}

	boolean hasCandidates() {
		return !candidates.isEmpty();
	}

	boolean hasSelection() {
		return selectedCandidate != null;
	}

	String providerLabel() {
		if (provider == null) {
			return "none";
		}
		var name = provider instanceof ProcessDescriptor p
			? Labels.asProviderName(p)
			: Labels.name(provider);
		if (name == null || name.isBlank()) {
			name = provider.refId;
		}
		return name != null ? name : "none";
	}

	String selectedLabel() {
		return selectedCandidate != null
			? selectedCandidate.label()
			: "none";
	}

	String status() {
		if (selectedCandidate != null)
			return "matched";
		if (!candidates.isEmpty())
			return "selectable";
		return "no candidate";
	}
}
