package org.openlca.app.collaboration.api;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.collaboration.model.SearchResult;
import org.openlca.app.collaboration.model.SearchResult.Dataset;
import org.openlca.app.collaboration.util.Json;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.core.model.ModelType;
import org.openlca.util.Strings;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class SearchInvocation extends Invocation<JsonObject, SearchResult> {

	private final String repositoryId;
	private final String query;
	private final ModelType type;
	private final int page;
	private final int pageSize;

	SearchInvocation(String repositoryId, String query, ModelType type, int page, int pageSize) {
		super(Type.GET, "public/search", JsonObject.class);
		this.repositoryId = repositoryId;
		this.query = query;
		this.type = type;
		this.page = page;
		this.pageSize = pageSize;
	}

	@Override
	protected String query() {
		var query = "?page=" + page
				+ "&pageSize=" + pageSize
				+ "&repositoryId=" + repositoryId;
		if (!Strings.nullOrEmpty(this.query)) {
			query += "&query=" + WebRequests.encodeQuery(this.query);
		}
		if (type != null) {
			query += "&type=" + type.name();
		}
		return query;
	}

	@Override
	protected SearchResult process(JsonObject response) {
		var data = parseDatasets(Json.toJsonArray(response.get("data")));
		var resultInfo = Json.toJsonObject(response.get("resultInfo"));
		return new SearchResult(data,
				Json.getInt(resultInfo, "currentPage", 0),
				Json.getInt(resultInfo, "pageSize", 10),
				Json.getInt(resultInfo, "totalCount", 0));
	}

	private List<Dataset> parseDatasets(JsonArray data) {
		var datasets = new ArrayList<Dataset>();
		for (var entry : data) {
			var versions = Json.toJsonArray(Json.getValue(entry, "versions"));
			if (versions == null)
				continue;
			for (var version : versions) {
				var repos = Json.toJsonArray(Json.getValue(version, "repos"));
				if (repos == null)
					continue;
				for (var repo : repos) {
					datasets.add(new Dataset(
							ModelType.valueOf(Json.getString(entry, "type")),
							Json.getString(entry, "refId"),
							Json.getString(version, "name"),
							Json.getString(version, "category"),
							Json.getString(repo, "path"),
							Json.getString(repo, "commitId")));
				}
			}
		}
		return datasets;
	}

}
