package org.openlca.app.navigation;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

public class NavigationLabelProvider extends ColumnLabelProvider implements
		ICommonLabelProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public String getDescription(Object anElement) {
		if (!(anElement instanceof ModelElement))
			return null;
		ModelElement element = (ModelElement) anElement;
		BaseDescriptor descriptor = element.getContent();
		return Labels.getDisplayInfoText(descriptor);
	}

	@Override
	public Image getImage(Object element) {
		if (!(element instanceof INavigationElement))
			return null;
		Object o = ((INavigationElement<?>) element).getContent();
		if (o instanceof IDatabaseConfiguration)
			return getDatabaseImage((IDatabaseConfiguration) o);
		if (o instanceof Category)
			return Images.getIcon((Category) o);
		if (o instanceof BaseDescriptor)
			return getModelComponentImage((BaseDescriptor) o);
		if (o instanceof ModelType)
			return Images.getIcon(dummyCategory((ModelType) o));
		return null;
	}

	private Category dummyCategory(ModelType type) {
		Category dummy = new Category();
		dummy.setModelType(type);
		return dummy;
	}

	private Image getModelComponentImage(BaseDescriptor modelComponent) {
		if (modelComponent == null || modelComponent.getModelType() == null)
			return null;
		switch (modelComponent.getModelType()) {
		case ACTOR:
			return ImageType.ACTOR_ICON.get();
		case FLOW:
			return ImageType.FLOW_ICON.get();
		case FLOW_PROPERTY:
			return ImageType.FLOW_PROPERTY_ICON.get();
		case IMPACT_METHOD:
			return ImageType.LCIA_ICON.get();
		case PROCESS:
			return ImageType.PROCESS_ICON.get();
		case PRODUCT_SYSTEM:
			return ImageType.PRODUCT_SYSTEM_ICON.get();
		case PROJECT:
			return ImageType.PROJECT_ICON.get();
		case SOURCE:
			return ImageType.SOURCE_ICON.get();
		case UNIT_GROUP:
			return ImageType.UNIT_GROUP_ICON.get();
		default:
			return null;
		}
	}

	private Image getDatabaseImage(IDatabaseConfiguration config) {
		if (Database.isActive(config))
			return ImageType.DB_ICON.get();
		else
			return ImageType.DB_ICON_DIS.get();
	}

	@Override
	public String getText(Object element) {
		if (!(element instanceof INavigationElement))
			return null;
		Object o = ((INavigationElement<?>) element).getContent();
		if (o instanceof IDatabaseConfiguration)
			return ((IDatabaseConfiguration) o).getName();
		if (o instanceof Category)
			return ((Category) o).getName();
		if (o instanceof ModelType)
			return getTypeName((ModelType) o);
		if (o instanceof BaseDescriptor)
			return Labels.getDisplayName((BaseDescriptor) o);
		else
			return null;
	}

	private String getTypeName(ModelType o) {
		if (o == null)
			return null;
		switch (o) {
		case ACTOR:
			return Messages.Actors;
		case FLOW:
			return Messages.Flows;
		case FLOW_PROPERTY:
			return Messages.FlowProperties;
		case IMPACT_METHOD:
			return Messages.ImpactAssessmentMethods;
		case PROCESS:
			return Messages.Processes;
		case PRODUCT_SYSTEM:
			return Messages.ProductSystems;
		case PROJECT:
			return Messages.Projects;
		case SOURCE:
			return Messages.Sources;
		case UNIT_GROUP:
			return Messages.UnitGroups;
		default:
			return Messages.Unknown;
		}
	}

	@Override
	public String getToolTipText(Object element) {
		return getDescription(element);
	}

	@Override
	public void init(ICommonContentExtensionSite aConfig) {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void restoreState(IMemento aMemento) {
	}

	@Override
	public void saveState(IMemento aMemento) {
	}

}
