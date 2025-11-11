package org.openlca.app.editors.systems;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IEditorPart;
import org.openlca.app.M;
<<<<<<< HEAD
=======
import org.openlca.app.components.FileChooser;
>>>>>>> master
import org.openlca.app.components.graphics.EditorActionBarContributor;
import org.openlca.app.components.graphics.MultiPageSubActionBars;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.actions.GraphBarContributor;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
<<<<<<< HEAD
=======
import org.openlca.core.math.MatrixRowSorter;
import org.openlca.core.matrix.MatrixData;
import org.openlca.core.matrix.index.TechIndex;
import org.openlca.core.model.CalculationSetup;
>>>>>>> master
import org.openlca.core.model.ProductSystem;

/**
 * A special implementation of a
 * <code>MultiPageEditorActionBarContributor</code> to switch between
 * action bar contributions for product system editor pages.
 */
public class ProductSystemToolBar extends EditorActionBarContributor {

	private GraphEditor editor;

	@Override
	public void setActivePage(IEditorPart activePage) {
		if (activePage instanceof GraphEditor graphEditor)
			this.editor = graphEditor;
		super.setActivePage(activePage);
	}

	@Override
	public MultiPageSubActionBars getNewSubActionBars() {
		return new MultiPageSubActionBars(getPage(),
					getActionBars2(),
					new GraphBarContributor(editor),
					"org.openlca.app.editors.graphical.actions" +
							".GraphActionBarContributor");
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

		toolBar.add(Actions.onCalculate(
				() -> CalculationDispatch.call(getProductSystem())));
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
}
