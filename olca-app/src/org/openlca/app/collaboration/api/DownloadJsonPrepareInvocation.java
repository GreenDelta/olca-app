package org.openlca.app.collaboration.api;

import java.util.Collection;

import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.git.util.TypedRefId;

class DownloadJsonPrepareInvocation extends Invocation<String, String> {

	private final String repositoryId;
	private final Collection<? extends TypedRefId> requestData;

	DownloadJsonPrepareInvocation(String repositoryId, Collection<? extends TypedRefId> requestData) {
		super(Type.PUT, "public/download/json/prepare", String.class);
		this.repositoryId = repositoryId;
		this.requestData = requestData;
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
	protected Object data() {
		return requestData;
	}

}
