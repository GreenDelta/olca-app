package org.openlca.app.editors.systems;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.openlca.app.M;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.graphical.GraphFile;
import org.openlca.app.editors.graphical.GraphicalEditorInput;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.ProductSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductSystemEditor extends ModelEditor<ProductSystem> {

	public static String ID = "editors.productsystem";

	private final static Logger log = LoggerFactory.getLogger(
			ProductSystemEditor.class);

	public ProductSystemEditor() {
		super(ProductSystem.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ProductSystemInfoPage(this));
			// addPage(new ProductSystemParameterPage(this));
			addPage(new ParameterPage2(this));

			var descriptor = getEditorInput().getDescriptor();
			GraphicalEditorInput gInput = new GraphicalEditorInput(descriptor);
			var graphEditor = new GraphEditor(this);
			int gIdx = addPage(graphEditor, gInput);
			setPageText(gIdx, M.ModelGraph);
			// Add a page listener to set the graph when it is activated the first
			// time.
			setGraphPageListener(graphEditor);

			addPage(new StatisticsPage(this));
			addCommentPage();
		} catch (Exception e) {
			ErrorReporter.on("failed to add page", e);
		}
	}

	private void setGraphPageListener(GraphEditor graphEditor) {
		var graphInit = new AtomicReference<IPageChangedListener>();
		IPageChangedListener fn = e -> {
			if (e.getSelectedPage() != graphEditor)
				return;
			var listener = graphInit.get();
			if (listener == null)
				return;
			var nodeArray = GraphFile.getLayout(graphEditor, "nodes");
			var stickyNoteArray = GraphFile.getLayout(graphEditor, "sticky-notes");
			var graph = graphEditor.getGraphFactory()
					.createGraph(graphEditor, nodeArray, stickyNoteArray);
			graphEditor.setModel(graph);
			var viewer = (GraphicalViewer) graphEditor
					.getAdapter(GraphicalViewer.class);
			viewer.setContents(graph);

			// Artificially refreshing the ActionBarContributor.
			refreshActionBar();

			removePageChangedListener(listener);
			graphInit.set(null);
		};
		graphInit.set(fn);
		addPageChangedListener(fn);
	}

	/**
	 * This method fixes two problems:
	 *  1. sub-actions were disabled when opening the first model graph of the
	 *  session.
	 *  2. sub-actions in the toolbar were invisible after closing every Product
	 *  System editors and opening a new one.
	 * <p>
	 *  To solve the first issue, the focus is set to the Navigator and back to
	 *  the model graph editor. This operation somehow updates the actions and
	 *  makes them enabled.
	 *  The second issue is solved by locking and unlocking the toolbar (or the
	 *  inverse as it depends on the current status of the toolbar). The first
	 *  execution of the action makes the action visible. The second execution
	 *  reset the toolbar to its initial state.
	 */
	private void refreshActionBar() {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.showView(Navigator.ID);
			setFocus();
		} catch (PartInitException ex) {
			log.error("Error when focusing on the graphical editor.", ex);
		}

		var service = PlatformUI.getWorkbench().getService(IHandlerService.class);
		String commandId = IWorkbenchCommandConstants.WINDOW_LOCK_TOOLBAR;
		try {
			service.executeCommand(commandId, null);
			service.executeCommand(commandId, null);
		} catch (ExecutionException | NotDefinedException | NotEnabledException |
						 NotHandledException e) {
			log.error("Error when (un)locking the tool bar.", e);
		}

	}

}
