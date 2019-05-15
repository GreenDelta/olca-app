package org.openlca.app.tools.mapping.model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.ilcd.io.ZipStore;
import org.openlca.io.ilcd.input.FlowImport;
import org.openlca.io.ilcd.input.ImportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ILCDProvider implements IMapProvider {

	public final ZipStore store;

	public ILCDProvider(File file) {
		try {
			store = new ZipStore(file);
		} catch (Exception e) {
			throw new RuntimeException("Could not open zip", e);
		}
	}

	@Override
	public List<FlowRef> getFlowRefs() {
		return null;
	}

	@Override
	public Optional<Flow> persist(FlowRef ref, IDatabase db) {
		if (ref == null || ref.flow == null || db == null)
			return Optional.empty();
		FlowDao dao = new FlowDao(db);
		Flow flow = dao.getForRefId(ref.flow.refId);
		if (flow != null)
			return Optional.of(flow);
		try {
			ImportConfig conf = new ImportConfig(store, db);
			FlowImport imp = new FlowImport(conf);
			flow = imp.run(ref.flow.refId);
			return Optional.ofNullable(flow);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to import flow " + ref.flow, e);
			return Optional.empty();
		}
	}

	@Override
	public void close() throws IOException {
		try {
			store.close();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to close ILCD zip store", e);
		}
	}
}
