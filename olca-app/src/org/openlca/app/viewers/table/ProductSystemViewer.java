package org.openlca.app.viewers.table;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.components.ObjectDialog;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.viewers.table.modify.IModelChangedListener.ModelChangeType;
import org.openlca.core.database.Cache;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Project;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;

public class ProductSystemViewer extends AbstractTableViewer<Long> {

	private Project project;
	private Cache cache;

	public ProductSystemViewer(Composite parent, Cache cache) {
		super(parent);
		this.cache = cache;
	}

	public void setInput(Project project) {
		this.project = project;
		if (project == null)
			setInput(new Long[0]);
		else {
			setInput(project.getProductSystems().toArray(
					new Long[project.getProductSystems().size()]));
		}
	}

	@OnAdd
	protected void onCreate() {
		BaseDescriptor[] descriptors = ObjectDialog
				.multiSelect(ModelType.PRODUCT_SYSTEM);
		if (descriptors != null)
			for (BaseDescriptor descriptor : descriptors)
				add((ProductSystemDescriptor) descriptor);
	}

	private void add(ProductSystemDescriptor descriptor) {
		fireModelChanged(ModelChangeType.CREATE, descriptor.getId());
		setInput(project);
	}

	@OnRemove
	protected void onRemove() {
		for (Long id : getAllSelected())
			fireModelChanged(ModelChangeType.REMOVE, id);
		setInput(project);
	}

	@OnDrop
	protected void onDrop(ProductSystemDescriptor descriptor) {
		if (descriptor != null)
			add(descriptor);
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new ProductSystemLabelProvider();
	}

	private class ProductSystemLabelProvider extends LabelProvider {

		private Map<Long, String> nameCache = new HashMap<Long, String>();

		@Override
		public Image getImage(Object element) {
			return Images.getIcon(ModelType.PRODUCT_SYSTEM);
		}

		@Override
		public String getText(Object element) {
			if (!(element instanceof Long))
				return null;
			Long id = (Long) element;
			if (!nameCache.containsKey(id))
				nameCache.put(id, Labels.getDisplayName(cache
						.getProductSystemDescriptor(id)));
			return nameCache.get(id);
		}
	}

}
