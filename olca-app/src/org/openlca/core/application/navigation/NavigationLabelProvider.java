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

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.IDatabaseServer;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Category;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.LCIAMethod;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.Source;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.model.results.LCIAResult;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.BaseLabelProvider;

/**
 * Implementation of the {@link ICommonLabelProvider} interface for providing
 * labels for the common viewer of the applications navigator
 */
public class NavigationLabelProvider extends BaseLabelProvider implements
		ICommonLabelProvider {

	private Image getCategoryImage(Category category) {
		if (category == null || category.getModelType() == null)
			return null;
		switch (category.getModelType()) {
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

	private Image getModelComponentImage(IModelComponent modelComponent) {
		Image img = null;
		if (modelComponent.getClass() == Flow.class) {
			img = ImageType.FLOW_ICON.get();
		} else if (modelComponent.getClass() == FlowProperty.class) {
			img = ImageType.FLOW_PROPERTY_ICON.get();
		} else if (modelComponent.getClass() == LCIAMethod.class) {
			img = ImageType.LCIA_ICON.get();
		} else if (modelComponent.getClass() == Process.class) {
			img = ImageType.PROCESS_ICON.get();
		} else if (modelComponent.getClass() == ProductSystem.class) {
			img = ImageType.PRODUCT_SYSTEM_ICON.get();
		} else if (modelComponent.getClass() == UnitGroup.class) {
			img = ImageType.UNIT_GROUP_ICON.get();
		} else if (modelComponent.getClass() == Actor.class) {
			img = ImageType.ACTOR_ICON.get();
		} else if (modelComponent.getClass() == Source.class) {
			img = ImageType.SOURCE_ICON.get();
		} else if (modelComponent.getClass() == Project.class) {
			img = ImageType.PROJECT_ICON.get();
		} else if (modelComponent.getClass() == LCIAResult.class) {
			img = ImageType.EXPRESSION_ICON.get();
		}
		return img;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {

	}

	@Override
	public String getDescription(Object anElement) {
		String description = null;
		if (anElement instanceof ModelNavigationElement) {
			ModelNavigationElement navElement = (ModelNavigationElement) anElement;
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
		if (o instanceof IDatabaseServer) {
			IDatabaseServer dataProvider = (IDatabaseServer) o;
			return dataProvider.isRunning() ? ImageType.CONNECT_ICON.get()
					: ImageType.DISCONNECT_ICON.get();
		} else if (element instanceof DatabaseNavigationElement)
			return ImageType.DB_ICON.get();
		else if (o instanceof Category)
			return getCategoryImage((Category) o);
		else if (o instanceof IModelComponent)
			return getModelComponentImage((IModelComponent) o);
		else
			return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof INavigationElement) {
			Object o = ((INavigationElement) element).getData();
			if (element instanceof DataProviderNavigationElement) {
				return "MySQL";
			} else if (element instanceof DatabaseNavigationElement) {
				return ((IDatabase) o).getName();
			} else if (o instanceof Category) {
				return ((Category) o).getName();
			} else if (o instanceof IModelComponent) {
				return super.getModelLabel((IModelComponent) o);
			}
		}
		return null;
	}

	@Override
	public String getToolTipText(Object element) {
		String text = null;
		if (element instanceof ModelNavigationElement) {
			ModelNavigationElement navElem = (ModelNavigationElement) element;
			IModelComponent modelComponent = (IModelComponent) navElem
					.getData();
			text = super.getToolTipText(modelComponent, navElem.getDatabase());
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
