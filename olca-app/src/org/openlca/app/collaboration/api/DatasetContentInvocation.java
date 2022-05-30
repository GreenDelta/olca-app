package org.openlca.app.collaboration.api;

import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.core.model.ModelType;

import com.google.gson.JsonObject;

/**
 * Invokes a web service call to load a data set matching by type, refId and
 * commitId
 */
class DatasetContentInvocation extends Invocation<JsonObject, JsonObject> {

	private final String repositoryId;
	private final String commitId;
	private final ModelType type;
	private final String refId;

	DatasetContentInvocation(String repositoryId, ModelType type, String refId, String commitId) {
		super(Type.GET, "public/fetch/data", JsonObject.class);
		this.repositoryId = repositoryId;
		this.type = type;
		this.refId = refId;
		this.commitId = commitId;
	}

	@Override
	protected void checkValidity() {
		Valid.checkNotEmpty(repositoryId, "repository id");
		Valid.checkNotEmpty(type, "model type");
		Valid.checkNotEmpty(refId, "reference id");
	}

	@Override
	protected String query() {
		var query = "/" + repositoryId + "/" + type + "/" + refId;
		if (commitId != null) {
			query += "?commitId=" + commitId;
		}
		return query;
	}

}
