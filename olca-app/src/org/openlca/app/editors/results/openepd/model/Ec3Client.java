package org.openlca.app.editors.results.openepd.model;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Ec3Client {

	private final String endpoint;
	private final String authKey;
	private final HttpClient http;

	private Ec3Client(Config config, String authKey) {
		this.endpoint = config.endpoint;
		this.authKey = authKey;
		http = config.http;
	}

	public String authKey() {
		return authKey;
	}

	public static Config of(String endpoint) {
		var url = !endpoint.endsWith("/")
			? endpoint + "/"
			: endpoint;
		return new Config(url);
	}

	public JsonObject getEpd(String id) {
		return get("epds/" + id, JsonObject.class);
	}

	public <T> T get(String path, Class<T> type) {
		var req = HttpRequest.newBuilder()
			.uri(URI.create(endpoint + path))
			.header("Accept", "application/json")
			.header("Authorization", "Bearer " + authKey)
			.GET()
			.build();
		try {
			var resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
			try (var stream = resp.body();
					 var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				return new Gson().fromJson(reader, type);
			}
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException("GET " + path + " failed", e);
		}
	}

	public <T> T post(String path, JsonElement body, Class<T> responseType) {
		try {
			var bodyStr = HttpRequest.BodyPublishers.ofString(
				new Gson().toJson(body), StandardCharsets.UTF_8);
			var req = HttpRequest.newBuilder()
				.uri(URI.create(endpoint + path))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + authKey)
				.header("Accept", "application/json")
				.POST(bodyStr)
				.build();
			var resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
			try (var stream = resp.body();
					 var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				return new Gson().fromJson(reader, responseType);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to post to EC3", e);
		}
	}

	public boolean logout() {
		var req = HttpRequest.newBuilder()
			.uri(URI.create(endpoint + "rest-auth/logout"))
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

		private final String endpoint;
		private final HttpClient http;

		private Config(String endpoint) {
			this.endpoint = endpoint;
			this.http = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_2)
				.build();
		}

		public Ec3Client login(String user, String password) {
			var obj = new JsonObject();
			obj.addProperty("username", user);
			obj.addProperty("password", password);
			var json = new Gson().toJson(obj);

			var req = HttpRequest.newBuilder()
				.uri(URI.create(endpoint + "rest-auth/login"))
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
