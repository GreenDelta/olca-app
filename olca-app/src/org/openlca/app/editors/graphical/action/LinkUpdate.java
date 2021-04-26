package org.openlca.app.editors.graphical.action;

import org.openlca.core.database.IDatabase;
import org.openlca.core.matrix.linking.ProviderLinking;
import org.openlca.core.model.ProductSystem;

public class LinkUpdate {

	private final IDatabase db;
	private final ProductSystem system;

	private LinkUpdate(Config config) {
		this.db = config.db;
		this.system = config.system;

	}

	public static class Config {

		private final IDatabase db;
		private final ProductSystem system;
		private ProviderLinking providerLinking;

		private Config(IDatabase db, ProductSystem system) {
			this.db = db;
			this.system = system;
		}

		public Config withProviderLinking(ProviderLinking providerLinking) {
			this.providerLinking = providerLinking;
			return this;
		}

	}

}
