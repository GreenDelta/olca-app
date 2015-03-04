package org.openlca.app.navigation;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.openlca.app.util.Labels;

/**
 * A class for filtering model elements from an navigation tree via a text
 * filter. The filter directly registers a listener on the text field.
 */
public class ModelTextFilter extends ViewerFilter {

	private Text filterText;

	public ModelTextFilter(Text filterText, final TreeViewer viewer) {
		this.filterText = filterText;
		filterText.addModifyListener((e) -> {
			viewer.refresh();
			expand(viewer);
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
		if (element instanceof ModelElement) {
			ModelElement modelElement = (ModelElement) element;
			String feed = Labels.getDisplayName(modelElement.getContent())
					.toLowerCase();
			return feed.contains(text);
		}
		for (INavigationElement<?> child : element.getChildren())
			if (select(child, text))
				return true;
		return false;
	}
}