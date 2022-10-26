package org.openlca.app.editors.systems;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IActionBars2;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.tools.graphics.MultiPageSubActionBars;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.actions.GraphBarContributor;
import org.openlca.app.preferences.FeatureFlag;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
import org.openlca.app.wizards.calculation.CalculationWizard;
import org.openlca.core.math.MatrixRowSorter;
import org.openlca.core.matrix.MatrixData;
import org.openlca.core.matrix.index.TechIndex;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.ProductSystem;
import org.openlca.io.MatrixImageExport;

/**
 * A special implementation of a
 * <code>MultiPageEditorActionBarContributor</code> to switch between
 * action bar contributions for product system editor pages.
 */
public class ProductSystemBarContributor extends
		MultiPageEditorActionBarContributor {

	private IActionBars2 actionBars2;
	private MultiPageSubActionBars graphicalSubActionBars;
	private MultiPageSubActionBars activeEditorActionBars;

	@Override
	public void init(IActionBars bars) {
		super.init(bars);
		assert bars instanceof IActionBars2;
		actionBars2 = (IActionBars2) bars;
	}

	/**
	 * This is the base contribution to the toolbar: the actions that always
	 * appear independently of the active page.
	 * @param toolBar the manager that controls the workbench toolbar
	 */
	@Override
	public void contributeToToolBar(IToolBarManager toolBar) {
		// open the matrix export dialog
		toolBar.add(Actions.create(
				M.ExportAsMatrix,
				Images.descriptor(FileType.CSV),
				() -> {
					var system = getProductSystem();
					if (system != null) {
						MatrixExportDialog.open(Database.get(), system);
					}
				}));

		// add the experimental matrix image export
		if (FeatureFlag.MATRIX_IMAGE_EXPORT.isEnabled())
			toolBar.add(new MatrixImageExportAction());
		toolBar.add(Actions.onCalculate(() -> {
			var system = getProductSystem();
			if (system == null)
				return;
			CalculationWizard.open(system);
		}));
	}

	@Override
	public void setActivePage(IEditorPart activePage) {
		setActiveActionBars(null, activePage);
		if (activePage instanceof GraphEditor)
				setActiveActionBars(getGraphicalSubActionBars(), activePage);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (graphicalSubActionBars != null) {
			graphicalSubActionBars.dispose();
			graphicalSubActionBars = null;
		}
	}

	/**
	 * Switches the active action bars.
	 */
	private void setActiveActionBars(MultiPageSubActionBars actionBars,
																	 IEditorPart activeEditor) {
		if (activeEditorActionBars != null
				&& activeEditorActionBars != actionBars) {
			activeEditorActionBars.deactivate();
		}
		activeEditorActionBars = actionBars;
		if (activeEditorActionBars != null) {
			activeEditorActionBars.setEditorPart(activeEditor);
			activeEditorActionBars.activate();
		}
	}

	/**
	 * @return Returns the bar manager for the graphical editor.
	 */
	public MultiPageSubActionBars getGraphicalSubActionBars() {
		if (graphicalSubActionBars == null)
			if (getPage() != null && actionBars2 != null)
				graphicalSubActionBars = new MultiPageSubActionBars(getPage(),
							actionBars2,
							new GraphBarContributor(),
							"org.openlca.app.editors.graphical.actions" +
									".GraphActionBarContributor");
		return graphicalSubActionBars;
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
					var db = Database.get();
					var setup = CalculationSetup.of(system);
					var techIndex = TechIndex.of(db, setup);
					var data = MatrixData.of(db, techIndex)
							.withSetup(setup)
							.build();;
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
