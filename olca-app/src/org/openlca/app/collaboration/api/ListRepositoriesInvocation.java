package org.openlca.app.collaboration.api;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.jersey.api.client.ClientResponse.Status;

class ListRepositoriesInvocation {

	private static final String PATH = "/repository?page=0";
	String baseUrl;
	String sessionId;

	List<String> execute() throws WebRequestException {
		Valid.checkNotEmpty(baseUrl, "base url");
		var response = WebRequests.call(Type.GET, baseUrl + PATH, sessionId);
		if (response.getStatus() == Status.NO_CONTENT.getStatusCode())
			return null;
		var result = new Gson().fromJson(response.getEntity(String.class), JsonObject.class);
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
