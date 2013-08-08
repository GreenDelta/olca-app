package org.openlca.app.editors.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.Messages;
import org.openlca.app.editors.ProductSystemEditor;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Editors;
import org.openlca.core.model.ProductSystem;

public class CalculationAction extends Action {

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.CALCULATE_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.Systems_CalculateButtonText;
	}

	@Override
	public void run() {
		ProductSystem productSystem = getProductSystem();
		if (productSystem == null)
			// TODO add error handling, something gone wrong. When the action is
			// active the active editor should always return the product system
			return;
		new CalculationWizardDialog(productSystem).open();
	}

	private ProductSystem getProductSystem() {
		ProductSystemEditor editor = Editors.getActive();
		if (editor == null)
			return null;
		return editor.getModel();
	}
}
