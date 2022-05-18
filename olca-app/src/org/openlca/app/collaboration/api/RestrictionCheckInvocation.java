package org.openlca.app.collaboration.api;

import java.util.Collections;
import java.util.List;

import org.openlca.app.collaboration.model.Restriction;
import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.git.model.ModelRef;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

/**
 * Invokes a web service call to check if the given ref ids are restricted
 */
class RestrictionCheckInvocation {

	private static final String PATH = "/restrictions";
	String baseUrl;
	String sessionId;
	String repositoryId;
	List<? extends ModelRef> refs;

	/**
	 * Retrieves the restrictions for the given ref ids
	 * 
	 * @return A mapping from ref id to restriction name for those ref ids that are
	 *         contained in a restriction (set)
	 * @throws WebRequestException
	 */
	List<Restriction> execute() throws WebRequestException {
		Valid.checkNotEmpty(baseUrl, "base url");
		Valid.checkNotEmpty(refs, "refs");
		var url = baseUrl + PATH + "?group=" + repositoryId.split("/")[0] + "&name=" + repositoryId.split("/")[1];
		var response = WebRequests.call(Type.POST, url, sessionId, refs.stream().map(d -> d.refId).toList());
		if (response.getStatus() == Status.NO_CONTENT.getStatusCode())
			return Collections.emptyList();
		return mapResults(response);
	}

	private List<Restriction> mapResults(ClientResponse response) {
		var entity = response.getEntity(String.class).trim();
		List<Restriction> list = new Gson().fromJson(entity, new TypeToken<List<Restriction>>() {
		}.getType());
		list.forEach(r -> {
			var ref = refs.stream()
					.filter(d -> d.refId.equals(r.datasetRefId))
					.findFirst().get();
			r.modelType = ref.type;
			r.path = ref.path;
		});
		return list;
	}

}
