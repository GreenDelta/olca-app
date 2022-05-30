package org.openlca.app.collaboration.api;

import java.util.Map;

import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.jsonld.SchemaVersion;

import com.google.common.base.Strings;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.ClientResponse.Status;

/**
 * Invokes a webservice call to check access to the specified repository
 */
class CheckAccessInvocation extends Invocation<Map<String, Integer>, Boolean> {

	private final String repositoryId;

	CheckAccessInvocation(String repositoryId) {
		super(Type.GET, "repository/meta", new TypeToken<Map<String, Integer>>() {
		}.getType());
		this.repositoryId = repositoryId;
	}

	@Override
	protected void checkValidity() {
		Valid.checkNotEmpty(repositoryId, "repository id");
	}

	@Override
	protected String query() {
		return "/" + repositoryId;
	}

	@Override
	protected Boolean process(Map<String, Integer> meta) {
		var currentVersion = SchemaVersion.current().value();
		var version = meta.get("schemaVersion");
		if (currentVersion != version)
			throw new RuntimeException(
					"Schema version " + version + " does not match current version " + currentVersion);
		return true;
	}

	@Override
	protected Boolean handleError(WebRequestException e) throws WebRequestException {
		var currentVersion = SchemaVersion.current().value();
		if (!Strings.isNullOrEmpty(e.getMessage())) {
			if (e.getErrorCode() == Status.FORBIDDEN.getStatusCode())
				return false;
			throw e; // repository does not exist or no access
		}
		// url does not exist -> old server
		throw new RuntimeException("Unknown schema version, does not match current version " + currentVersion);
	}

}
