package org.openlca.app.viewers;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.navigation.elements.Group;
import org.openlca.app.navigation.elements.GroupType;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.RefEntity;
import org.openlca.core.model.UncertaintyType;
import org.openlca.core.model.descriptors.Descriptor;

public class BaseLabelProvider extends ColumnLabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element instanceof RefEntity entity)
			return Images.get(entity);
		if (element instanceof Descriptor descriptor)
			return Images.get(descriptor);
		if (element instanceof Exchange exchange)
			return Images.get(exchange.flow);
		if (element instanceof FlowType flowType)
			return Images.get(flowType);
		if (element instanceof ProcessType processType)
			return Images.get(processType);
		if (element instanceof ModelType modelType)
			return Images.get(modelType);
		if (element instanceof EnviFlow enviFlow)
			return Images.get(enviFlow);
		if (element instanceof TechFlow techFlow)
			return Images.get(techFlow.provider());
		if (element instanceof Group group)
			return Images.get(group);
		if (element instanceof GroupType groupType)
			return Images.get(groupType);
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Descriptor descriptor)
			return Labels.name(descriptor);
		if (element instanceof RefEntity entity)
			return Labels.name(entity);
		if (element instanceof Exchange exchange)
			return Labels.name(exchange.flow);
		if (element instanceof FlowPropertyFactor factor)
			return Labels.name(factor.flowProperty);
		if (element instanceof EnviFlow enviFlow)
			return Labels.name(enviFlow);
		if (element instanceof TechFlow techFlow)
			return Labels.name(techFlow);
		if (element instanceof Enum<?>)
			return getEnumText(element);
		if (element != null)
			return element.toString();
		return null;
	}

	private String getEnumText(Object enumValue) {
		if (enumValue instanceof AllocationMethod)
			return Labels.of((AllocationMethod) enumValue);
		if (enumValue instanceof FlowPropertyType)
			return Labels.of((FlowPropertyType) enumValue);
		if (enumValue instanceof FlowType)
			return Labels.of((FlowType) enumValue);
		if (enumValue instanceof ProcessType)
			return Labels.of((ProcessType) enumValue);
		if (enumValue instanceof ModelType)
			return Labels.of((ModelType) enumValue);
		if (enumValue instanceof UncertaintyType)
			return Labels.of((UncertaintyType) enumValue);
		if (enumValue != null)
			return enumValue.toString();
		return null;
	}


	@Override
	public String getToolTipText(Object obj) {
		if (obj instanceof RefEntity e)
			return e.description;
		return null;
	}

}
