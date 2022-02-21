package org.openlca.app.collaboration.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.openlca.app.collaboration.util.WorkspaceDiffs;
import org.openlca.app.collaboration.viewers.diff.CompareViewer;
import org.openlca.app.collaboration.viewers.diff.DiffNode;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.DiffResult;
import org.openlca.app.collaboration.viewers.json.label.Direction;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.util.UI;
import org.openlca.git.model.Commit;
import org.openlca.git.model.Diff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompareView extends ViewPart {

	public final static String ID = "views.collaboration.compare";
	private final static Logger log = LoggerFactory.getLogger(CompareView.class);
	private CompareViewer viewer;
	private DiffNode input;

	public static void clear() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (page == null)
			return;
		for (IViewReference viewRef : page.getViewReferences()) {
			if (!ID.equals(viewRef.getId()))
				continue;
			CompareView view = (CompareView) viewRef.getView(false);
			if (view == null)
				return;
			view.input = null;
			view.viewer.setInput(new ArrayList<>());
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite body = new Composite(parent, SWT.NONE);
		UI.gridLayout(body, 1, 0, 0);
		viewer = new CompareViewer(body);
	}

	public static void update(Commit commit) {
		update(commit, null);
	}

	public static void update(Commit commit, List<INavigationElement<?>> elements) {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (page == null)
				return;
			CompareView view = (CompareView) page.showView(ID);
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
		var isAhead = Repository.get().isAhead(commit);
		viewer.setDirection(isAhead ? Direction.RIGHT_TO_LEFT : Direction.LEFT_TO_RIGHT);
		var head = Repository.get().commits.head();
		var rDiffs = getDiffs(commit, head, isAhead);
		var wsDiffs = new HashMap<String, Diff>();
		for (var diff : WorkspaceDiffs.get(head, elements)) {
			wsDiffs.put(diff.path(), diff);
		}
		var keys = new HashSet<String>();
		keys.addAll(wsDiffs.keySet());
		keys.addAll(rDiffs.keySet());
		var differences = new HashMap<String, DiffResult>();
		for (var key : keys) {
			differences.put(key, new DiffResult(wsDiffs.get(key), rDiffs.get(key)));
		}
		return new DiffNodeBuilder(Database.get()).build(differences.values());
	}

	private Map<String, Diff> getDiffs(Commit commit, Commit head, boolean isAhead) {
		var diffMap = new HashMap<String, Diff>();
		var headId = head != null ? head.id : null;
		var diffs = Repository.get().diffs.find()
				.between(
						isAhead ? headId : commit.id,
						isAhead ? commit.id : headId)
				.all();
		for (var diff : diffs) {
			diffMap.put(diff.path(), diff);
		}
		return diffMap;
	}

	@Override
	public void setFocus() {

	}

}
