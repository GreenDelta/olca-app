package org.openlca.app.tools.mapping.model;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.descriptors.BaseDescriptor;

public class FlowMap extends BaseDescriptor {

	/** Description of the source system. */
	public BaseDescriptor source;

	/** Description of the target system. */
	public BaseDescriptor target;

	public final List<FlowMapEntry> entries = new ArrayList<>();

}
