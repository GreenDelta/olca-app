package org.openlca.app.tools.openepd.model;

import java.io.File;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.JsonObject;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.ErrorReporter;
import org.openlca.jsonld.Json;

public class Ec3Credentials {

	private String ec3Url;
	private String epdUrl;
	private String user;
	private String password;

	public String ec3Url() {
		return ec3Url;
	}

	public Ec3Credentials ec3Url(String url) {
		this.ec3Url = url;
		return this;
	}

	public String epdUrl() {
		return epdUrl;
	}

	public Ec3Credentials epdUrl(String url) {
		this.epdUrl = url;
		return this;
	}

	public String user() {
		return user;
	}

	public Ec3Credentials user(String user) {
		this.user = user;
		return this;
	}

	public String password() {
		return password;
	}

	public Ec3Credentials password(String password) {
		this.password = password;
		return this;
	}

	public static Ec3Credentials getDefault() {
		var c = new Ec3Credentials();
		c.ec3Url = "https://buildingtransparency.org/api";
		c.epdUrl = "https://openepd.buildingtransparency.org/api";
		var file = file();
		if (!file.exists())
			return c;
		try {
			var json = Json.readObject(file).orElse(null);
			if (json == null)
				return c;
			c.ec3Url = Objects.requireNonNullElse(
					Json.getString(json, "ec3Url"), c.ec3Url);
			c.epdUrl = Objects.requireNonNullElse(
					Json.getString(json, "epdUrl"), c.epdUrl);
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
		json.addProperty("ec3Url", ec3Url);
		json.addProperty("epdUrl", epdUrl);
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
			var client = Ec3Client.of(ec3Url)
				.withEpdUrl(epdUrl)
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
