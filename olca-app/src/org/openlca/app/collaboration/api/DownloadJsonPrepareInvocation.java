package org.openlca.app.collaboration.api;

import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.git.util.TypedRefId;

class DownloadJsonPrepareInvocation extends Invocation<String, String> {

	private final String repositoryId;
	private final TypedRefId id;

	DownloadJsonPrepareInvocation(String repositoryId, TypedRefId id) {
		super(Type.GET, "public/download/json/prepare", String.class);
		this.repositoryId = repositoryId;
		this.id = id;
	}

	@Override
	protected void checkValidity() {
		Valid.checkNotEmpty(repositoryId, "repository id");
		Valid.checkNotEmpty(id, "id");
	}

	@Override
	protected String query() {
		var base = "/" + repositoryId + "/";
		if (id == null || id.type == null)
			return base;
		return base + id.type.name() + "/" + id.refId;
	}
}
