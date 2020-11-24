package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.wizards.calculation.CalculationWizard;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.model.ModelType;

class CalculateSystemAction extends Action implements INavigationAction {

	private long systemId;

	public CalculateSystemAction() {
		setId(getClass().getCanonicalName());
		setText(M.Calculate);
		setImageDescriptor(Icon.CALCULATION_WIZARD.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof ModelElement))
			return false;
		var element = (ModelElement) first;
		if (element.getContent().type != ModelType.PRODUCT_SYSTEM)
			return false;
		systemId = element.getContent().id;
		return true;
	}

	@Override
	public void run() {
		var system = new ProductSystemDao(Database.get()).getForId(systemId);
		if (system == null)
			return;
		CalculationWizard.open(system);
	}

}
