package org.openlca.app.collaboration.navigation;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.MsgBox;
import org.openlca.collaboration.client.CSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
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
		if (!checkCS(config))
			return;
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
		var index = CONFIGS.indexOf(config);
		if (index == -1)
			return;
		CONFIGS.set(index, config);
		write();
	}

	private static boolean checkCS(ServerConfig config) {
		if (WebRequests.execute(() -> CSClient.isCollaborationServer(config.url), false))
			return true;
		MsgBox.warning(M.NotACollaborationServer);
		return false;
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

		public CSClient createClient() {
			return new CSClient(url,
					() -> AuthenticationDialog.promptCredentials(url, user));
		}

		@Override
		public boolean equals(Object object) {
			if (!(object instanceof ServerConfig config))
				return false;
			return Objects.equal(config.url(), url());
		}

		@Override
		public int hashCode() {
			if (url == null)
				return "".hashCode();
			return url.hashCode();
		}

	}

}
