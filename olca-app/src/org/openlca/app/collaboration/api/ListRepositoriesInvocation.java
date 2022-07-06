package org.openlca.app.collaboration.api;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.collaboration.util.WebRequests.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ListRepositoriesInvocation extends Invocation<JsonObject, List<String>> {

	ListRepositoriesInvocation() {
		super(Type.GET, "repository?page=0", JsonObject.class);
	}

	@Override
	protected List<String> process(JsonObject response) {
		JsonArray data = response.get("data").getAsJsonArray();
		List<String> repositories = new ArrayList<>();
		for (JsonElement e : data) {
			JsonObject repository = e.getAsJsonObject();
			String group = repository.get("group").getAsString();
			String name = repository.get("name").getAsString();
			repositories.add(group + "/" + name);
		}
		return repositories;
	}

}
