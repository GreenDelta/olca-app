package org.openlca.app.tools.mapping.replacer;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.io.maps.FlowMap;

/**
 * Configuration of the flow replacement when a flow mapping is applied.
 */
public class ReplacerConfig {

	public final FlowMap mapping;

	/**
	 * The provider of the target flows of the mapping. In a replacement, the source
	 * flows (that should be replaced by the target flows) are always flows in the
	 * currently active databases. Of course, the target flow provider could be also
	 * the same database.
	 *
	 */
	public final IProvider provider;

	public final List<RootDescriptor> models = new ArrayList<>();
	public boolean deleteMapped;

	public ReplacerConfig(FlowMap mapping, IProvider provider) {
		this.mapping = mapping;
		this.provider = provider;
	}

}
