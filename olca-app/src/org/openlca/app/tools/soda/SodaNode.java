package org.openlca.app.tools.soda;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.util.ErrorReporter;
import org.openlca.jsonld.Json;
import org.openlca.util.Strings;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

record SodaNode(
		String name,
		String url,
		boolean hasEpds
) {

	static List<SodaNode> getAllKnown() {
		var stream = SodaNode.class.getResourceAsStream("soda-nodes.json");
		if (stream == null)
			return Collections.emptyList();
		try (stream;
				 var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {

			var array = new Gson().fromJson(reader, JsonArray.class);
			var nodes = new ArrayList<SodaNode>(array.size());
			for (var e : array) {
				if (!e.isJsonObject())
					continue;
				var obj = e.getAsJsonObject();
				var node = new SodaNode(
						Json.getString(obj, "name"),
						Json.getString(obj, "url"),
						Json.getBool(obj, "hasEpds", false)
				);
				nodes.add(node);
			}

			nodes.sort((n1, n2) -> Strings.compare(n1.name, n2.name));
			return nodes;
		} catch (Exception e) {
			ErrorReporter.on("failed to parse default list of soda4LCA nodes", e);
			return Collections.emptyList();
		}
	}
}
