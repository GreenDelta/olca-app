package org.openlca.app.results.comparison.component;

import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.core.model.descriptors.CategoryDescriptor;

public class HighlightCategoryCombo extends AbstractComboViewer<CategoryDescriptor> {

	public HighlightCategoryCombo(Composite parent, CategoryDescriptor... values) {
		super(parent);
		setNullText("No Process Category was selected");
		setInput(values);
	}

	@Override
	public Class<CategoryDescriptor> getType() {
		return CategoryDescriptor.class;
	}

	@Override
	public void select(CategoryDescriptor value) {
		if (value == null) {
			if (isNullable())
				((TableCombo) getViewer().getControl()).select(0);
		} else
			super.select(value);
	}

}
