package org.openlca.app.editors.actions;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.editors.ProductSystemEditor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Editors;
import org.openlca.core.model.ProductSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductSystemActionContributor extends EditorActionBarContributor {

	private CalculationAction calculationAction = new CalculationAction();
	private EditAction editAction = new EditAction();
	private Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(calculationAction);
		toolBarManager.add(editAction);
		toolBarManager.add(Actions.onCalculate(new Runnable() {
			public void run() {
				tryCalculate();
			}
		}));
	}

	private void tryCalculate() {
		log.trace("action -> calculate product system");
		ProductSystem productSystem = getProductSystem();
		if (productSystem == null) {
			log.error("unexpected error: product system is null");
			return;
		}
		new CalculationWizardDialog(productSystem).open();
	}

	private ProductSystem getProductSystem() {
		ProductSystemEditor editor = Editors.getActive();
		if (editor == null)
			return null;
		return editor.getModel();
	}

}
