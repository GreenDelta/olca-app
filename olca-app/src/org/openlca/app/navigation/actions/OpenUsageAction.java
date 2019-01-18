package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.search.ParameterUsagePage;
import org.openlca.app.search.SearchPage;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

/**
 * Opens a view with the usages of a model in other entities.
 */
class OpenUsageAction extends Action implements INavigationAction {

	private CategorizedDescriptor descriptor;

	public OpenUsageAction() {
		setText(M.Usage);
		setImageDescriptor(Icon.LINK.descriptor());
	}

	public void setDescriptor(CategorizedDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	@Override
	public boolean accept(INavigationElement<?> elem) {
		if (!(elem instanceof ModelElement))
			return false;
		ModelElement e = (ModelElement) elem;
		CategorizedDescriptor d = e.getContent();
		if (d == null || d.type == null)
			return false;
		if (!d.type.isCategorized())
			return false;
		descriptor = d;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		if (descriptor == null)
			return;
		if (descriptor.type == ModelType.PARAMETER) {
			ParameterUsagePage.show(descriptor.name);
		} else {
			SearchPage.forUsage(descriptor);
		}
	}

}
