package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.systems.CalculationWizard;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;

class CalculateSystemAction extends Action implements INavigationAction {

	private long systemId;

	public CalculateSystemAction() {
		setId(getClass().getCanonicalName());
		setText(M.Calculate);
		setImageDescriptor(Icon.CALCULATION_WIZARD.descriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> elem) {
		if (!(elem instanceof ModelElement))
			return false;
		ModelElement element = (ModelElement) elem;
		if (element.getContent().type != ModelType.PRODUCT_SYSTEM)
			return false;
		systemId = element.getContent().id;
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		ProductSystem system = new ProductSystemDao(Database.get()).getForId(systemId);
		if (system == null)
			return;
		CalculationWizard.open(system);
	}

}
