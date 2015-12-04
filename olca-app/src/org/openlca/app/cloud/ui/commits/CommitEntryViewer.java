package org.openlca.app.cloud.ui.commits;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Commit;

class CommitEntryViewer extends AbstractViewer<Commit, TreeViewer> {

	CommitEntryViewer(Composite parent, RepositoryClient client) {
		super(parent, client);
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent, SWT.BORDER);
		viewer.setContentProvider(new ContentProvider(
				(RepositoryClient) viewerParameters[0]));
		viewer.setLabelProvider(getLabelProvider());
		Tree tree = viewer.getTree();
		UI.gridData(tree, true, true);
		return viewer;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new LabelProvider();
	}

}
