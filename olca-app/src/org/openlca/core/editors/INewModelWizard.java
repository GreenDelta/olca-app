package org.openlca.core.editors;

import org.eclipse.ui.INewWizard;
import org.openlca.core.model.Category;

public interface INewModelWizard extends INewWizard {

	void setCategory(Category category);

}
