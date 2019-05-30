package org.openlca.app.tools.mapping.model;

import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.io.maps.FlowMap;
import org.openlca.io.maps.FlowRef;

public class DBProvider implements IProvider {

	public final IDatabase db;

	public DBProvider(IDatabase db) {
		this.db = db;
	}

	@Override
	public List<FlowRef> getFlowRefs() {
		// TODO: not yet implemented
		return null;
	}

	@Override
	public void syncSourceFlows(FlowMap fm) {
		// TODO: not yet implemented
	}

	@Override
	public void syncTargetFlows(FlowMap fm) {
		// TODO: not yet implemented
	}

	@Override
	public void persist(List<FlowRef> refs, IDatabase db) {
	}
}
