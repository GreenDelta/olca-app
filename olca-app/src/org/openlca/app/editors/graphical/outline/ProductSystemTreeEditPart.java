package org.openlca.app.editors.graphical.outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.editparts.AbstractTreeEditPart;
import org.openlca.app.db.Database;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProductSystemTreeEditPart extends AbstractTreeEditPart {

	@Override
	public ProductSystem getModel() {
		return (ProductSystem) super.getModel();
	}

	@Override
	protected List<ProcessDescriptor> getModelChildren() {
		Map<Long, ProcessDescriptor> resultMap = Database.getEntityCache().getAll(
				ProcessDescriptor.class, getModel().getProcesses());
		List<ProcessDescriptor> descriptors = new ArrayList<>(
				resultMap.values());
		Collections.sort(descriptors, new Comparator<ProcessDescriptor>() {

			@Override
			public int compare(ProcessDescriptor d1, ProcessDescriptor d2) {
				return d1.getName().toLowerCase()
						.compareTo(d2.getName().toLowerCase());
			}

		});
		return descriptors;
	}

}
