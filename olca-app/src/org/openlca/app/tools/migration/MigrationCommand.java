package org.openlca.app.tools.migration;

import org.openlca.app.rcp.Workspace;
import org.openlca.commons.Res;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;
import org.openlca.io.olca.migration.MigrationConfig;
import org.openlca.io.olca.migration.MigrationExecutor;
import org.openlca.io.olca.migration.MigrationPlan;

record MigrationCommand(
	MigrationPlan plan,
	MigrationConfig config,
	DatabaseConfig targetConfig
) {

	Res<ProductSystemDescriptor> execute() {
		var config = reconnect();
		if (config.isError())
			return config.castError();
		var target = config.value().target();
		try (target) {
			var system = MigrationExecutor.of(plan, config.value()).execute();
			return system.isError()
				? system.wrapError("Failed to transfer product system")
				: Res.ok(Descriptor.of(system.value()));
		} catch (Exception e) {
			return Res.error("Failed to transfer product system", e);
		}
	}

	private Res<MigrationConfig> reconnect() {
		try {
			var target = targetConfig.connect(Workspace.dbDir());
			var c = new MigrationConfig(
				config.source(), target, config.system(), config.strategies());
			return Res.ok(c);
		} catch (Exception e) {
			return Res.error(
				"Failed to open target database: " + targetConfig.name(), e);
		}
	}
}
