package org.openlca.app.editors.graphical.search;

import java.util.Objects;

import org.openlca.app.util.Labels;
import org.openlca.core.model.descriptors.RootDescriptor;

/**
 * Contains the data of a possible connection candidate.
 */
class LinkCandidate implements Comparable<LinkCandidate> {

	/// The provider or process of this candidate.
	final RootDescriptor process;

	/// The exchange ID of this link candidate.
	final long exchangeId;

	/// Indicates whether the provider or process is already
	/// in the product system.
	final boolean isInSystem;

	/// In case the link candidate is a provider, this indicates
	/// whether the provider is already connected to the original
	/// exchange.
	final boolean isConnected;

	/// In case the link candidate is a provider, this indicates
	/// whether the provider is the default provider of the original
	/// exchange.
	final boolean isDefaultProvider;

	boolean doConnect;
	boolean doCreate;

	private LinkCandidate(
			ModelExchange exchange, RootDescriptor process, long exchangeId
	) {
		this.process = process;
		this.exchangeId = exchangeId;
		this.isInSystem = exchange.graph.getProductSystem()
				.processes.contains(process.id);

		var linkSearch = exchange.graph.linkSearch;
		if (exchange.isProvider) {
			// check if the candidate exchange is linked to the provider
			this.isConnected = linkSearch.getConsumerLinks(process.id)
					.stream()
					.anyMatch(link -> link.exchangeId == exchangeId
							&& link.providerId == exchange.process.id);
		} else {
			this.isConnected = linkSearch.getProviderLinks(process.id)
					.stream()
					.anyMatch(link -> link.exchangeId == exchange.exchange.id);
		}
		this.isDefaultProvider = !exchange.isProvider
				&& exchange.exchange.defaultProviderId == process.id;
	}

	static LinkCandidate of(
			ModelExchange exchange, RootDescriptor process, long exchangeId
	) {
		return new LinkCandidate(exchange, process, exchangeId);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LinkCandidate other))
			return false;
		if (other.process == null)
			return process == null;
		return Objects.equals(other.process, process)
				&& exchangeId == other.exchangeId;
	}

	@Override
	public int compareTo(LinkCandidate o) {
		if (isDefaultProvider != o.isDefaultProvider)
			return isDefaultProvider ? -1 : 1;
		if (isConnected != o.isConnected)
			return isConnected ? -1 : 1;
		if (isInSystem != o.isInSystem)
			return isInSystem ? -1 : 1;
		String n1 = Labels.name(process);
		String n2 = Labels.name(o.process);
		return n1.toLowerCase().compareTo(n2.toLowerCase());
	}

}
