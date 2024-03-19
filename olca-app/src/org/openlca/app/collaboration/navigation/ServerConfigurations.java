package org.openlca.app.collaboration.navigation;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
	private final static File FILE = new File(Workspace.root(), FILE_NAME);
	private static List<ServerConfig> CONFIGS = read();

	public static List<ServerConfig> get() {
		return CONFIGS;
	}

	public static void put(ServerConfig config) {
		var index = CONFIGS.indexOf(config);
		if (index == -1) {
			CONFIGS.add(config);
		} else {
			CONFIGS.set(index, config);
		}
		write();
	}

	public static void remove(ServerConfig config) {
		CONFIGS.remove(config);
		write();
	}

	public static void update(ServerConfig config) {

	}

	private static List<ServerConfig> read() {
		if (!FILE.exists())
			return new ArrayList<>();
		try (var reader = new FileReader(FILE)) {
			return new Gson().fromJson(reader, new TypeToken<List<ServerConfig>>() {
			});
		} catch (IOException e) {
			log.error("Error reading " + FILE_NAME, e);
			return new ArrayList<>();
		}
	}

	private static void write() {
		try (var writer = new FileWriter(FILE)) {
			new Gson().toJson(CONFIGS, writer);
		} catch (IOException e) {
			log.error("Error writing " + FILE_NAME, e);
		}
	}

	public record ServerConfig(String url, String user) {

		public ServerConfig(String url) {
			this(url, null);
		}
		
		public CollaborationServer open() {
			return new CollaborationServer(url,
					() -> AuthenticationDialog.promptCredentials(url, user));
		}

	}

}
