package org.openlca.app.tools.openepd.model;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

import com.google.gson.JsonObject;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.ErrorReporter;
import org.openlca.jsonld.Json;

public class Credentials {

	private String url;
	private String queryUrl;
	private String user;
	private String password;

	public String url() {
		return url;
	}

	public Credentials url(String url) {
		this.url = url;
		return this;
	}

	public String queryUrl() {
		return queryUrl;
	}

	public Credentials queryUrl(String url) {
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
		c.url = "https://buildingtransparency.org/api";
		c.queryUrl = "https://openepd.buildingtransparency.org/api";
		var file = file();
		if (!file.exists())
			return c;
		try {
			var json = Json.readObject(file).orElse(null);
			if (json == null)
				return c;
			c.url = Json.getString(json, "url");
			c.queryUrl = Json.getString(json, "queryUrl");
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
		json.addProperty("queryUrl", queryUrl);
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
			var client = Ec3Client.of(url)
				.withQueryUrl(queryUrl)
				.login(user, password);
			return Optional.of(client);
		} catch (Exception e) {
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
