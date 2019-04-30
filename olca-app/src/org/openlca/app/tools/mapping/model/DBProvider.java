package org.openlca.app.tools.mapping.model;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.openlca.core.database.IDatabase;

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
}
