package org.openlca.app.collaboration.api;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.collaboration.util.WebRequests.Type;

import com.google.gson.JsonObject;

class ListRepositoriesInvocation extends Invocation<JsonObject, List<String>> {

	ListRepositoriesInvocation() {
		super(Type.GET, "repository?page=0", JsonObject.class);
	}

	@Override
	protected List<String> process(JsonObject result) {
		var data = result.get("data").getAsJsonArray();
		var repositories = new ArrayList<String>();
		data.forEach(e -> {
			var repository = e.getAsJsonObject();
			var group = repository.get("group").getAsString();
			var name = repository.get("name").getAsString();
			repositories.add(group + "/" + name);
		});
		return repositories;
	}

}
