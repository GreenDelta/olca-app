package org.openlca.app.tools.transfer;

import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;

public record TransferTargetSelection(
	ProductSystemDescriptor productSystem,
	DatabaseConfig targetDatabase,
	ProviderLinkingStrategy providerLinkingStrategy
) {
}