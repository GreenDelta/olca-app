package org.openlca.app.collaboration.viewers.diff;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.git.repo.OlcaRepository;

public class CompareViewer extends DiffNodeViewer {

	public CompareViewer(Composite parent, OlcaRepository repo) {
		super(parent, repo);
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		var viewer = Trees.createViewer(parent);
		viewer.setLabelProvider(new DiffNodeLabelProvider());
		viewer.setContentProvider(new DiffNodeContentProvider());
		viewer.setComparator(new DiffNodeComparator());
		viewer.addDoubleClickListener(this::onDoubleClick);
		return viewer;
	}

}
