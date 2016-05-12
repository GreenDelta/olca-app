package org.openlca.app.cloud.ui.preferences;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.openlca.app.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public final class CloudConfigurations {

	private static final Logger log = LoggerFactory.getLogger(CloudConfigurations.class);

	public static List<CloudConfiguration> get() {
		File file = getFile();
		if (!file.exists())
			return new ArrayList<>();
		try {
			String data = new String(Files.readAllBytes(file.toPath()));
			return new Gson().fromJson(data, new TypeToken<List<CloudConfiguration>>() {
			}.getType());
		} catch (IOException e) {
			log.error("Error loading cloud configurations", e);
			return new ArrayList<>();
		}
	}

	public static CloudConfiguration getDefault() {
		for (CloudConfiguration config : get())
			if (config.isDefault())
				return config;
		return null;
	}

	static void save(List<CloudConfiguration> list) {
		File file = getFile();
		if (file.exists())
			file.delete();
		String data = new Gson().toJson(list);
		try {
			Files.write(file.toPath(), data.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		} catch (IOException e) {
			log.error("Error saving cloud configurations", e);
		}
	}

	private static File getFile() {
		File workspace = App.getWorkspace();
		return new File(workspace, "cloud-server.json");
	}

}
