package org.openlca.app.tools.transfer;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ProductSystem;

public record TransferConfig(
	IDatabase source,
	IDatabase target,
	ProductSystem system,
	LinkingStrategy strategy) {
}
