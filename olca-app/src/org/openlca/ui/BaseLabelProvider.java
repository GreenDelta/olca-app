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
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Category;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.Location;
import org.openlca.core.model.NormalizationWeightingSet;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.UncertaintyDistributionType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseLabelProvider extends ColumnLabelProvider {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	public Image getImage(Object element) {
		if (element instanceof RootEntity)
			return Images.getIcon((RootEntity) element);
		if (element instanceof BaseDescriptor)
			return Images.getIcon(((BaseDescriptor) element).getModelType());
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof BaseDescriptor)
			return getModelLabel((BaseDescriptor) element);
		if (element instanceof RootEntity)
			return getModelLabel((RootEntity) element);
		if (element instanceof ImpactCategory)
			return ((ImpactCategory) element).getName();
		if (element instanceof Exchange)
			return getModelLabel(((Exchange) element).getFlow());
		if (element instanceof FlowPropertyFactor)
			return getModelLabel(((FlowPropertyFactor) element)
					.getFlowProperty());
		if (element instanceof Unit)
			return ((Unit) element).getName();
		if (element instanceof Location)
			return ((Location) element).getName();
		if (element instanceof NormalizationWeightingSet)
			return ((NormalizationWeightingSet) element).getReferenceSystem();
		if (element instanceof BaseDescriptor)
			return ((BaseDescriptor) element).getDisplayName();
		if (element instanceof Enum<?>)
			return getEnumText(element);
		if (element != null)
			return element.toString();
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

	protected String getModelLabel(RootEntity o) {
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

	protected String getModelLabel(BaseDescriptor d) {
		if (d == null)
			return null;
		return Strings.cut(d.getDisplayName(), 75);
	}

	@Override
	public String getToolTipText(Object element) {
		return getToolTipText2(element);
	}

	private String getToolTipText2(Object element) {
		if (!(element instanceof RootEntity))
			return null;
		RootEntity component = (RootEntity) element;
		String name = component.getName();
		String text = name == null ? "" : name + "\n";

		text = addLocationAndType(component, text);

		try {
			Category category = component.getCategory();
			if (category != null)
				text += Messages.Category + ": "
						+ CategoryPath.getShort(category);
		} catch (Exception e) {
			log.error("Loading category failed", e);
		}

		String description = component.getDescription();
		if (description != null)
			text += "\n" + Messages.Description + ": "
					+ WordUtils.wrap(description, 65, "\n", false);

		return text;
	}

	private String addLocationAndType(RootEntity component, String text) {
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
