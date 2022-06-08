package org.openlca.app.collaboration.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.openlca.app.collaboration.util.PathFilters;
import org.openlca.app.collaboration.viewers.diff.CompareViewer;
import org.openlca.app.collaboration.viewers.diff.DiffNode;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.app.collaboration.viewers.json.label.Direction;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.util.UI;
import org.openlca.git.model.Commit;
import org.openlca.git.util.Constants;
import org.openlca.git.util.Diffs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO FIX ME
public class CompareView extends ViewPart {

	public final static String ID = "views.collaboration.compare";
	private final static Logger log = LoggerFactory.getLogger(CompareView.class);
	private CompareViewer viewer;
	private DiffNode input;

	public static void clear() {
		var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (page == null)
			return;
		for (var viewRef : page.getViewReferences()) {
			if (!ID.equals(viewRef.getId()))
				continue;
			var view = (CompareView) viewRef.getView(false);
			if (view == null)
				return;
			view.input = null;
			view.viewer.setInput(new ArrayList<>());
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		var body = new Composite(parent, SWT.NONE);
		UI.gridLayout(body, 1, 0, 0);
		viewer = new CompareViewer(body);
	}

	public static void update(Commit commit) {
		update(commit, null);
	}

	public static void update(Commit commit, List<INavigationElement<?>> elements) {
		try {
			var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (page == null)
				return;
			var view = (CompareView) page.showView(ID);
			view.doUpdate(commit, elements);
		} catch (PartInitException e) {
			log.error("Error compare view", e);
		}
	}

	private void doUpdate(Commit commit, List<INavigationElement<?>> elements) {
		if (!Repository.isConnected() || commit == null) {
			viewer.setInput(new ArrayList<>());
			return;
		}
		input = buildNode(commit, elements);
		viewer.setInput(input != null ? Collections.singleton(input) : new ArrayList<>());
	}

	private DiffNode buildNode(Commit commit, List<INavigationElement<?>> elements) {
		var isAhead = Repository.get().localHistory.isAheadOf(commit, Constants.REMOTE_REF);
		viewer.setDirection(isAhead ? Direction.RIGHT_TO_LEFT : Direction.LEFT_TO_RIGHT);
		var diffs = Diffs.workspace(Repository.get().toConfig(), commit, PathFilters.of(elements)).stream()
				.map(d -> new TriDiff(d, null))
				.toList();
		return new DiffNodeBuilder(Database.get()).build(diffs);
	}

	@Override
	public void setFocus() {

	}

}
