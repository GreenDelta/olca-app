package org.openlca.app.editors.systems;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.openlca.app.M;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.graphical.GraphFile;
import org.openlca.app.editors.graphical.GraphicalEditorInput;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.ProductSystem;

import static org.openlca.app.tools.graphics.EditorActionBarContributor.refreshActionBar;

public class ProductSystemEditor extends ModelEditor<ProductSystem> {

	public static String ID = "editors.productsystem";
	private GraphEditor graphEditor;

	public ProductSystemEditor() {
		super(ProductSystem.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ProductSystemInfoPage(this));
			addPage(new ParameterPage(this));
			addGraphPage();
			addPage(new StatisticsPage(this));
			addExtensionPages();
		} catch (Exception e) {
			ErrorReporter.on("failed to add page", e);
		}
	}

	private void addGraphPage() throws PartInitException {
		var descriptor = getEditorInput().getDescriptor();
		GraphicalEditorInput gInput = new GraphicalEditorInput(descriptor);
		graphEditor = new GraphEditor(this);
		int gIdx = addPage(graphEditor, gInput);
		setPageText(gIdx, M.ModelGraph);
		// Add a page listener to set the graph when it is activated the first
		// time.
		setGraphPageListener(graphEditor);
		// Add a part listener to save the graph layout when the editor is closed.
		var page = Editors.getActivePage();
		if (page != null)
			page.addPartListener(new IPartListener2() {
				@Override
				public void partClosed(IWorkbenchPartReference partRef) {
					IPartListener2.super.partClosed(partRef);
					if (partRef.getId().equals(ID)
							&& partRef.getPage().getDirtyEditors().length == 0) {
						GraphFile.save(graphEditor);
					}
				}
			});
	}

	private void setGraphPageListener(GraphEditor graphEditor) {
		var graphInit = new AtomicReference<IPageChangedListener>();
		IPageChangedListener fn = e -> {
			if (e.getSelectedPage() != graphEditor)
				return;
			var listener = graphInit.get();
			if (listener == null)
				return;

			graphEditor.onFirstActivation();

			// Artificially refreshing the ActionBarContributor.
			refreshActionBar(this);

			removePageChangedListener(listener);
			graphInit.set(null);
		};
		graphInit.set(fn);
		addPageChangedListener(fn);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (graphEditor != null) {
			graphEditor.doSave();
			GraphFile.save(graphEditor);
		}
		super.doSave(monitor);
	}

}
