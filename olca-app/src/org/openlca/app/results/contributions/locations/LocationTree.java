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
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.results.ContributionItem;

/**
 * Table for showing the result contributions for locations of an analysis
 * result.
 */
class LocationTree {

	private TreeViewer viewer;
	private TreeLabel treeLabel;

	public LocationTree(Composite parent, boolean withMinHeight) {
		UI.gridLayout(parent, 1);
		treeLabel = new TreeLabel();
		String[] labels = { M.Location + "/" + M.Process, M.Amount, M.Unit };
		viewer = Trees.createViewer(parent, labels, treeLabel);
		viewer.setContentProvider(new TreeContentProvider());
		Trees.bindColumnWidths(viewer.getTree(), 0.4, 0.3, 0.3);

		Action onOpen = Actions.onOpen(() -> {
			Object obj = Viewers.getFirstSelected(viewer);
			if (obj == null)
				return;
			if (obj instanceof LocationItem) {
				LocationItem item = (LocationItem) obj;
				if (item.contribution != null) {
					App.openEditor(item.contribution.item);
				}
			}
			if (obj instanceof ContributionItem) {
				ContributionItem<?> item = (ContributionItem<?>) obj;
				if (item.item instanceof CategorizedDescriptor) {
					App.openEditor((CategorizedDescriptor) item.item);
				}
			}
		});
		Actions.bind(viewer, onOpen, TreeClipboard.onCopy(viewer));
		Trees.onDoubleClick(viewer, e -> onOpen.run());
		viewer.getTree().getColumns()[1].setAlignment(SWT.RIGHT);
	}

	void setInput(List<LocationItem> contributions, String unit) {
		treeLabel.unit = unit;
		viewer.setInput(contributions);
	}

}
