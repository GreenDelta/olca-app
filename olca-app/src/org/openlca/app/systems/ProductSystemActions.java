package org.openlca.app.systems;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.openlca.core.model.ProductSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductSystemActions extends EditorActionBarContributor {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(new EditAction());
		toolBarManager.add(new CsvExportAction());
		toolBarManager.add(Actions.onCalculate(new Runnable() {
			public void run() {
				log.trace("action -> calculate product system");
				ProductSystem productSystem = getProductSystem();
				if (productSystem == null)
					return;
				new CalculationWizardDialog(productSystem).open();
			}
		}));
	}

	private ProductSystem getProductSystem() {
		ProductSystemEditor editor = Editors.getActive();
		if (editor == null) {
			log.error("unexpected error: the product system editor is not active");
			return null;
		}
		ProductSystem system = editor.getModel();
		if (system == null)
			log.error("The product system is null");
		return editor.getModel();
	}

	private class CsvExportAction extends Action {
		public CsvExportAction() {
			setImageDescriptor(ImageType.MATRIX_ICON.getDescriptor());
			setText(Messages.Systems_MatrixExportAction_Text);
		}

		@Override
		public void run() {
			ProductSystem system = getProductSystem();
			CsvExportShell shell = new CsvExportShell(UI.shell(), system);
			shell.open();
		}
	}

}
