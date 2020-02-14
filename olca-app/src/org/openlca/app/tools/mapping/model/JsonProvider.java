package org.openlca.app.tools.mapping.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ModelType;
import org.openlca.io.maps.FlowMap;
import org.openlca.io.maps.FlowRef;
import org.openlca.io.maps.Status;
import org.openlca.jsonld.ZipStore;
import org.openlca.jsonld.input.JsonImport;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class JsonProvider implements IProvider {

	public final File file;

	private JsonProvider(File file) {
		this.file = file;
	}

	public static JsonProvider of(String path) {
		return new JsonProvider(new File(path));
	}

	public static JsonProvider of(File file) {
		return new JsonProvider(file);
	}

	public List<FlowMap> getFlowMaps() {
		try (ZipStore store = ZipStore.open(file)) {
			List<String> files = store.getFiles("flow_mappings");
			List<FlowMap> maps = new ArrayList<>();
			for (String f : files) {
				byte[] data = store.get(f);
				String json = new String(data, "utf-8");
				JsonObject obj = new Gson().fromJson(json, JsonObject.class);
				FlowMap map = FlowMap.fromJson(obj);
				maps.add(map);
			}
			return maps;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to read mapping files", e);
			return Collections.emptyList();
		}
	}

	@Override
	public List<FlowRef> getFlowRefs() {
		return new JsonRefCollector(file).collect();
	}

	@Override
	public void persist(List<FlowRef> refs, IDatabase db) {
		if (refs == null || db == null)
			return;
		try (ZipStore store = ZipStore.open(file)) {
			FlowDao dao = new FlowDao(db);
			JsonImport imp = new JsonImport(store, db);
			for (FlowRef ref : refs) {
				Flow flow = dao.getForRefId(ref.flow.refId);
				if (flow != null)
					continue;
				imp.run(ModelType.FLOW, ref.flow.refId);
			}
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed persist flows", e);
		}
	}

	@Override
	public void sync(Stream<FlowRef> externalRefs) {
		if (externalRefs == null)
			return;
		Map<String, FlowRef> packRefs = getFlowRefs().stream().collect(
				Collectors.toMap(ref -> ref.flow.refId, ref -> ref));
		externalRefs.forEach(ref -> {
			if (Sync.isInvalidFlowRef(ref))
				return;

			// we update the status in the following sync. steps
			ref.status = null;

			// check the flow
			FlowRef packRef = packRefs.get(ref.flow.refId);
			if (packRef == null) {
				ref.status = Status.error("there is no flow with id="
						+ ref.flow.refId + " in the data package");
				return;
			}

			// check the flow property
			if (ref.property == null) {
				ref.property = packRef.property;
			} else if (packRef.property == null ||
					!Strings.nullOrEqual(
							packRef.property.refId, ref.property.refId)) {
				ref.status = Status.error("the flow in the data package has"
						+ " a different flow property");
				return;
			}

			// check the unit
			if (ref.unit == null) {
				ref.unit = packRef.unit;
			} else if (packRef.unit != null) {
				// TODO: check units
			}

			Sync.checkFlowName(ref, packRef.flow.name);
			Sync.checkFlowCategory(ref, packRef.flowCategory);
			Sync.checkFlowType(ref, packRef.flow.flowType);
			Sync.checkFlowLocation(ref, packRef.flowLocation);

			if (ref.status == null) {
				ref.status = Status.ok("flow in sync with data package");
			}

		});
	}

}
