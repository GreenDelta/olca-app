package org.openlca.app.components;

import org.openlca.core.model.descriptors.BaseDescriptor;

/**
 * An interface for handling drop events of single base descriptors.
 */
public interface ISingleModelDrop {

	void handle(BaseDescriptor descriptor);

}
