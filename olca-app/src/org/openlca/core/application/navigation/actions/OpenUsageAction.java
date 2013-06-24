package org.openlca.core.application.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.core.application.FeatureFlag;
import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.navigation.ModelElement;
import org.openlca.core.application.views.UsageView;
import org.openlca.core.application.views.UsageViewInput;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Process;
import org.openlca.core.model.Source;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.Editors;

/**
 * Opens a view with the usages of a model in other entities.
 */
public class OpenUsageAction extends Action implements INavigationAction {

	private BaseDescriptor descriptor;
	private Class<?>[] classes = { Actor.class, Source.class, UnitGroup.class,
			FlowProperty.class, Flow.class, Process.class };

	public OpenUsageAction() {
		setText(Messages.Common_Usage);
		setImageDescriptor(ImageType.LINK_16_BLUE.getDescriptor());
	}

	public void setDescriptor(BaseDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	@Override
	public void run() {
		Editors.open(new UsageViewInput(descriptor, Database.get()),
				UsageView.ID);
	}

	@Override
	public boolean accept(INavigationElement navigationElement) {
		if (!FeatureFlag.USAGE_MENU.isEnabled())
			return false;
		if (!(navigationElement instanceof ModelElement))
			return false;
		ModelElement element = (ModelElement) navigationElement;
		Object data = element.getData();
		if (!(data instanceof IModelComponent))
			return false;
		IModelComponent comp = (IModelComponent) data;
		if (isInClasses(comp)) {
			descriptor = Descriptors.toDescriptor(comp);
			return true;
		}
		return false;
	}

	private boolean isInClasses(IModelComponent comp) {
		for (Class<?> clazz : classes)
			if (clazz.isInstance(comp))
				return true;
		return false;
	}

	@Override
	public boolean accept(List<INavigationElement> elements) {
		return false;
	}

}
