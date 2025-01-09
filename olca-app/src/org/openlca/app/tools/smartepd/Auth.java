package org.openlca.app.tools.smartepd;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

import org.openlca.io.smartepd.SmartEpdClient;
import org.openlca.jsonld.Json;
import org.openlca.util.Res;

import com.google.gson.JsonObject;

record Auth(String url, String apiKey) {

	Auth(String url, String apiKey) {
		this.url = Objects.requireNonNull(url);
		this.apiKey = Objects.requireNonNull(apiKey);
	}

	static Auth of(String url, String apiKey) {
		var base = url.strip();
		if (!base.endsWith("/")) {
			base += "/";
		}
		if (!base.endsWith("/api/")) {
			base += "api/";
		}
		return new Auth(base, apiKey.strip());
	}

	static Optional<Auth> readFrom(File file) {
		if (file == null || !file.exists())
			return Optional.empty();
		var obj = Json.readObject(file).orElse(null);
		if (obj == null)
			return Optional.empty();
		var url = Json.getString(obj, "url");
		var apiKey = Json.getString(obj, "apiKey");
		return url != null && apiKey != null
				? Optional.of(new Auth(url, apiKey))
				: Optional.empty();
	}

	void write(File file) {
		var obj = new JsonObject();
		obj.addProperty("url", url);
		obj.addProperty("apiKey", apiKey);
		Json.write(obj, file);
	}

	Res<SmartEpdClient> createClient() {
		var client = SmartEpdClient.of(url, apiKey);
		var res = client.getProjects();
		return res.hasError()
			? res.wrapError("failed to create API connection")
			: Res.of(client);
	}
}
