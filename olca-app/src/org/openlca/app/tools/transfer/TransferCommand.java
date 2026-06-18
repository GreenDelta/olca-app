package org.openlca.app.tools.transfer;

import org.openlca.app.rcp.Workspace;
import org.openlca.commons.Res;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;
import org.openlca.io.olca.systransfer.TransferConfig;
import org.openlca.io.olca.systransfer.TransferExecutor;
import org.openlca.io.olca.systransfer.TransferPlan;

record TransferCommand(
	TransferPlan plan,
	TransferConfig config,
	DatabaseConfig targetConfig
) {

	Res<ProductSystemDescriptor> execute() {
		var config = reconnect();
		if (config.isError())
			return config.castError();
		var target = config.value().target();
		try (target) {
			var system = TransferExecutor.of(plan, config.value()).execute();
			return system.isError()
				? system.wrapError("Failed to transfer product system")
				: Res.ok(Descriptor.of(system.value()));
		} catch (Exception e) {
			return Res.error("Failed to transfer product system", e);
		}
	}

	private Res<TransferConfig> reconnect() {
		try {
			var target = targetConfig.connect(Workspace.dbDir());
			var c = new TransferConfig(
				config.source(), target, config.system(), config.strategies());
			return Res.ok(c);
		} catch (Exception e) {
			return Res.error(
				"Failed to open target database: " + targetConfig.name(), e);
		}
	}
}
