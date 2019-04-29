package org.openlca.app.tools.mapping.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.jsonld.ZipStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class JsonProvider implements IMapProvider {

	public final ZipStore store;

	public JsonProvider(File file) {
		try {
			store = ZipStore.open(file);
		} catch (Exception e) {
			throw new RuntimeException("Could not open zip", e);
		}
	}

	@Override
	public List<FlowMap> getFlowMaps() {
		try {
			List<String> files = store.getFiles("flow_mappings");
			List<FlowMap> maps = new ArrayList<>();
			for (String f : files) {
				byte[] data = store.get(f);
				String json = new String(data, "utf-8");
				JsonObject obj = new Gson().fromJson(json, JsonObject.class);
				FlowMap map = FlowMap.from(obj);
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
		return null;
	}

	@Override
	public void close() {
		try {
			store.close();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to close JSON-LD zip store", e);
		}
	}
}
