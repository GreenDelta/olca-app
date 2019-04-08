package org.openlca.app.cloud.ui.compare.json;

import java.util.Set;

import com.google.gson.JsonElement;

public interface IDependencyResolver {

	public Set<String> resolve(JsonElement parent, String property);

}