package org.openlca.app.collaboration.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.openlca.app.collaboration.viewers.diff.CompareViewer;
import org.openlca.app.collaboration.viewers.diff.DiffNode;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.viewers.diff.TriDiff;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.util.UI;
import org.openlca.git.model.Commit;
import org.openlca.git.util.Constants;
import org.openlca.git.util.Diffs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		input = new DiffNodeBuilder(Database.get()).build(getDiffs(commit, elements));
		viewer.setInput(input != null ? Collections.singleton(input) : new ArrayList<>());
	}

	private List<TriDiff> getDiffs(Commit commit, List<INavigationElement<?>> elements) {
		var repo = Repository.get();
		var localHistory = repo.commits.find().refs(Constants.LOCAL_REF).all();
		if (localHistory.contains(commit)) {
			var diffs = Diffs.of(repo.git, commit).with(Database.get(), repo.workspaceIds);
			return diffs.stream()
					.map(local -> new TriDiff(local, null))
					.toList();
		}
		var localCommit = getCommonParent(localHistory, commit);
		var remoteDiffs = Diffs.of(repo.git, localCommit).with(commit);
		var diffs = new ArrayList<TriDiff>();
		var localDiffs = Diffs.of(repo.git, localCommit).with(Database.get(), repo.workspaceIds);
		localDiffs.forEach(local -> {
			var remote = remoteDiffs.stream()
					.filter(e -> e.path.equals(local.path))
					.findFirst()
					.orElse(null);
			if (remote != null) {
				remoteDiffs.remove(remote);
			}
			diffs.add(new TriDiff(local, remote));
		});
		remoteDiffs.forEach(remote -> diffs.add(new TriDiff(remote, null)));
		return diffs;
	}

	private Commit getCommonParent(List<Commit> localHistory, Commit commit) {
		if (localHistory.isEmpty())
			return null;
		if (localHistory.contains(commit))
			return commit;
		var other = Repository.get().commits.find().refs(Constants.REMOTE_REF).until(commit.id).all();
		if (other.isEmpty())
			return null;
		var commonHistory = other.stream()
				.filter(localHistory::contains)
				.toList();
		if (commonHistory.isEmpty())
			return null;
		return commonHistory.get(commonHistory.size() - 1);
	}

	@Override
	public void setFocus() {

	}

}
