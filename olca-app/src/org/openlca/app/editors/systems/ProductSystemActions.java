package org.openlca.app.editors.systems;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.preferences.FeatureFlag;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.FileType;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.calculation.CalculationWizard;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.DataStructures;
import org.openlca.core.math.MatrixRowSorter;
import org.openlca.core.model.ProductSystem;
import org.openlca.io.MatrixImageExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductSystemActions extends EditorActionBarContributor {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void contributeToToolBar(IToolBarManager toolbar) {
		toolbar.add(new CsvExportAction());
		toolbar.add(new ExcelExportAction());
		if (FeatureFlag.MATRIX_IMAGE_EXPORT.isEnabled())
			toolbar.add(new MatrixImageExportAction());
		toolbar.add(Actions.onCalculate(new Runnable() {
			@Override
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
			setImageDescriptor(Images.descriptor(FileType.CSV));
			setText(M.ExportAsMatrix);
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
			setImageDescriptor(Images.descriptor(FileType.EXCEL));
			setText(M.ExcelExport);
		}

		@Override
		public void run() {
			ProductSystem system = getProductSystem();
			new SystemExportDialog(system, Database.get()).open();
		}
	}

	private class MatrixImageExportAction extends Action {
		public MatrixImageExportAction() {
			setImageDescriptor(Icon.SAVE_AS_IMAGE.descriptor());
			setText(M.SaveAsImage);
		}

		@Override
		public void run() {
			final ProductSystem system = getProductSystem();
			final File file = FileChooser.forExport("*.png", "matrix.png");
			if (system == null || file == null)
				return;
			App.run(M.ImageExport, () -> {
				try {
					var setup = new CalculationSetup(system);
					var data = DataStructures.matrixData(Database.get(), setup);
					var matrix = data.techMatrix;
					matrix = new MatrixRowSorter(matrix, App.getSolver()).run();
					new MatrixImageExport(matrix, file).run();
				} catch (Exception e) {
					log.error("Matrix image export failed", e);
				}
			});
		}
	}

}
