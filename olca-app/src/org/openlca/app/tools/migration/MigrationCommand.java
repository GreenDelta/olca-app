package org.openlca.app.tools.migration;

import org.openlca.app.rcp.Workspace;
import org.openlca.commons.Res;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.io.olca.migration.MigrationConfig;
import org.openlca.io.olca.migration.MigrationExecutor;
import org.openlca.io.olca.migration.MigrationPlan;

record MigrationCommand(
	MigrationPlan plan,
	MigrationConfig config,
	DatabaseConfig targetConfig
) {

	Res<Void> execute() {
		var config = reconnect();
		if (config.isError())
			return config.castError();
		var target = config.value().target();
		try (target) {
			return MigrationExecutor.of(plan, config.value()).execute();			
		} catch (Exception e) {
			return Res.error("Migration failed", e);
		}
	}

	private Res<MigrationConfig> reconnect() {
		try {
			var target = targetConfig.connect(Workspace.dbDir());
			var c = new MigrationConfig(
				config.source(), 
				target, 
				config.entities(), 
				config.allProcesses(), 
				config.strategies());
			return Res.ok(c);
		} catch (Exception e) {
			return Res.error(
				"Failed to open target database: " + targetConfig.name(), e);
		}
	}
}
