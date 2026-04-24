package org.openlca.app.tools.transfer;

import java.util.List;

import org.openlca.core.model.ProviderType;

final class TransferMatch {

	private final long sourceProcessId;
	private final String sourceProcessName;
	private final long sourceExchangeId;
	private final int sourceExchangeInternalId;
	private final long flowId;
	private final String flowName;
	private final long sourceProviderId;
	private final byte sourceProviderType;
	private final String sourceProviderRefId;
	private final String sourceProviderName;
	private final String sourceProviderLocation;
	private final List<ProviderCandidate> candidates;
	private ProviderCandidate selectedCandidate;

	TransferMatch(
		long sourceProcessId,
		String sourceProcessName,
		long sourceExchangeId,
		int sourceExchangeInternalId,
		long flowId,
		String flowName,
		long sourceProviderId,
		byte sourceProviderType,
		String sourceProviderRefId,
		String sourceProviderName,
		String sourceProviderLocation,
		List<ProviderCandidate> candidates,
		ProviderCandidate selectedCandidate
	) {
		this.sourceProcessId = sourceProcessId;
		this.sourceProcessName = sourceProcessName;
		this.sourceExchangeId = sourceExchangeId;
		this.sourceExchangeInternalId = sourceExchangeInternalId;
		this.flowId = flowId;
		this.flowName = flowName;
		this.sourceProviderId = sourceProviderId;
		this.sourceProviderType = sourceProviderType;
		this.sourceProviderRefId = sourceProviderRefId;
		this.sourceProviderName = sourceProviderName;
		this.sourceProviderLocation = sourceProviderLocation;
		this.candidates = candidates != null ? List.copyOf(candidates) : List.of();
		this.selectedCandidate = selectedCandidate;
	}

	long sourceProcessId() {
		return sourceProcessId;
	}

	String sourceProcessName() {
		return sourceProcessName;
	}

	long sourceExchangeId() {
		return sourceExchangeId;
	}

	int sourceExchangeInternalId() {
		return sourceExchangeInternalId;
	}

	long flowId() {
		return flowId;
	}

	String flowName() {
		return flowName;
	}

	long sourceProviderId() {
		return sourceProviderId;
	}

	byte sourceProviderType() {
		return sourceProviderType;
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

	String sourceProviderLabel() {
		var name = sourceProviderName != null && !sourceProviderName.isBlank()
			? sourceProviderName
			: sourceProviderRefId;
		if (name == null || name.isBlank()) {
			return "none";
		}
		var label = new StringBuilder(name);
		if (sourceProviderLocation != null && !sourceProviderLocation.isBlank()) {
			label.append(" [").append(sourceProviderLocation).append(']');
		}
		return label.append(" (").append(typeLabel(sourceProviderType)).append(')').toString();
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

	private static String typeLabel(byte providerType) {
		return switch (providerType) {
			case ProviderType.SUB_SYSTEM -> "System";
			case ProviderType.RESULT -> "Result";
			default -> "Process";
		};
	}
}
