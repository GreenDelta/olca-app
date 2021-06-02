package org.openlca.app.results.comparison.component;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ImpactDescriptor;

public class ImpactCategoryTable {
	private TableViewer viewer;
	List<ImpactDescriptor> categories;

	public ImpactCategoryTable(Composite body, List<ImpactDescriptor> categories) {
		this.categories = categories;
		List<CategoryVariant> l = categories.stream().map(c -> new CategoryVariant(c)).collect(Collectors.toList());
		viewer = Tables.createViewer(body, "Impact Category", "Display");
		viewer.setLabelProvider(new CategoryLabelProvider());
		new ModifySupport<CategoryVariant>(viewer).bind("Display", new DisplayModifier());
		viewer.setInput(l);
		Tables.bindColumnWidths(viewer, 0.85, 0.16);
	}

	public List<ImpactDescriptor> getImpactDescriptors() {
		return categories;
	}

	private class CategoryLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			var category = (CategoryVariant) obj;
			return switch (col) {
			case 0 -> Images.get(ModelType.IMPACT_CATEGORY);
			case 1 -> category.isDisabled ? Icon.CHECK_FALSE.get() : Icon.CHECK_TRUE.get();
			default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			var category = (CategoryVariant) obj;
			return switch (col) {
			case 0 -> category.category.name;
			default -> null;
			};
		}
	}

	private class DisplayModifier extends CheckBoxCellModifier<CategoryVariant> {
		@Override
		protected boolean isChecked(CategoryVariant v) {
			return !v.isDisabled;
		}

		@Override
		protected void setChecked(CategoryVariant v, boolean b) {
			if (v.isDisabled != b)
				return;
			if (b)
				categories.add(v.category);
			else
				categories.remove(v.category);
			v.isDisabled = !b;
		}
	}

	private class CategoryVariant {
		public ImpactDescriptor category;
		public boolean isDisabled;

		public CategoryVariant(ImpactDescriptor c) {
			category = c;
			isDisabled = false;
		}
	}
}
