package org.openlca.app.collaboration.api;

import java.util.Collections;
import java.util.List;

import org.openlca.app.collaboration.model.LibraryRestriction;
import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.git.model.Diff;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

/**
 * Invokes a web service call to check if the given ref ids are contained in any
 * known library (e.g. openLCA reference data)
 */
class LibraryCheckInvocation {

	private static final String PATH = "/library";
	String baseUrl;
	String sessionId;
	String repositoryId;
	List<Diff> diffs;

	/**
	 * Retrieves the libraries for the given ref ids
	 * 
	 * @return A mapping from ref id to library name for those ref ids that are
	 *         contained in a library
	 * @throws WebRequestException
	 */
	List<LibraryRestriction> execute() throws WebRequestException {
		Valid.checkNotEmpty(baseUrl, "base url");
		Valid.checkNotEmpty(diffs, "changes");
		var url = baseUrl + PATH + "?group=" + repositoryId.split("/")[0] + "&name=" + repositoryId.split("/")[1];
		var response = WebRequests.call(Type.POST, url, sessionId, diffs.stream().map(d -> d.ref().refId).toList());
		if (response.getStatus() == Status.NO_CONTENT.getStatusCode())
			return Collections.emptyList();
		return mapResults(response);
	}

	private List<LibraryRestriction> mapResults(ClientResponse response) {
		var entity = response.getEntity(String.class).trim();
		List<LibraryRestriction> list = new Gson().fromJson(entity, new TypeToken<List<LibraryRestriction>>() {
		}.getType());
		list.forEach(r -> {
			var diff = diffs.stream()
					.filter(d -> d.ref().refId.equals(r.datasetRefId))
					.findFirst().get();
			var ref = diff.ref();
			r.modelType = ref.type;
			r.path = ref.path;
		});
		return list;
	}

}
