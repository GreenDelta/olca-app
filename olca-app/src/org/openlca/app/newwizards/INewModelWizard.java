package org.openlca.app.newwizards;

import org.eclipse.ui.INewWizard;
import org.openlca.core.model.Category;

public interface INewModelWizard extends INewWizard {

	void setCategory(Category category);

}
