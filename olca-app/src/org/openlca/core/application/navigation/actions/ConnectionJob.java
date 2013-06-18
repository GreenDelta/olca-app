package org.openlca.core.application.navigation.actions;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.application.db.DatabaseUpdateDialog;
import org.openlca.core.application.db.IDatabaseConfiguration;
import org.openlca.core.database.DatabaseDescriptor;
import org.openlca.core.database.mysql.ServerApp;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Connects to a database. */
class ConnectionJob implements Runnable {

	private Logger log = LoggerFactory.getLogger(getClass());

	public ConnectionJob(IDatabaseConfiguration config) {
		this.server = server;
	}

	@Override
	public void run() {
		try {
			if (server.isEmbedded()) {
				ServerApp app = startEmbedded();
				server.setServerApp(app);
			}
			this.server.connect();
			server.getDatabaseDescriptors();
			List<DatabaseDescriptor> descriptors = server
					.getDatabaseDescriptors();
			log.trace("{} databases found", descriptors.size());
			List<DatabaseDescriptor> forUpdates = new ArrayList<>();
			for (DatabaseDescriptor d : descriptors)
				if (!d.isUpToDate())
					forUpdates.add(d);
			update(forUpdates);
			connect(descriptors);
		} catch (Exception e) {
			log.error("Error in server connection", e);
		}
	}

	private void update(List<DatabaseDescriptor> descriptors) {
		if (descriptors.isEmpty())
			return;
		log.trace("Update for {} databases is required", descriptors.size());
		new DatabaseUpdateDialog(UI.shell(), null, descriptors).open();
	}

}