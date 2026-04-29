package org.openlca.app.tools.transfer;

import java.util.ArrayList;
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
import org.openlca.io.olca.systransfer.MatchingStrategy;
import org.openlca.io.olca.systransfer.TransferConfig;

class TargetSelection {

	private final IDatabase source;
	private final List<DatabaseConfig> targets;
	private final List<ProductSystemDescriptor> systems;
	private final List<MatchingStrategy> strategies;

	private DatabaseConfig target;
	private ProductSystemDescriptor system;

	private TargetSelection(
		IDatabase source,
		List<DatabaseConfig> targets,
		List<ProductSystemDescriptor> systems) {
		this.source = source;
		this.targets = targets;
		this.systems = systems;
		this.strategies = new ArrayList<>(List.of(MatchingStrategy.values()));
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

	List<MatchingStrategy> strategies() {
		return strategies;
	}

	void setTarget(DatabaseConfig target) {
		this.target = target;
	}

	void setSystem(ProductSystemDescriptor system) {
		this.system = system;
	}

	void moveStrategy(MatchingStrategy strategy, int delta) {
		if (strategy == null || delta == 0)
			return;
		int index = strategies.indexOf(strategy);
		int next = index + delta;
		if (index < 0 || next < 0 || next >= strategies.size())
			return;
		strategies.remove(index);
		strategies.add(next, strategy);
	}

	void removeStrategy(MatchingStrategy strategy) {
		if (strategy != null) {
			strategies.remove(strategy);
		}
	}

	boolean canMoveUp(MatchingStrategy strategy) {
		return strategies.indexOf(strategy) > 0;
	}

	boolean canMoveDown(MatchingStrategy strategy) {
		int index = strategies.indexOf(strategy);
		return index >= 0 && index < strategies.size() - 1;
	}

	boolean isComplete() {
		return source != null
			&& target != null
			&& system != null
			&& !strategies.isEmpty();
	}

	Res<TransferSetup> openConfig() {
		if (!isComplete())
			return Res.error("The selection is not complete");
		try {
			var system = source.get(ProductSystem.class, this.system.id);
			if (system == null)
				return Res.error("Failed to load product system");
			var target = this.target.connect(Workspace.dbDir());
			var config = new TransferConfig(
				source,
				target,
				system,
				strategies.toArray(MatchingStrategy[]::new));
			return Res.ok(new TransferSetup(config));
		} catch (Exception e) {
			return Res.error("Failed to create target configuration", e);
		}
	}

	record TransferSetup(TransferConfig config) implements AutoCloseable {

		@Override
		public void close() {
			if (config == null || config.target() == null)
				return;
			try {
				config.target().close();
			} catch (Exception e) {
				// ignore close errors of the temporary target connection
			}
		}
	}
}
