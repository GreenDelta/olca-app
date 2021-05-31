package org.openlca.app.editors.systems;

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
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
import org.openlca.app.util.UI;
import org.openlca.app.wizards.calculation.CalculationWizard;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.MatrixRowSorter;
import org.openlca.core.matrix.MatrixData;
import org.openlca.core.model.ProductSystem;
import org.openlca.io.MatrixImageExport;

public class ProductSystemActions extends EditorActionBarContributor {

	@Override
	public void contributeToToolBar(IToolBarManager toolbar) {
		// toolbar.add(new CsvExportAction());

		// open the matrix export dialog
		toolbar.add(Actions.create(
			M.ExportAsMatrix,
			Images.descriptor(FileType.CSV),
			() -> {
				var system = getProductSystem();
				if (system != null) {
					MatrixExportDialog.open(Database.get(), system);
				}
			}));

		// toolbar.add(new ExcelExportAction());

		// add the experimental matrix image export
		if (FeatureFlag.MATRIX_IMAGE_EXPORT.isEnabled())
			toolbar.add(new MatrixImageExportAction());
		toolbar.add(Actions.onCalculate(() -> {
			var system = getProductSystem();
			if (system == null)
				return;
			CalculationWizard.open(system);
		}));
	}

	private ProductSystem getProductSystem() {
		try {
			ProductSystemEditor editor = Editors.getActive();
			return editor == null
				? null
				: editor.getModel();
		} catch (Exception e) {
			ErrorReporter.on("failed to get product system", e);
			return null;
		}
	}

	@Deprecated
	private class CsvExportAction extends Action {
		public CsvExportAction() {
			setImageDescriptor(Images.descriptor(FileType.CSV));
			setText(M.ExportAsMatrix);
		}

		@Override
		public void run() {
			var system = getProductSystem();
			var shell = new CsvExportShell(UI.shell(), system);
			shell.open();
		}
	}

	@Deprecated
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
			var system = getProductSystem();
			var file = FileChooser.forSavingFile(M.Export, "matrix.png");
			if (system == null || file == null)
				return;
			App.run(M.ImageExport, () -> {
				try {
					var setup = new CalculationSetup(system);
					var data = MatrixData.of(Database.get(), setup);
					var matrix = data.techMatrix.asMutable();
					matrix = new MatrixRowSorter(matrix, App.getSolver()).run();
					new MatrixImageExport(matrix, file).run();
				} catch (Exception e) {
					ErrorReporter.on("failed to export system as image", e);
				}
			});
		}
	}

}
