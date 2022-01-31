package org.openlca.app.tools.openepd.model;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.openlca.util.Strings;

public class Ec3Client {

	private final String authUrl;
	private final String authKey;
	private final String queryUrl;
	private final HttpClient http;

	private Ec3Client(Config config, String authKey) {
		this.authUrl = config.authUrl;
		this.queryUrl = config.queryUrl != null
			? config.queryUrl
			: config.authUrl;
		this.authKey = authKey;
		http = config.http;
	}

	public String authKey() {
		return authKey;
	}

	public static Config of(String authUrl) {
		var url = !authUrl.endsWith("/")
			? authUrl + "/"
			: authUrl;
		return new Config(url);
	}

	public Ec3Response get(String path) {
		var p = path.startsWith("/")
			? path.substring(1)
			: path;
		var req = HttpRequest.newBuilder()
			.uri(URI.create(queryUrl + p))
			.header("Accept", "application/json")
			.header("Authorization", "Bearer " + authKey)
			.GET()
			.build();
		try {
			var resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
			return Ec3Response.of(resp);
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException("GET " + path + " failed", e);
		}
	}

	public Ec3Response post(String path, JsonElement body) {
		var p = path.startsWith("/")
			? path.substring(1)
			: path;
		try {
			var bodyStr = HttpRequest.BodyPublishers.ofString(
				new Gson().toJson(body), StandardCharsets.UTF_8);
			var req = HttpRequest.newBuilder()
				.uri(URI.create(authUrl + p))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + authKey)
				.header("Accept", "application/json")
				.POST(bodyStr)
				.build();
			var resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
			return Ec3Response.of(resp);
		} catch (Exception e) {
			throw new RuntimeException("Failed to post to EC3", e);
		}
	}

	public boolean logout() {
		var req = HttpRequest.newBuilder()
			.uri(URI.create(authUrl + "rest-auth/logout"))
			.header("Authorization", "Bearer " + authKey)
			.POST(HttpRequest.BodyPublishers.noBody())
			.build();
		try {
			var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
			return resp.statusCode() == 200;
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException("POST rest-auth/logout failed", e);
		}
	}

	public static class Config {

		private final String authUrl;
		private final HttpClient http;
		private String queryUrl;

		private Config(String authUrl) {
			this.authUrl = Objects.requireNonNull(authUrl);
			this.http = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_2)
				.build();
		}

		public Config withQueryUrl(String queryUrl) {
			if (Strings.notEmpty(queryUrl)) {
				this.queryUrl = queryUrl;
			}
			return this;
		}

		public Ec3Client login(String user, String password) {
			var obj = new JsonObject();
			obj.addProperty("username", user);
			obj.addProperty("password", password);
			var json = new Gson().toJson(obj);

			var req = HttpRequest.newBuilder()
				.uri(URI.create(authUrl + "rest-auth/login"))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
				.build();

			try {
				var resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
				try (var stream = resp.body();
						 var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
					var jsonKey = new Gson().fromJson(reader, JsonObject.class);
					var authKey = jsonKey.get("key");
					if (authKey == null || !authKey.isJsonPrimitive())
						throw new RuntimeException("login failed");
					return new Ec3Client(this, authKey.getAsString());
				}
			} catch (InterruptedException | IOException e) {
				throw new RuntimeException("login failed", e);
			}
		}

		public Ec3Client session(String authKey) {
			return new Ec3Client(this, authKey);
		}

	}
}
