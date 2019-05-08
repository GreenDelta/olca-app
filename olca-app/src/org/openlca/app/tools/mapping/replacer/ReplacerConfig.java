package org.openlca.app.tools.mapping.replacer;

import org.openlca.app.tools.mapping.model.FlowMap;
import org.openlca.app.tools.mapping.model.IMapProvider;

/**
 * Configuration of the flow replacement when a flow mapping is applied.
 */
public class ReplacerConfig {

	public final FlowMap mapping;
	public final IMapProvider provider;

	public boolean processes;
	public boolean methods;
	public boolean deleteMapped;

	public ReplacerConfig(FlowMap mapping, IMapProvider provider) {
		this.mapping = mapping;
		this.provider = provider;
		this.processes = true;
	}

}
