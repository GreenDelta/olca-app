package org.openlca.app.tools.mapping.replacer;

import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.io.maps.FlowMap;

/**
 * Configuration of the flow replacement when a flow mapping is applied.
 */
public class ReplacerConfig {

	public final FlowMap mapping;
	public final IProvider provider;

	public boolean processes;
	public boolean methods;
	public boolean deleteMapped;

	public ReplacerConfig(FlowMap mapping, IProvider provider) {
		this.mapping = mapping;
		this.provider = provider;
		this.processes = true;
	}

}
