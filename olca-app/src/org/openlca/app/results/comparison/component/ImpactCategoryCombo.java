package org.openlca.app.results.comparison.component;

import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.core.model.descriptors.ImpactDescriptor;

public class ImpactCategoryCombo extends AbstractComboViewer<ImpactDescriptor> {

	public ImpactCategoryCombo(Composite parent, ImpactDescriptor... values) {
		super(parent);
		setNullText("No Impact Category was selected");
		setInput(values);
	}

	@Override
	public Class<ImpactDescriptor> getType() {
		return ImpactDescriptor.class;
	}

	@Override
	public void select(ImpactDescriptor value) {
		if (value == null) {
			if (isNullable())
				((TableCombo) getViewer().getControl()).select(0);
		} else
			super.select(value);
	}

}
