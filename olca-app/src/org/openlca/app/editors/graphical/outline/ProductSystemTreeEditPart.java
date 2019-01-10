package org.openlca.app.editors.graphical.outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.gef.editparts.AbstractTreeEditPart;
import org.openlca.app.db.Cache;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProductSystemTreeEditPart extends AbstractTreeEditPart {

	@Override
	public ProductSystem getModel() {
		return (ProductSystem) super.getModel();
	}

	@Override
	protected List<ProcessDescriptor> getModelChildren() {
		Set<Long> ids = getModel().processes;
		Map<Long, ProcessDescriptor> resultMap = Cache.getEntityCache().getAll(ProcessDescriptor.class, ids);
		List<ProcessDescriptor> descriptors = new ArrayList<>(resultMap.values());
		Collections.sort(descriptors, (d1, d2) -> {
			return d1.name.toLowerCase().compareTo(d2.name.toLowerCase());
		});
		return descriptors;
	}

}
