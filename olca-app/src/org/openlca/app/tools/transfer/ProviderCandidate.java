package org.openlca.app.tools.transfer;

import java.util.Objects;

import org.openlca.core.model.Exchange;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProviderType;
import org.openlca.core.model.Result;

/// A possible target-side provider that can be used when creating a process
/// link in a transferred product system.
public record ProviderCandidate(
	long flowId,
	long providerId,
	byte providerType,
	String providerRefId,
	String providerName,
	String providerLocation
) {

	static ProviderCandidate of(Process process, Exchange exchange) {
		return new ProviderCandidate(
			exchange.flow != null ? exchange.flow.id : 0,
			process.id,
			ProviderType.PROCESS,
			process.refId,
			process.name,
			process.location != null ? process.location.code : null);
	}

	static ProviderCandidate of(ProductSystem system) {
		if (system == null
			|| system.referenceExchange == null
			|| system.referenceExchange.flow == null)
			return null;
		return new ProviderCandidate(
			system.referenceExchange.flow.id,
			system.id,
			ProviderType.SUB_SYSTEM,
			system.refId,
			system.name,
			locationOf(system.referenceProcess));
	}

	static ProviderCandidate of(Result result) {
		if (result == null
			|| result.referenceFlow == null
			|| result.referenceFlow.flow == null)
			return null;
		var system = result.productSystem;
		return new ProviderCandidate(
			result.referenceFlow.flow.id,
			result.id,
			ProviderType.RESULT,
			result.refId,
			result.name,
			system != null ? locationOf(system.referenceProcess) : null);
	}

	boolean hasFlow(long flowId) {
		return this.flowId == flowId;
	}

	boolean matchesRefId(String refId) {
		return Objects.equals(providerRefId, refId);
	}

	boolean matchesNameAndLocation(String name, String location) {
		return Objects.equals(providerName, name)
			&& Objects.equals(providerLocation, location);
	}

	String label() {
		var name = providerName != null && !providerName.isBlank()
			? providerName
			: providerRefId;
		if (name == null || name.isBlank()) {
			name = "unknown provider";
		}
		var label = new StringBuilder(name);
		if (providerLocation != null && !providerLocation.isBlank()) {
			label.append(" [").append(providerLocation).append(']');
		}
		return label.append(" (").append(typeLabel()).append(')').toString();
	}

	private String typeLabel() {
		return switch (providerType) {
			case ProviderType.SUB_SYSTEM -> "System";
			case ProviderType.RESULT -> "Result";
			default -> "Process";
		};
	}

	private static String locationOf(Process process) {
		return process != null && process.location != null
			? process.location.code
			: null;
	}
}
