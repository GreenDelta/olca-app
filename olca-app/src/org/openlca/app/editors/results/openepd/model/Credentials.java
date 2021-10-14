package org.openlca.app.editors.results.openepd.model;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.ErrorReporter;
import org.openlca.jsonld.Json;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class Credentials {

	private String url;
	private String user;
	private String password;

	public String url() {
		return url;
	}

	public Credentials url(String url) {
		this.url = url;
		return this;
	}

	public String user() {
		return user;
	}

	public Credentials user(String user) {
		this.user = user;
		return this;
	}

	public String password() {
		return password;
	}

	public Credentials password(String password) {
		this.password = password;
		return this;
	}

	public static Credentials getDefault() {
		var c = new Credentials();
		c.url = "https://etl-api.cqd.io/api";
		var file = file();
		if (!file.exists())
			return c;
		try {
			var json = Json.readObject(file).orElse(null);
			if (json == null)
				return c;
			c.url = Json.getString(json, "url");
			c.user = Json.getString(json, "user");
			c.password = Json.getString(json, "password");
			return c;
		} catch (Exception e) {
			ErrorReporter.on("failed to read EC3 credentials from " + file, e);
			return c;
		}
	}

	public void save() {
		var json = new JsonObject();
		json.addProperty("url", url);
		json.addProperty("user", user);
		json.addProperty("password", password);
		var file = file();
		try {
			Json.write(json, file);
		} catch (Exception e) {
			ErrorReporter.on("failed to write EC3 credentials to " + file, e);
		}
	}

	public Optional<Ec3Client> login() {
		try {
			var client = Ec3Client.of(url).login(user, password);
			return Optional.of(client);
		} catch (Exception e) {
			var log = LoggerFactory.getLogger(getClass());
			log.error("failed to connect to EC3; user: "
								+ user + "; password: " + password, e);
			return Optional.empty();
		}
	}

	private static File file() {
		var dir = Workspace.getDir();
		if (!dir.exists()) {
			try {
				Files.createDirectories(dir.toPath());
			} catch (Exception e) {
				ErrorReporter.on(
					"No write access in workspace; failed to create " + dir, e);
			}
		}
		return new File(dir, ".ec3");
	}
}
