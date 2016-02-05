package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.editors.UsageView;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

/**
 * Opens a view with the usages of a model in other entities.
 */
public class OpenUsageAction extends Action implements INavigationAction {

	private CategorizedDescriptor descriptor;

	public OpenUsageAction() {
		setText(M.Usage);
		setImageDescriptor(Icon.LINK.descriptor());
	}

	public void setDescriptor(CategorizedDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	@Override
	public void run() {
		UsageView.open(descriptor);
	}

	@Override
	public boolean accept(INavigationElement<?> navigationElement) {
		if (!(navigationElement instanceof ModelElement))
			return false;
		ModelElement element = (ModelElement) navigationElement;
		descriptor = element.getContent();
		if (descriptor.getModelType() == ModelType.PARAMETER)
			// exclude parameters, because they are not linked via id
			return false; 
		return descriptor.getModelType().isCategorized();
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

}
