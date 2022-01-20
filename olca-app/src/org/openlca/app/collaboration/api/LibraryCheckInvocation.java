package org.openlca.app.collaboration.api;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.collaboration.model.LibraryRestriction;
import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.git.model.Reference;

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
	Collection<Reference> refs;

	/**
	 * Retrieves the libraries for the given ref ids
	 * 
	 * @return A mapping from ref id to library name for those ref ids that are
	 *         contained in a library
	 * @throws WebRequestException
	 */
	List<LibraryRestriction> execute() throws WebRequestException {
		Valid.checkNotEmpty(baseUrl, "base url");
		Valid.checkNotEmpty(refs, "refs");
		var url = baseUrl + PATH + "?group=" + repositoryId.split("/")[0] + "&name=" + repositoryId.split("/")[1];
		var refIds = refs.stream().map(r -> r.refId).toList();
		var response = WebRequests.call(Type.POST, url, sessionId, refIds);
		if (response.getStatus() == Status.NO_CONTENT.getStatusCode())
			return Collections.emptyList();
		return mapResults(response);
	}

	private List<LibraryRestriction> mapResults(ClientResponse response) {
		var entity = response.getEntity(String.class).trim();
		var refMap = refs.stream().collect(Collectors.toMap(r -> r.refId, r -> r));
		List<LibraryRestriction> list = new Gson().fromJson(entity, new TypeToken<List<LibraryRestriction>>() {
		}.getType());
		list.forEach(r -> r.ref = refMap.get(r.datasetRefId));
		return list;
	}

}
