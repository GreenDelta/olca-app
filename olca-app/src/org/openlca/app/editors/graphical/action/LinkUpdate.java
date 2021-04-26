package org.openlca.app.editors.graphical.action;

import java.util.Objects;

import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;

public class LinkUpdate {

	private final IDatabase db;
	private final ProductSystem system;
	private final ProviderLinking providerLinking;
	private final ProcessType preferredType;
	private final boolean keepExisting;
	private final boolean preferSameLocations;

	private LinkUpdate(Config config) {
		this.db = config.db;
		this.system = config.system;
		this.providerLinking = config.providerLinking;
		this.preferredType = config.preferredType;
		this.keepExisting = config.keepExisting;
		this.preferSameLocations = config.preferSameLocations;
	}

	public static Config of(IDatabase db, ProductSystem system) {
		return new Config(db, system);
	}

	public static class Config {

		private final IDatabase db;
		private final ProductSystem system;
		private ProviderLinking providerLinking = ProviderLinking.PREFER_DEFAULTS;
		private ProcessType preferredType = ProcessType.LCI_RESULT;
		private boolean keepExisting;
		private boolean preferSameLocations;

		private Config(IDatabase db, ProductSystem system) {
			this.db = Objects.requireNonNull(db);
			this.system = Objects.requireNonNull(system);
		}

		public Config withProviderLinking(ProviderLinking providerLinking) {
			if (providerLinking != null) {
				this.providerLinking = providerLinking;
			}
			return this;
		}

		public Config withPreferredType(ProcessType type) {
			if (type != null) {
				this.preferredType = type;
			}
			return this;
		}

		public Config keepExistingLinks(boolean b) {
			this.keepExisting = b;
			return this;
		}

		public Config preferLinksInSameLocation(boolean b) {
			this.preferSameLocations = b;
			return this;
		}
	}

}
