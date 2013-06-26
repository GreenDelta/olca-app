package org.openlca.ui.viewer;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;

@Deprecated
/**
 * Use ImpactCategoryViewer instead
 */
public class LCIACategoryViewer extends AbstractComboViewer<ImpactCategory> {

	public LCIACategoryViewer(Composite parent) {
		super(parent);
		setInput(new ImpactCategory[0]);
	}

	public void setInput(ImpactMethod method) {
		List<ImpactCategory> categories = method.getLCIACategories();
		setInput(categories.toArray(new ImpactCategory[categories.size()]));
	}

}
