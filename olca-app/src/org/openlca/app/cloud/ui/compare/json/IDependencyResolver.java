package org.openlca.app.cloud.ui.compare.json;

import com.google.gson.JsonElement;

public interface IDependencyResolver {

	public String resolve(JsonElement parent, String property);

}