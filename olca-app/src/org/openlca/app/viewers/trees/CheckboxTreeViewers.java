package org.openlca.app.viewers.trees;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

public abstract class CheckboxTreeViewers {

	@SuppressWarnings("unchecked")
	public static <T> CheckboxTreeViewer create(Composite composite,
			TreeCheckStateContentProvider<T> selectionProvider) {
		var viewer = new CheckboxTreeViewer(composite, SWT.VIRTUAL | SWT.MULTI | SWT.BORDER);
		viewer.setUseHashlookup(true);
		viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setContentProvider(selectionProvider);
		viewer.setCheckStateProvider(selectionProvider);
		viewer.addCheckStateListener(event -> {
			selectionProvider.setSelection((T) event.getElement(), event.getChecked());
			viewer.refresh();
			selectionProvider.onCheckStateChanged();
		});
		ColumnViewerToolTipSupport.enableFor(viewer);
		return viewer;
	}

	// We want to avoid a resizing of the import dialog when the user flips to
	// this page. Thus, we set the input of the tree viewer after receiving the
	// first paint event.
	public static <T> void registerInputHandler(Composite composite, CheckboxTreeViewer viewer, Object input,
			Runnable callback) {
		composite.addPaintListener(new PaintListener() {
			private boolean init = false;

			@Override
			public void paintControl(PaintEvent e) {
				if (init)
					return;
				init = true;
				composite.removePaintListener(this);
				if (input == null)
					return;
				viewer.setInput(input);
				if (callback != null) {
					callback.run();
				}
			}
		});
	}

	@SuppressWarnings("unchecked")
	public static void expandGrayed(CheckboxTreeViewer viewer) {
		if (!(viewer.getContentProvider() instanceof TreeCheckStateContentProvider))
			return;
		var selectionProvider = (TreeCheckStateContentProvider<Object>) viewer.getContentProvider();
		for (var element : selectionProvider.getElements(viewer.getInput())) {
			expandIfGrayed(viewer, selectionProvider, element);
		}
	}

	private static void expandIfGrayed(CheckboxTreeViewer viewer,
			TreeCheckStateContentProvider<Object> selectionProvider, Object element) {
		if (selectionProvider.isChecked(element) && selectionProvider.isGrayed(element)) {
			viewer.setExpandedState(element, true);
			for (var child : selectionProvider.childrenOf(element)) {
				expandIfGrayed(viewer, selectionProvider, child);
			}
		}
	}

}
