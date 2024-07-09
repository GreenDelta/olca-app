package org.openlca.app.navigation;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.util.Labels;

/**
 * A class for filtering model elements from a navigation tree via a text
 * filter. The filter directly registers a listener on the text field.
 * The listener executes a refresh of the tree after a delay.
 */
public class ModelTextFilter extends ViewerFilter {
	private static final int DELAY = 300;  // milliseconds
	private static int runnableId = 0;

	private final Text filterText;

	public ModelTextFilter(Text text, TreeViewer viewer) {
		this.filterText = text;
		text.addModifyListener(e -> {
			var display = viewer.getControl().getShell().getDisplay();
			var currentRunnableId = ++runnableId;

			display.timerExec(DELAY, () -> {
				if (currentRunnableId != runnableId)
					return;
				if (!viewer.getTree().isDisposed()) {
					viewer.refresh();
					expand(viewer);
				}
			});
		});
	}

	private void expand(TreeViewer viewer) {
		TreeItem[] items = viewer.getTree().getItems();
		while (items != null && items.length > 0) {
			TreeItem next = items[0];
			next.setExpanded(true);
			for (int i = 1; i < items.length; i++)
				items[i].setExpanded(false);
			items = next.getItems();
			viewer.refresh();
		}
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		String text = filterText.getText();
		if (text == null || text.trim().isEmpty())
			return true;
		text = text.trim().toLowerCase();
		return select((INavigationElement<?>) element, text);
	}

	private boolean select(INavigationElement<?> element, String text) {
		if (element instanceof ModelElement modelElement) {
			String feed = Labels.name(modelElement.getContent())
					.toLowerCase();
			return feed.contains(text);
		}
		for (INavigationElement<?> child : element.getChildren()) {
			if (select(child, text))
				return true;
		}
		return false;
	}
}
