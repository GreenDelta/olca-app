package org.openlca.app.collaboration.navigation;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.rcp.Workspace;
import org.openlca.collaboration.api.CollaborationServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ServerConfigurations {

	private final static Logger log = LoggerFactory.getLogger(ServerConfigurations.class);
	private final static String FILE_NAME = "servers.json";

	private static List<CollaborationServer> SERVERS;

	public static List<CollaborationServer> get() {
		if (SERVERS == null) {
			SERVERS = read();
		}
		return SERVERS;
	}

	private static List<CollaborationServer> read() {
		var file = new File(Workspace.root(), FILE_NAME);
		if (!file.exists())
			return new ArrayList<>();
		try {
			return new Gson().fromJson(new FileReader(file), new TypeToken<List<ServerConfig>>() {
			}).stream()
					.map(config -> new CollaborationServer(config.url,
							() -> AuthenticationDialog.promptCredentials(config.url)))
					.collect(Collectors.toList());

		} catch (IOException e) {
			log.error("Error reading " + FILE_NAME, e);
			return new ArrayList<>();
		}
	}

	private record ServerConfig(String url, String username) {
	}

}
