package org.openlca.app.collaboration.api;

import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.core.model.ModelType;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Invokes a web service call to load a data set matching by type, refId and
 * commitId
 */
class DatasetContentInvocation {

	private static final String PATH = "/public/fetch/data/";
	String baseUrl;
	String sessionId;
	String repositoryId;
	String commitId;
	ModelType type;
	String refId;

	/**
	 * Retrieves a data set matching the specified type, refId for the given
	 * commit id.
	 * 
	 * @return The requested openLCA entity as JsonObject
	 * @throws WebRequestException
	 *             If user is out of sync or has no access to the specified
	 *             repository
	 */
	JsonObject execute() throws WebRequestException {
		Valid.checkNotEmpty(baseUrl, "base url");
		Valid.checkNotEmpty(repositoryId, "repository id");
		Valid.checkNotEmpty(type, "model type");
		Valid.checkNotEmpty(refId, "reference id");
		var url = baseUrl + PATH + repositoryId + "/" + type + "/" + refId;
		if (commitId != null) {
			url += "?commitId=" + commitId;
		}
		var response = WebRequests.call(Type.GET, url, sessionId);
		var json = response.getEntity(String.class);
		if (Strings.isNullOrEmpty(json))
			return null;
		var element = new Gson().fromJson(json, JsonElement.class);
		return element.getAsJsonObject();
	}

}
