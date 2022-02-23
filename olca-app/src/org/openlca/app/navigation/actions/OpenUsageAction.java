package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.search.ParameterUsagePage;
import org.openlca.app.search.SearchPage;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.RootDescriptor;

/**
 * Opens a view with the usages of a model in other entities.
 */
class OpenUsageAction extends Action implements INavigationAction {

	private RootDescriptor descriptor;

	public OpenUsageAction() {
		setText(M.Usage);
		setImageDescriptor(Icon.LINK.descriptor());
	}

	public void setDescriptor(RootDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof ModelElement e))
			return false;
		var d = e.getContent();
		if (d == null || d.type == null)
			return false;
		if (!d.type.isRoot())
			return false;
		descriptor = d;
		return true;
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
