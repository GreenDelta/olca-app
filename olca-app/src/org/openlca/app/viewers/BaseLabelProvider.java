package org.openlca.app.viewers;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.navigation.elements.Group;
import org.openlca.app.navigation.elements.GroupType;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.RefEntity;
import org.openlca.core.model.UncertaintyType;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Strings;

public class BaseLabelProvider extends ColumnLabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element instanceof RefEntity)
			return Images.get((RefEntity) element);
		if (element instanceof Descriptor)
			return Images.get((Descriptor) element);
		if (element instanceof Exchange)
			return Images.get(((Exchange) element).flow);
		if (element instanceof FlowType)
			return Images.get((FlowType) element);
		if (element instanceof ProcessType)
			return Images.get((ProcessType) element);
		if (element instanceof ModelType)
			return Images.get((ModelType) element);
		if (element instanceof Group)
			return Images.get((Group) element);
		if (element instanceof GroupType)
			return Images.get((GroupType) element);
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Descriptor)
			return getModelLabel((Descriptor) element);
		if (element instanceof RefEntity)
			return getModelLabel((RefEntity) element);
		if (element instanceof Exchange)
			return getModelLabel(((Exchange) element).flow);
		if (element instanceof FlowPropertyFactor)
			return getModelLabel(((FlowPropertyFactor) element).flowProperty);
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

	protected String getModelLabel(RefEntity o) {
		if (o == null)
			return "";
		String label = Strings.cut(o.name, 75);
		Location location = null;
		if (o instanceof Flow)
			location = ((Flow) o).location;
		else if (o instanceof Process)
			location = ((Process) o).location;
		if (location != null && location.code != null)
			label += " (" + location.code + ")";
		return label;
	}

	protected String getModelLabel(Descriptor d) {
		if (d == null)
			return null;
		return Strings.cut(Labels.name(d), 75);
	}

	@Override
	public String getToolTipText(Object obj) {
		if (obj instanceof Descriptor)
			return ((Descriptor) obj).description;
		return null;
	}

}
