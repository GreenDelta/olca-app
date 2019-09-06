package org.openlca.app.tools.mapping.replacer;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.io.maps.FlowMap;

/**
 * Configuration of the flow replacement when a flow mapping is applied.
 */
public class ReplacerConfig {

	public final FlowMap mapping;
	public final IProvider provider;

	public final List<CategorizedDescriptor> models = new ArrayList<>();
	public boolean deleteMapped;

	public ReplacerConfig(FlowMap mapping, IProvider provider) {
		this.mapping = mapping;
		this.provider = provider;
	}

}
