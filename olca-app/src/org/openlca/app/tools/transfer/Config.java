package org.openlca.app.tools.transfer;

import java.util.List;

import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.commons.Res;
import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;

class Config {

	private final IDatabase source;
	private final List<DatabaseConfig> targets;
	private final List<ProductSystemDescriptor> systems;

	private DatabaseConfig target;
	private ProductSystemDescriptor system;
	private LinkingStrategy strategy;

	private Config(
		IDatabase source,
		List<DatabaseConfig> targets,
		List<ProductSystemDescriptor> systems) {
		this.source = source;
		this.targets = targets;
		this.systems = systems;
	}

	static Res<Config> load() {
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

		var config = new Config(source, targets, systems);
		return Res.ok(config);
	}

	public IDatabase source() {
		return source;
	}

	public List<DatabaseConfig> targets() {
		return targets;
	}

	public List<ProductSystemDescriptor> systems() {
		return systems;
	}

	public DatabaseConfig target() {
		return target;
	}

	public void setTarget(DatabaseConfig target) {
		this.target = target;
	}

	public ProductSystemDescriptor system() {
		return system;
	}

	public void setSystem(ProductSystemDescriptor system) {
		this.system = system;
	}

	public LinkingStrategy strategy() {
		return strategy;
	}

	public void setStrategy(LinkingStrategy strategy) {
		this.strategy = strategy;
	}

	boolean isComplete() {
		return source != null
			&& target != null
			&& system != null
			&& strategy != null;
	}
}
