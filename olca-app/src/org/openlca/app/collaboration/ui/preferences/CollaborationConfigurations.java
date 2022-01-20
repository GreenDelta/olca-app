package org.openlca.app.collaboration.ui.preferences;

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

public final class CollaborationConfigurations {

	private static final Logger log = LoggerFactory.getLogger(CollaborationConfigurations.class);

	public static List<CollaborationConfiguration> get() {
		var file = getFile();
		if (!file.exists())
			return new ArrayList<>();
		try {
			var data = new String(Files.readAllBytes(file.toPath()));
			return new Gson().fromJson(data, new TypeToken<List<CollaborationConfiguration>>() {
			}.getType());
		} catch (IOException e) {
			log.error("Error loading collaboration configurations", e);
			return new ArrayList<>();
		}
	}

	public static CollaborationConfiguration getDefault() {
		for (var config : get())
			if (config.isDefault())
				return config;
		return null;
	}

	static void save(List<CollaborationConfiguration> list) {
		var file = getFile();
		if (file.exists())
			file.delete();
		var data = new Gson().toJson(list);
		try {
			Files.write(file.toPath(), data.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		} catch (IOException e) {
			log.error("Error saving collaboration configurations", e);
		}
	}

	private static File getFile() {
		var workspace = App.getWorkspace();
		return new File(workspace, "collaboration-server.json");
	}

}
