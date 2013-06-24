/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.navigation;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;
import org.openlca.core.application.db.Database;
import org.openlca.core.application.db.IDatabaseConfiguration;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Category;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.Source;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.resources.ImageType;
import org.openlca.ilcd.methods.LCIAMethod;
import org.openlca.ilcd.processes.LCIAResult;

/**
 * Implementation of the {@link ICommonLabelProvider} interface for providing
 * labels for the common viewer of the applications navigator
 */
public class NavigationLabelProvider extends ColumnLabelProvider implements
		ICommonLabelProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {

	}

	@Override
	public String getDescription(Object anElement) {
		String description = null;
		if (anElement instanceof ModelElement) {
			ModelElement navElement = (ModelElement) anElement;
			IModelComponent component = (IModelComponent) navElement.getData();
			if (component.getDescription() != null
					&& component.getDescription().length() > 0) {
				description = component.getDescription();
			}
		}
		return description;
	}

	@Override
	public Image getImage(Object element) {
		if (!(element instanceof INavigationElement))
			return null;
		Object o = ((INavigationElement) element).getData();
		if (o instanceof IDatabaseConfiguration)
			return getDatabaseImage((IDatabaseConfiguration) o);
		if (o instanceof Category)
			return getCategoryImage((Category) o);
		if (o instanceof BaseDescriptor)
			return getModelComponentImage((BaseDescriptor) o);
		if (o instanceof ModelType)
			return getCategoryImage((ModelType) o);
		return null;
	}

	private Image getCategoryImage(Category category) {
		if (category == null)
			return null;
		return getCategoryImage(category.getModelType());
	}

	private Image getCategoryImage(ModelType modelType) {
		if (modelType == null)
			return null;
		switch (modelType) {
		case ACTOR:
			return ImageType.ACTOR_CATEGORY_ICON.get();
		case FLOW:
			return ImageType.FLOW_CATEGORY_ICON.get();
		case FLOW_PROPERTY:
			return ImageType.FLOW_PROPERTY_CATEGORY_ICON.get();
		case IMPACT_METHOD:
			return ImageType.LCIA_CATEGORY_ICON.get();
		case IMPACT_RESULT:
			return ImageType.FOLDER_EXPR.get();
		case PROCESS:
			return ImageType.PROCESS_CATEGORY_ICON.get();
		case PRODUCT_SYSTEM:
			return ImageType.PRODUCT_SYSTEM_CATEGORY_ICON.get();
		case PROJECT:
			return ImageType.PROJECT_CATEGORY_ICON.get();
		case SOURCE:
			return ImageType.SOURCE_CATEGORY_ICON.get();
		case UNIT_GROUP:
			return ImageType.UNIT_GROUP_CATEGORY_ICON.get();
		default:
			return ImageType.FOLD_ICON.get();
		}
	}

	private Image getModelComponentImage(BaseDescriptor modelComponent) {
		if (modelComponent == null || modelComponent.getModelType() == null)
			return null;
		switch (modelComponent.getModelType()) {
		case ACTOR:

			break;

		default:
			break;
		}

		if (modelComponent.getModelType() == ModelType.FLOW)
			return ImageType.FLOW_ICON.get();
		else if (modelComponent.getClass() == FlowProperty.class)
			return ImageType.FLOW_PROPERTY_ICON.get();
		else if (modelComponent.getClass() == LCIAMethod.class)
			return ImageType.LCIA_ICON.get();
		else if (modelComponent.getClass() == Process.class)
			return ImageType.PROCESS_ICON.get();
		else if (modelComponent.getClass() == ProductSystem.class)
			return ImageType.PRODUCT_SYSTEM_ICON.get();
		else if (modelComponent.getClass() == UnitGroup.class)
			return ImageType.UNIT_GROUP_ICON.get();
		else if (modelComponent.getClass() == Actor.class)
			return ImageType.ACTOR_ICON.get();
		else if (modelComponent.getClass() == Source.class)
			return ImageType.SOURCE_ICON.get();
		else if (modelComponent.getClass() == Project.class)
			return ImageType.PROJECT_ICON.get();
		else if (modelComponent.getClass() == LCIAResult.class)
			return ImageType.EXPRESSION_ICON.get();

		return null;
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
		Object o = ((INavigationElement) element).getData();
		if (o instanceof IDatabaseConfiguration)
			return ((IDatabaseConfiguration) o).getName();
		if (o instanceof Category)
			return ((Category) o).getName();
		if (o instanceof ModelType)
			return getTypeName((ModelType) o);
		else
			return null;
	}

	private String getTypeName(ModelType o) {
		if (o == null)
			return null;
		switch (o) {
		case ACTOR:
			return "Actors";
		case FLOW:
			return "Flows";
		case FLOW_PROPERTY:
			return "Flow properties";
		case IMPACT_METHOD:
			return "LCIA methods";
		case PROCESS:
			return "Processes";
		case PRODUCT_SYSTEM:
			return "Product systems";
		case PROJECT:
			return "Projects";
		case SOURCE:
			return "Sources";
		case UNIT_GROUP:
			return "Unit groups";
		default:
			return "<<unknown>>";
		}
	}

	@Override
	public String getToolTipText(Object element) {
		String text = null;
		if (element instanceof ModelElement) {
			ModelElement navElem = (ModelElement) element;
			IModelComponent modelComponent = (IModelComponent) navElem
					.getData();
		}
		return text;
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
