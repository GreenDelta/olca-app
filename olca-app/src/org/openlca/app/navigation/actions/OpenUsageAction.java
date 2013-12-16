package org.openlca.app.navigation.actions;

import java.util.EnumSet;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.FeatureFlag;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.UsageView;
import org.openlca.app.editors.UsageViewInput;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Editors;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

/**
 * Opens a view with the usages of a model in other entities.
 */
public class OpenUsageAction extends Action implements INavigationAction {

	private BaseDescriptor descriptor;

	//@formatter:off
	private EnumSet<ModelType> types = EnumSet.of(
			ModelType.ACTOR,
			ModelType.SOURCE, 
			ModelType.UNIT_GROUP, 
			ModelType.FLOW_PROPERTY,
			ModelType.FLOW, 
			ModelType.PROCESS,
			ModelType.IMPACT_METHOD);
	//@formatter:on

	public OpenUsageAction() {
		setText(Messages.Usage);
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
	public boolean accept(INavigationElement<?> navigationElement) {
		if (!FeatureFlag.USAGE_MENU.isEnabled())
			return false;
		if (!(navigationElement instanceof ModelElement))
			return false;
		ModelElement element = (ModelElement) navigationElement;
		descriptor = element.getContent();
		return types.contains(descriptor.getModelType());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

}
