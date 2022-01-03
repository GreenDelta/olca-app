package org.openlca.app.editors.results.openepd.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.openlca.jsonld.Json;
import org.openlca.util.Strings;

import com.google.gson.JsonElement;

public class Api {

	private Api() {
	}

	public static DescriptorRequest descriptors(Ec3Client client) {
		return new DescriptorRequest(client);
	}

	public static class DescriptorRequest {

		private final Ec3Client client;
		private int page = 1;
		private int pageSize = 50;
		private String query = null;

		private DescriptorRequest(Ec3Client client) {
			this.client = client;
		}

		public DescriptorRequest page(int page) {
			this.page = page;
			return this;
		}

		public DescriptorRequest pageSize(int pageSize) {
			this.pageSize = pageSize;
			return this;
		}

		public DescriptorRequest query(String query) {
			this.query = query;
			return this;
		}

		private String path() {
			var path = "/epds" +
				"?page_number=" + page +
				"&page_size=" + pageSize +
				"&fields=id,name,description,category,manufacturer,declared_unit";
			if (query != null) {
				var q = query.trim();
				if (Strings.notEmpty(q)) {
					path += "&q=" + URLEncoder.encode(q, StandardCharsets.UTF_8);
				}
			}
			return path;
		}

		public DescriptorResponse get() {
			return DescriptorResponse.get(this);
		}

	}

	public record DescriptorResponse(
		int page,
		int totalCount,
		int totalPages,
		List<Ec3Epd> descriptors) {

		private static DescriptorResponse get(DescriptorRequest req) {
			var r = req.client.get(req.path());
			List<Ec3Epd> descriptors = r.hasJson()
				? parse(r.json())
				: Collections.emptyList();
			return new DescriptorResponse(
				req.page, r.totalCount(), r.pageCount(), descriptors);
		}

		private static List<Ec3Epd> parse(JsonElement json) {
			if (json == null || !json.isJsonArray())
				return Collections.emptyList();
			return Json.stream(json.getAsJsonArray())
				.map(Ec3Epd::fromJson)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
		}
	}

}
