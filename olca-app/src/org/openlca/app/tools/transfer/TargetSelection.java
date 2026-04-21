package org.openlca.app.tools.transfer;

import java.util.List;

import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.Workspace;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;

class TargetSelection {

	private final IDatabase source;
	private final List<DatabaseConfig> targets;
	private final List<ProductSystemDescriptor> systems;

	private DatabaseConfig target;
	private ProductSystemDescriptor system;
	private LinkingStrategy strategy;

	private TargetSelection(
		IDatabase source,
		List<DatabaseConfig> targets,
		List<ProductSystemDescriptor> systems) {
		this.source = source;
		this.targets = targets;
		this.systems = systems;
	}

	static Res<TargetSelection> load() {
		var source = Database.get();
		if (source == null)
			return Res.error(M.NoDatabaseOpenedInfo);

		var targets = Database.getConfigurations().getAll().stream()
			.filter(c -> !Database.isActive(c))
			.sorted((ci, cj) -> Strings.compareIgnoreCase(ci.name(), cj.name()))
			.toList();
		if (targets.isEmpty()) {
			return Res.error(
				"There are no possible target databases in the workspace.");
		}

		var systems = source.getDescriptors(ProductSystem.class).stream()
			.filter(d -> d instanceof ProductSystemDescriptor)
			.map(d -> (ProductSystemDescriptor) d)
			.sorted((di, dj) -> Strings.compareIgnoreCase(di.name, dj.name))
			.toList();
		if (systems.isEmpty()) {
			return Res.error(
				"There are no product systems in the currently active database");
		}

		var config = new TargetSelection(source, targets, systems);
		return Res.ok(config);
	}

	List<DatabaseConfig> targets() {
		return targets;
	}

	List<ProductSystemDescriptor> systems() {
		return systems;
	}

	void setTarget(DatabaseConfig target) {
		this.target = target;
	}

	void setSystem(ProductSystemDescriptor system) {
		this.system = system;
	}

	void setStrategy(LinkingStrategy strategy) {
		this.strategy = strategy;
	}

	boolean isComplete() {
		return source != null
			&& target != null
			&& system != null
			&& strategy != null;
	}

	Res<TransferConfig> open() {
		if (!isComplete())
			return Res.error("The selection is not complete");

		try {
			var system = source.get(ProductSystem.class, this.system.id);
			if (system == null)
				return Res.error("Failed to load product system");
			var target = this.target.connect(Workspace.dbDir());
			var existing = target.get(ProductSystem.class, system.refId);
			if (existing != null) {

				target.close();
			}

			var config = new TransferConfig(source, target, )

		}

	}
}
