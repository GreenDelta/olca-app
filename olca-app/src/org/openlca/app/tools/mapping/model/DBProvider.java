package org.openlca.app.tools.mapping.model;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;

public class DBProvider implements IMapProvider {

	public final IDatabase db;

	public DBProvider(IDatabase db) {
		this.db = db;
	}

	@Override
	public void close() throws IOException {
		// we do not close the database here!
		// as the close method is intended for
		// closing external resources like files
	}

	@Override
	public List<FlowRef> getFlowRefs() {
		// TODO: not yet implemented
		return Collections.emptyList();
	}

	@Override
	public Optional<Flow> persist(FlowRef ref, IDatabase db) {
		if (ref == null || ref.flow == null || db == null)
			return Optional.empty();
		Flow flow = new FlowDao(db).getForRefId(ref.flow.refId);
		return Optional.ofNullable(flow);
	}
}
