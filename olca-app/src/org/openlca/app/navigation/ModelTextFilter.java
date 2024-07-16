package org.openlca.app.navigation;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.util.Labels;

/**
 * A class for filtering model elements from a navigation tree via a text
 * filter. The filter directly registers a listener on the text field.
 */
public class ModelTextFilter extends ViewerFilter {

	private final Text filterText;
	private final AtomicBoolean instantSearch = new AtomicBoolean(true);

	public ModelTextFilter(Text text, TreeViewer viewer) {
		this.filterText = text;
		text.addModifyListener(e -> {
			if (!instantSearch.get())
				return;
			viewer.refresh();
			expand(viewer);
		});
		text.addTraverseListener(e -> {
			if (e.detail == SWT.TRAVERSE_RETURN) {
				viewer.refresh();
				expand(viewer);
				// avoid that this event bubbles to other controls
				e.detail = SWT.TRAVERSE_NONE;
			}
		});
	}

	public ModelTextFilter withInstantSearch(boolean b) {
		instantSearch.set(b);
		return this;
	}

	public boolean isWithInstantSearch() {
		return instantSearch.get();
	}

	public String getText() {
		return filterText.getText();
	}

	private void expand(TreeViewer viewer) {
		var items = viewer.getTree().getItems();
		while (items != null && items.length > 0) {
			var next = items[0];
			next.setExpanded(true);
			for (int i = 1; i < items.length; i++) {
				items[i].setExpanded(false);
			}
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
		if (element instanceof ModelElement e) {
			var feed = Labels.name(e.getContent()).toLowerCase();
			return feed.contains(text);
		}
		for (var child : element.getChildren()) {
			if (select(child, text))
				return true;
		}
		return false;
	}
}
