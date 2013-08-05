package org.openlca.app.navigation.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

class ExportAction extends Action implements INavigationAction {

	private List<BaseDescriptor> components;
	private ModelType type;
	private List<Long> visited;
	private ModelType[] acceptedTypes;

	ExportAction(ModelType... acceptedTypes) {
		if (acceptedTypes == null)
			this.acceptedTypes = new ModelType[0];
		else
			this.acceptedTypes = acceptedTypes;
	}

	private boolean accepted(ModelType type) {
		for (ModelType t : acceptedTypes)
			if (type == t)
				return true;
		return false;
	}

	private boolean accepted(INavigationElement<?> element) {
		if (!(element instanceof ModelTypeElement
				|| element instanceof CategoryElement || element instanceof ModelElement))
			return false;

		ModelType elementType = null;
		if (element instanceof ModelTypeElement)
			elementType = ((ModelTypeElement) element).getContent();
		else if (element instanceof CategoryElement)
			elementType = ((CategoryElement) element).getContent()
					.getModelType();
		else
			elementType = ((ModelElement) element).getContent().getModelType();

		if (type != null && type != elementType)
			return false;
		if (!accepted(elementType))
			return false;
		type = elementType;
		return true;
	}

	private List<BaseDescriptor> getComponents(INavigationElement<?> element) {
		if (!(element instanceof ModelTypeElement
				|| element instanceof CategoryElement || element instanceof ModelElement))
			return Collections.emptyList();

		if (element instanceof ModelTypeElement
				|| element instanceof CategoryElement) {
			List<BaseDescriptor> list = new ArrayList<>();
			for (INavigationElement<?> child : element.getChildren())
				list.addAll(getComponents(child));
			return list;
		}

		BaseDescriptor descriptor = ((ModelElement) element).getContent();
		if (visited.contains(descriptor.getId()))
			return Collections.emptyList();

		visited.add(descriptor.getId());
		return Collections.singletonList(descriptor);
	}

	protected ModelType getType() {
		return type;
	}

	List<BaseDescriptor> getComponents() {
		return components;
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		visited = new ArrayList<>();
		type = null;
		if (!accepted(element))
			return false;
		components = getComponents(element);
		return !components.isEmpty();
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		visited = new ArrayList<>();
		type = null;
		for (INavigationElement<?> element : elements)
			if (!accepted(element))
				return false;
		components = new ArrayList<>();
		for (INavigationElement<?> element : elements)
			components.addAll(getComponents(element));
		return !components.isEmpty();
	}

}
