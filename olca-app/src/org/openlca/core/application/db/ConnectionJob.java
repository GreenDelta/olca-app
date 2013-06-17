package org.openlca.core.application.db;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.openlca.core.application.App;
import org.openlca.core.application.Messages;
import org.openlca.core.database.DatabaseDescriptor;
import org.openlca.core.database.IDatabaseServer;
import org.openlca.core.database.mysql.MySQLServer;
import org.openlca.core.database.mysql.ServerApp;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConnectionJob implements Runnable {

	private Logger log = LoggerFactory.getLogger(getClass());
	private MySQLServer server;

	public ConnectionJob(MySQLServer server) {
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

	private ServerApp startEmbedded() throws Exception {
		log.trace("Start embedded server");
		try {
			String prop = System.getProperty("eclipse.home.location");
			URL url = new URL(prop);
			String home = new File(url.getPath()).getAbsolutePath();
			File appDir = new File(home);
			File dataDir = new File(Platform.getInstanceLocation().getURL()
					.getFile(), "data");
			int port = Integer.parseInt(server.getProperties().get(
					IDatabaseServer.PORT));
			ServerApp app = new ServerApp(appDir, dataDir, port);
			app.start();
			return app;
		} catch (Exception e) {
			log.error("Could not start embedded MySQL server");
			throw e;
		}
	}

	private void update(List<DatabaseDescriptor> descriptors) {
		if (descriptors.isEmpty())
			return;
		log.trace("Update for {} databases is required", descriptors.size());
		new DatabaseUpdateDialog(UI.shell(), server, descriptors).open();
	}

	private void connect(List<DatabaseDescriptor> descriptors) {
		log.trace("Connect to {} databases ", descriptors.size());
		DatabaseConnectionJob job = new DatabaseConnectionJob(descriptors);
		App.run(Messages.ConnectDataProviderAction_Connecting, job,
				new NavigationCallback());
	}

	private class DatabaseConnectionJob implements Runnable {

		private List<DatabaseDescriptor> descriptors;

		public DatabaseConnectionJob(List<DatabaseDescriptor> descriptors) {
			this.descriptors = descriptors;
		}

		@Override
		public void run() {
			log.trace("Connect to databases");
			try {
				for (DatabaseDescriptor d : descriptors)
					if (d.isUpToDate()) {
						server.connect(d);
						log.trace("Connected to {}", d);
					}
			} catch (Exception e) {
				log.error("Failed to connect to database", e);
			}
		}
	}

}