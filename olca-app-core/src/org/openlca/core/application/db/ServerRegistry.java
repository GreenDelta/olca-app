package org.openlca.core.application.db;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.openlca.core.database.IDatabaseServer;
import org.openlca.core.database.MySQLServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A registry of the stored database connections. */
public class ServerRegistry {

	private static ServerRegistry instance;
	private List<? extends IDatabaseServer> servers;
	private ServerFile serverFile;
	private Logger log = LoggerFactory.getLogger(getClass());

	private ServerRegistry() {
		String workspace = Platform.getInstanceLocation().getURL().getFile();
		String filePath = workspace + "/provider.xml";
		log.trace("read provider file @ {}", filePath);
		serverFile = new ServerFile(new File(filePath));
		servers = serverFile.read(MySQLServer.class);
	}

	public static ServerRegistry getInstance() {
		if (instance == null)
			instance = new ServerRegistry();
		return instance;
	}

	public List<IDatabaseServer> getServers() {
		return new ArrayList<>(servers);
	}

	public void removeServer(IDatabaseServer server) {
		servers.remove(server);
		serverFile.write(servers);
	}

	public void addServer(IDatabaseServer server) {
		List<IDatabaseServer> newList = new ArrayList<>();
		newList.add(server);
		for (IDatabaseServer p : servers)
			newList.add(p);
		servers = newList;
		serverFile.write(servers);
	}

}
