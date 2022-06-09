package org.openlca.app.collaboration.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openlca.app.collaboration.model.Restriction;
import org.openlca.app.collaboration.util.Valid;
import org.openlca.app.collaboration.util.WebRequests.Type;
import org.openlca.git.model.ModelRef;

import com.google.gson.reflect.TypeToken;

/**
 * Invokes a web service call to check if the given ref ids are restricted
 */
class RestrictionCheckInvocation extends Invocation<List<Restriction>, List<Restriction>> {

	private final String repositoryId;
	private final Collection<? extends ModelRef> refs;

	RestrictionCheckInvocation(String repositoryId, Collection<? extends ModelRef> refs) {
		super(Type.POST, "restrictions", new TypeToken<List<Restriction>>() {
		});
		this.repositoryId = repositoryId;
		this.refs = refs;
	}

	@Override
	protected void checkValidity() {
		Valid.checkNotEmpty(repositoryId, "repository id");
		Valid.checkNotEmpty(refs, "refs");
	}

	@Override
	protected String query() {
		return "?group=" + repositoryId.split("/")[0] + "&name=" + repositoryId.split("/")[1];
	}

	@Override
	protected Object data() {
		return refs.stream().map(d -> d.refId).toList();
	}

	@Override
	protected List<Restriction> process(List<Restriction> restrictions) {
		if (restrictions == null)
			return new ArrayList<>();
		restrictions.forEach(r -> {
			var ref = refs.stream()
					.filter(d -> d.refId.equals(r.datasetRefId))
					.findFirst().get();
			r.modelType = ref.type;
			r.path = ref.path;
		});
		return restrictions;
	}

}
