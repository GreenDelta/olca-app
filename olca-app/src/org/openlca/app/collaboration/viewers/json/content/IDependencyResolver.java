package org.openlca.app.collaboration.viewers.json.content;

import java.util.Set;

public interface IDependencyResolver {

	public Set<String> resolve(JsonNode node);

}