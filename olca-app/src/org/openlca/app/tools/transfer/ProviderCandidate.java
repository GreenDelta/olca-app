package org.openlca.app.tools.transfer;

import java.util.Objects;

import org.openlca.app.util.Labels;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProviderType;
import org.openlca.core.model.Result;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;

/// A possible target-side provider that can be used when creating a process
/// link in a transferred product system.
public record ProviderCandidate(
	FlowDescriptor flow,
	Descriptor provider,
	String locationCode
) {

	static ProviderCandidate of(Process process, Exchange exchange) {
		return new ProviderCandidate(
			exchange.flow != null ? org.openlca.core.model.descriptors.Descriptor.of(exchange.flow) : null,
			org.openlca.core.model.descriptors.Descriptor.of(process),
			process.location != null ? process.location.code : null);
	}

	static ProviderCandidate of(ProductSystem system) {
		if (system == null
			|| system.referenceExchange == null
			|| system.referenceExchange.flow == null)
			return null;
		return new ProviderCandidate(
			org.openlca.core.model.descriptors.Descriptor.of(system.referenceExchange.flow),
			org.openlca.core.model.descriptors.Descriptor.of(system),
			locationOf(system.referenceProcess));
	}

	static ProviderCandidate of(Result result) {
		if (result == null
			|| result.referenceFlow == null
			|| result.referenceFlow.flow == null)
			return null;
		var system = result.productSystem;
		return new ProviderCandidate(
			org.openlca.core.model.descriptors.Descriptor.of(result.referenceFlow.flow),
			org.openlca.core.model.descriptors.Descriptor.of(result),
			system != null ? locationOf(system.referenceProcess) : null);
	}

	boolean hasFlow(long flowId) {
		return flow != null && flow.id == flowId;
	}

	boolean matchesRefId(String refId) {
		return provider != null && Objects.equals(provider.refId, refId);
	}

	boolean matchesNameAndLocation(String name, String location) {
		return provider != null
			&& Objects.equals(provider.name, name)
			&& Objects.equals(locationCode, location);
	}

	String label() {
		var name = provider instanceof ProcessDescriptor p
			? Labels.asProviderName(p)
			: Labels.name(provider);
		if ((name == null || name.isBlank()) && provider != null) {
			name = provider.refId;
		}
		if (name == null || name.isBlank()) {
			name = "unknown provider";
		}
		var label = new StringBuilder(name);
		if (locationCode != null && !locationCode.isBlank()
			&& !(provider instanceof ProcessDescriptor)) {
			label.append(" [").append(locationCode).append(']');
		}
		return label.append(" (").append(typeLabel()).append(')').toString();
	}

	private String typeLabel() {
		var providerType = provider != null && provider.type != null
			? ProviderType.of(provider.type)
			: ProviderType.PROCESS;
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
