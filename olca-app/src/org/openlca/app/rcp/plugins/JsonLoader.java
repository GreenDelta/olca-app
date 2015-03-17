package org.openlca.app.rcp.plugins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.IOUtils;
import org.openlca.app.App;
import org.openlca.app.rcp.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JsonLoader {

	private final static Logger log = LoggerFactory
			.getLogger(PluginService.class);
	private final static String JSON_NAME = "plugins.json";

	public String getPluginsJson() {
		String json = getRemoteJson();
		if (json != null)
			return json;
		return getWorkspaceJson();
	}

	private String getRemoteJson() {
		try {
			URL jsonUrl = new URL(PluginService.BASE_URL + "/"
					+ App.getVersion() + "/" + JSON_NAME);
			StringBuilder json = new StringBuilder();
			for (String line : IOUtils.readLines(jsonUrl.openStream()))
				json.append(line);
			String pluginsJson = json.toString();
			updateWorkspaceFile(pluginsJson);
			return pluginsJson;
		} catch (IOException e) {
			log.info("Could not load remote json", e);
			return null;
		}
	}

	private void updateWorkspaceFile(String updatedJson) {
		try {
			Files.write(localJsonFile().toPath(), updatedJson.getBytes(),
					StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		} catch (IOException e) {
			log.warn("Could not update workspace plugins json", e);
		}
	}

	private String getWorkspaceJson() {
		try {
			File jsonFile = localJsonFile();
			if (!jsonFile.exists())
				Files.copy(streamBundledJson(), jsonFile.toPath());
			byte[] encoded = Files.readAllBytes(jsonFile.toPath());
			return new String(encoded, "utf-8");
		} catch (IOException e) {
			log.info("Could not load workspace json", e);
			return null;
		}
	}

	private File localJsonFile() {
		File workspace = Workspace.getDir();
		File jsonFile = new File(workspace, JSON_NAME);
		return jsonFile;
	}

	private InputStream streamBundledJson() {
		return getClass().getResourceAsStream(JSON_NAME);
	}

}
