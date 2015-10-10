package org.openlca.app.cloud.navigation;

import java.util.LinkedList;
import java.util.Queue;

import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.CategoryDescriptor;
import org.openlca.core.model.descriptors.Descriptors;

import com.greendelta.cloud.model.data.DatasetIdentifier;

public class NavigationUtil {

	public static INavigationElement<?> findElement(NavigationRoot root,
			ModelType type, Long id) {
		Queue<INavigationElement<?>> queue = new LinkedList<>();
		queue.add(root);
		while (!queue.isEmpty()) {
			INavigationElement<?> next = queue.poll();
			if (contentEqual(next.getContent(), type, id))
				return next;
			if (!(next instanceof ModelTypeElement))
				queue.addAll(next.getChildren());
			else if (type == ModelType.CATEGORY)
				queue.addAll(next.getChildren());
			else if (next.getContent() == type)
				queue.addAll(next.getChildren());
		}
		return null;
	}

	private static boolean contentEqual(Object content, ModelType type, Long id) {
		if (id == null)
			id = 0l;
		CategorizedDescriptor descriptor = toDescriptor(content);
		if (descriptor == null)
			if (content instanceof ModelType && id == 0)
				return content == type;
			else
				return false;
		if (descriptor.getModelType() != type)
			return false;
		return descriptor.getId() == id;
	}

	private static CategorizedDescriptor toDescriptor(Object object) {
		if (object instanceof CategorizedDescriptor)
			return (CategorizedDescriptor) object;
		if (object instanceof CategorizedEntity)
			return Descriptors.toDescriptor((CategorizedEntity) object);
		return null;
	}

	public static INavigationElement<?> findParent(NavigationRoot root,
			CategorizedDescriptor model) {
		if (model.getCategory() != null)
			return findElement(root, ModelType.CATEGORY, model.getCategory());
		if (model.getModelType() != ModelType.CATEGORY)
			return findElement(root, model.getModelType(), 0l);
		CategoryDescriptor category = (CategoryDescriptor) model;
		return findElement(root, category.getCategoryType(), 0l);
	}

	public static DatasetIdentifier toIdentifier(INavigationElement<?> element) {
		CategorizedDescriptor descriptor = null;
		if (element instanceof CategoryElement) {
			descriptor = Descriptors.toDescriptor(((CategoryElement) element)
					.getContent());
		} else if (element instanceof ModelElement)
			descriptor = ((ModelElement) element).getContent();
		if (descriptor == null)
			return null;
		Category category = null;
		if (element.getParent() instanceof CategoryElement)
			category = ((CategoryElement) element.getParent()).getContent();
		return CloudUtil.toIdentifier(descriptor,
				Descriptors.toDescriptor(category));
	}
	
}
