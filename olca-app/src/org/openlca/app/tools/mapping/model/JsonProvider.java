package org.openlca.app.tools.mapping.model;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ModelType;
import org.openlca.io.maps.FlowMap;
import org.openlca.io.maps.FlowRef;
import org.openlca.jsonld.ZipStore;
import org.openlca.jsonld.input.JsonImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class JsonProvider implements IProvider {

	private final File file;

	private JsonProvider(File file) {
		this.file = file;
	}

	public static JsonProvider of(String path) {
		return new JsonProvider(new File(path));
	}

	public static JsonProvider of(File file) {
		return new JsonProvider(file);
	}

	public File file() {
		return file;
	}

	public List<FlowMap> getFlowMaps() {
		try (ZipStore store = ZipStore.open(file)) {
			List<String> files = store.getFiles("flow_mappings");
			List<FlowMap> maps = new ArrayList<>();
			for (String f : files) {
				byte[] data = store.getBytes(f);
				String json = new String(data, StandardCharsets.UTF_8);
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
		Sync.packageSync(this, externalRefs);
	}

}
