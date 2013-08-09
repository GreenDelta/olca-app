package org.openlca.app.editors.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.Messages;
import org.openlca.app.editors.ProductSystemEditor;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Editors;
import org.openlca.core.model.ProductSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CalculationAction extends Action {

	private Logger log = LoggerFactory.getLogger(getClass());

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
