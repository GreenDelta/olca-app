package org.openlca.app.editors.systems;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.dialogs.IPageChangedListener;
import org.openlca.app.M;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.graphical.GraphicalEditorInput;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.ProductSystem;

public class ProductSystemEditor extends ModelEditor<ProductSystem> {

	public static String ID = "editors.productsystem";

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
			// Add a page listener to focus the graph on the reference node when it is
			// activated the first time.
			var graphInit = new AtomicReference<IPageChangedListener>();
			IPageChangedListener fn = e -> {
				if (e.getSelectedPage() != graphEditor)
					return;
				var listener = graphInit.get();
				if (listener == null)
					return;
				graphEditor.focusOnReferenceNode();
				removePageChangedListener(listener);
				graphInit.set(null);
			};
			graphInit.set(fn);
			addPageChangedListener(fn);

			addPage(new StatisticsPage(this));
			addCommentPage();
		} catch (Exception e) {
			ErrorReporter.on("failed to add page", e);
		}
	}

}
