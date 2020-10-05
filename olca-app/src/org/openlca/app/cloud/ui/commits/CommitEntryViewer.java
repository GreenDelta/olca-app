package org.openlca.app.cloud.ui.commits;

import java.util.Collection;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.model.data.Commit;

class CommitEntryViewer extends AbstractViewer<Commit, TreeViewer> {

	CommitEntryViewer(Composite parent, RepositoryClient client) {
		super(parent, client);
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = Trees.createViewer(parent, getLabelProvider());
		viewer.setContentProvider(new ContentProvider((RepositoryClient) viewerParameters[0]));
		return viewer;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new LabelProvider();
	}

	@Override
	public void setInput(Collection<Commit> collection) {
		super.setInput(collection);
	}

}
