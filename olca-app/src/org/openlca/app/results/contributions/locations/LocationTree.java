package org.openlca.app.results.contributions.locations;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.util.trees.TreeClipboard;
import org.openlca.app.util.trees.Trees;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.results.Contribution;

/**
 * Table for showing the result contributions for locations of an analysis
 * result.
 */
class LocationTree {

	private TreeViewer tree;
	private TreeLabel label;

	public LocationTree(Composite parent) {
		UI.gridLayout(parent, 1);
		label = new TreeLabel();
		String[] labels = { M.Location, M.Amount, M.Unit };
		tree = Trees.createViewer(parent, labels, label);
		tree.setContentProvider(new TreeContentProvider());
		Trees.bindColumnWidths(tree.getTree(), 0.4, 0.3, 0.3);
		Action onOpen = Actions.onOpen(this::onOpenElement);
		Actions.bind(tree, onOpen, TreeClipboard.onCopy(tree));
		Trees.onDoubleClick(tree, e -> onOpen.run());
		tree.getTree().getColumns()[1].setAlignment(SWT.RIGHT);
	}

	void setInput(List<Contribution<Location>> contributions, String unit) {
		label.unit = unit;
		tree.setInput(contributions);
	}

	private void onOpenElement() {
		Object obj = Viewers.getFirstSelected(tree);
		if (obj == null)
			return;
		if (obj instanceof Contribution) {
			Contribution<?> c = (Contribution<?>) obj;
			if (c.item instanceof CategorizedDescriptor) {
				App.openEditor((CategorizedDescriptor) c.item);
			} else if (c.item instanceof CategorizedEntity) {
				App.openEditor((CategorizedEntity) c.item);
			}
		}
	}

}
