package org.openlca.app.editors.systems;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.openlca.core.math.DataStructures;
import org.openlca.core.math.IMatrix;
import org.openlca.core.math.IMatrixFactory;
import org.openlca.core.math.MatrixRowSorter;
import org.openlca.core.matrix.Inventory;
import org.openlca.core.model.ProductSystem;
import org.openlca.io.MatrixImageExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductSystemActions extends EditorActionBarContributor {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(new CsvExportAction());
		toolBarManager.add(new ExcelExportAction());
		if (FeatureFlag.MATRIX_IMAGE_EXPORT.isEnabled())
			toolBarManager.add(new MatrixImageExportAction());
		toolBarManager.add(Actions.onCalculate(new Runnable() {
			public void run() {
				log.trace("action -> calculate product system");
				ProductSystem productSystem = getProductSystem();
				if (productSystem == null)
					return;
				CalculationWizard.open(productSystem);
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
			setText(Messages.ExportAsMatrix);
		}

		@Override
		public void run() {
			ProductSystem system = getProductSystem();
			CsvExportShell shell = new CsvExportShell(UI.shell(), system);
			shell.open();
		}
	}

	private class ExcelExportAction extends Action {
		public ExcelExportAction() {
			setImageDescriptor(ImageType.FILE_EXCEL_SMALL.getDescriptor());
			setText(Messages.ExportAsMatrix);
		}

		@Override
		public void run() {
			ProductSystem system = getProductSystem();
			new SystemExportDialog(system, Database.get()).open();
		}
	}

	private class MatrixImageExportAction extends Action {
		public MatrixImageExportAction() {
			setImageDescriptor(ImageType.SAVE_AS_IMAGE_ICON.getDescriptor());
			setText(Messages.SaveAsImage);
		}

		@Override
		public void run() {
			final ProductSystem system = getProductSystem();
			final File file = FileChooser.forExport("*.png", "matrix.png");
			if (system == null || file == null)
				return;
			App.run(Messages.ImageExport, new Runnable() {
				public void run() {
					try {
						Inventory inventory = DataStructures.createInventory(
								system, Cache.getMatrixCache());
						IMatrixFactory<?> factory = App.getSolver()
								.getMatrixFactory();
						IMatrix matrix = inventory.getTechnologyMatrix()
								.createRealMatrix(factory);
						matrix = new MatrixRowSorter(matrix, factory).run();
						new MatrixImageExport(matrix, file).run();
					} catch (Exception e) {
						log.error("Matrix image export failed", e);
					}
				}
			});
		}
	}

}
