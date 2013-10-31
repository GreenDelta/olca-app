package org.openlca.app.inventory;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Cache;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Editors;
import org.openlca.app.util.InformationPopup;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.results.InventoryResult;
import org.openlca.io.xls.results.InventoryResultExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryResultActions extends EditorActionBarContributor {

	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(new ExcelExport());
	}

	private class ExcelExport extends Action {

		private Logger log = LoggerFactory.getLogger(getClass());

		public ExcelExport() {
			setImageDescriptor(ImageType.EXCEL_ICON.getDescriptor());
			setText(Messages.ExportToExcel);
			setToolTipText(Messages.ExportToExcel);
		}

		@Override
		public void run() {
			InventoryResultEditor editor = Editors.getActive();
			if (editor == null) {
				log.error("unexpected error: the product system editor is not active");
				return;
			}
			InventoryResult result = editor.getResult();
			CalculationSetup setup = editor.getSetup();
			final File file = FileChooser.forExport("xlsx",
					"inventory_result.xlsx");
			if (file == null)
				return;
			runExport(result, setup, file);

		}

		private void runExport(InventoryResult result, CalculationSetup setup,
				final File file) {
			final InventoryResultExport export = new InventoryResultExport(
					setup, result, Cache.getEntityCache());
			export.setExportFile(file);
			App.run("Export results", export, new Runnable() {
				public void run() {
					if (export.doneWithSuccess())
						InformationPopup.show("Export done",
								"Exported successfully to " + file.getName());
				}
			});
		}

	}

}
