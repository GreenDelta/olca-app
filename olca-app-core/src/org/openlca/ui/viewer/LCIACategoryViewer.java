package org.openlca.ui.viewer;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.model.LCIACategory;
import org.openlca.core.model.LCIAMethod;

@Deprecated
/**
 * Use ImpactCategoryViewer instead
 */
public class LCIACategoryViewer extends AbstractComboViewer<LCIACategory> {

	public LCIACategoryViewer(Composite parent) {
		super(parent);
		setInput(new LCIACategory[0]);
	}

	public void setInput(LCIAMethod method) {
		setInput(method.getLCIACategories());
	}

}
