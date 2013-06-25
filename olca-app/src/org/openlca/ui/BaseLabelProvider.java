/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.ui;

import org.apache.commons.lang3.text.WordUtils;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.openlca.core.application.Messages;
import org.openlca.core.database.CategoryDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Actor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Category;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.LCIACategory;
import org.openlca.core.model.LCIAMethod;
import org.openlca.core.model.Location;
import org.openlca.core.model.NormalizationWeightingSet;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.Source;
import org.openlca.core.model.UncertaintyDistributionType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.model.results.LCIAResult;
import org.openlca.core.resources.ImageType;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseLabelProvider extends ColumnLabelProvider {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private IDatabase database;

	public BaseLabelProvider() {
	}

	public BaseLabelProvider(IDatabase database) {
		this.database = database;
	}

	@Override
	public Image getImage(Object element) {
		IModelComponent component = null;
		if (element instanceof IModelComponent) {
			component = (IModelComponent) element;
		}
		return component == null ? null : getImage(component);
	}

	protected Image getImage(IModelComponent component) {
		Image img = null;
		if (component.getClass() == Flow.class) {
			img = ImageType.FLOW_ICON.get();
		} else if (component.getClass() == FlowProperty.class) {
			img = ImageType.FLOW_PROPERTY_ICON.get();
		} else if (component.getClass() == LCIAMethod.class) {
			img = ImageType.LCIA_ICON.get();
		} else if (component.getClass() == Process.class) {
			img = ImageType.PROCESS_ICON.get();
		} else if (component.getClass() == ProductSystem.class) {
			img = ImageType.PRODUCT_SYSTEM_ICON.get();
		} else if (component.getClass() == UnitGroup.class) {
			img = ImageType.UNIT_GROUP_ICON.get();
		} else if (component.getClass() == Actor.class) {
			img = ImageType.ACTOR_ICON.get();
		} else if (component.getClass() == Source.class) {
			img = ImageType.SOURCE_ICON.get();
		} else if (component.getClass() == Project.class) {
			img = ImageType.PROJECT_ICON.get();
		} else if (component.getClass() == LCIAResult.class) {
			img = ImageType.EXPRESSION_ICON.get();
		}
		return img;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof IModelComponent)
			return getModelLabel((IModelComponent) element);
		else if (element instanceof LCIACategory)
			return ((LCIACategory) element).getName();
		else if (element instanceof Exchange)
			return getModelLabel(((Exchange) element).getFlow());
		else if (element instanceof FlowPropertyFactor)
			return getModelLabel(((FlowPropertyFactor) element)
					.getFlowProperty());
		else if (element instanceof Unit)
			return ((Unit) element).getName();
		else if (element instanceof Location)
			return ((Location) element).getName();
		else if (element instanceof NormalizationWeightingSet)
			return ((NormalizationWeightingSet) element).getReferenceSystem();
		else if (element instanceof BaseDescriptor)
			return ((BaseDescriptor) element).getDisplayName();
		else if (element instanceof Enum<?>)
			return getEnumText(element);
		else if (element != null)
			return element.toString();
		else
			return null;
	}

	private String getEnumText(Object enumValue) {
		if (enumValue instanceof AllocationMethod)
			return Labels.allocationMethod((AllocationMethod) enumValue);
		if (enumValue instanceof FlowPropertyType)
			return Labels.flowPropertyType((FlowPropertyType) enumValue);
		if (enumValue instanceof FlowType)
			return Labels.flowType((FlowType) enumValue);
		if (enumValue instanceof ProcessType)
			return Labels.processType((ProcessType) enumValue);
		if (enumValue instanceof UncertaintyDistributionType)
			return Labels
					.uncertaintyType((UncertaintyDistributionType) enumValue);
		if (enumValue != null)
			return enumValue.toString();
		return null;
	}

	protected String getModelLabel(IModelComponent o) {
		if (o == null)
			return "";
		String label = Strings.cut(o.getName(), 75);
		Location location = null;
		if (o instanceof Flow)
			location = ((Flow) o).getLocation();
		else if (o instanceof Process)
			location = ((Process) o).getLocation();
		if (location != null && location.getCode() != null)
			label += " (" + location.getCode() + ")";
		return label;
	}

	@Override
	public String getToolTipText(Object element) {
		return getToolTipText2(element);
	}

	/**
	 * Getter of the tooltip of a specific element on a specific database
	 * 
	 * @param element
	 *            The element to create the tool tip text for
	 * @param database
	 *            The database
	 * @return The tool tip text for the element
	 */
	protected String getToolTipText(Object element, IDatabase database) {
		IDatabase old = this.database;
		this.database = database;
		String text = getToolTipText2(element);
		this.database = old;
		return text;
	}

	private String getToolTipText2(Object element) {
		if (!(element instanceof IModelComponent))
			return null;
		IModelComponent component = (IModelComponent) element;
		String name = component.getName();
		String text = name == null ? "" : name + "\n";

		text = addLocationAndType(component, text);

		if (database != null) {
			try {
				CategoryDao dao = new CategoryDao(database.getEntityFactory());
				Category category = dao.getForId(component.getCategoryId());
				if (category != null)
					text += Messages.Category + ": "
							+ dao.getShortPath(component.getCategoryId());
			} catch (Exception e) {
				log.error("Loading category failed", e);
			}
		}

		String description = component.getDescription();
		if (description != null)
			text += "\n" + Messages.Description + ": "
					+ WordUtils.wrap(description, 65, "\n", false);

		return text;
	}

	private String addLocationAndType(IModelComponent component, String text) {
		Location location = null;
		String type = null;
		if (component instanceof Process) {
			Process p = (Process) component;
			location = p.getLocation();
			if (p.getProcessType() != null)
				type = Labels.processType(p);
		} else if (component instanceof Flow) {
			Flow f = (Flow) component;
			location = f.getLocation();
		}
		if (type != null)
			text += type + "\n";
		if (location != null)
			text += Messages.Common_Location + ": " + location.getName() + "\n";
		return text;
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

}
