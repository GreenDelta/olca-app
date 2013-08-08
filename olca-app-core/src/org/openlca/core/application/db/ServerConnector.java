package org.openlca.core.application.db;

import org.openlca.core.application.App;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabaseServer;
import org.openlca.core.database.MySQLServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for creating the initial connection to a MySQL server and its
 * databases. If the server is specified as embedded server it first tries to
 * start this server and brings up an update dialog if required.
 */
public class ServerConnector {

	private Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Connects to the database server and returns a list of database
	 * descriptors.
	 */
	public void connect(IDatabaseServer server) throws Exception {
		if (!(server instanceof MySQLServer)) {
			throw new Exception("Currently only MySQL is supported");
		}
		MySQLServer myserver = (MySQLServer) server;
		App.runInUI(Messages.ConnectDataProviderAction_Connecting,
				new ConnectionJob(myserver));
	}

	public void disconnect(final IDatabaseServer server) {
		App.run(Messages.ConnectDataProviderAction_Disconnecting,
				new Runnable() {
					@Override
					public void run() {
						try {
							server.shutdown();
						} catch (Exception e) {
							log.error("Shut down of database server failed", e);
						}
					}
				}, new NavigationCallback());
	}
}
