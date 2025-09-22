package org.openlca.app.collaboration.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.viewers.diff.CompareViewer;
import org.openlca.app.collaboration.viewers.diff.DiffNode;
import org.openlca.app.collaboration.viewers.diff.DiffNodeBuilder;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.git.model.Commit;
import org.openlca.git.model.TriDiff;
import org.openlca.git.repo.OlcaRepository;
import org.openlca.git.util.Constants;

public class CompareView extends ViewPart {

	public final static String ID = "views.collaboration.compare";
	private static CompareView instance;
	private CompareViewer viewer;
	private DiffNode input;

	public CompareView() {
		instance = this;
		setTitleImage(Icon.COMPARE_VIEW.get());
	}

	public static void clear() {
		if (instance == null)
			return;
		instance.input = null;
		instance.viewer.setInput(new ArrayList<>());
	}

	@Override
	public void createPartControl(Composite parent) {
		var body = UI.composite(parent);
		UI.gridLayout(body, 1, 0, 0);
		viewer = new CompareViewer(body, Repository.get());
	}

	public static void update(OlcaRepository repo, Commit commit) {
		update(repo, commit, null);
	}

	public static void update(OlcaRepository repo, Commit commit, List<INavigationElement<?>> elements) {
		if (instance == null) {
			var page = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow()
					.getActivePage();
			if (page == null)
				return;
			try {
				page.showView(ID);
			} catch (PartInitException e) {
				ErrorReporter.on("Error opening compare view", e);
				return;
			}
		}
		instance.doUpdate(repo, commit, elements);
	}

	private void doUpdate(OlcaRepository repo, Commit commit, List<INavigationElement<?>> elements) {
		if (repo == null || commit == null) {
			viewer.setRepository(repo);
			viewer.setInput(new ArrayList<>());
			return;
		}
		input = new DiffNodeBuilder(Database.get()).build(getDiffs(repo, commit, elements));
		viewer.setRepository(repo);
		viewer.setInput(input != null ? Collections.singleton(input) : new ArrayList<>());
	}

	private List<TriDiff> getDiffs(OlcaRepository repo, Commit commit, List<INavigationElement<?>> elements) {
		var localHistory = repo.commits.find().refs(Constants.LOCAL_REF).all();
		if (localHistory.contains(commit)) {
			var diffs = repo.diffs.find().commit(commit).withDatabase();
			return diffs.stream()
					.map(local -> new TriDiff(local, null))
					.toList();
		}
		var localCommit = getCommonParent(repo, localHistory, commit);
		var remoteDiffs = repo.diffs.find().commit(localCommit).with(commit);
		var diffs = new ArrayList<TriDiff>();
		var localDiffs = repo.diffs.find().commit(localCommit).withDatabase();
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

	private Commit getCommonParent(OlcaRepository repo, List<Commit> localHistory, Commit commit) {
		if (localHistory.isEmpty())
			return null;
		if (localHistory.contains(commit))
			return commit;
		var other = repo.commits.find().refs(Constants.REMOTE_REF).until(commit.id).all();
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

	@Override
	public void dispose() {
		instance = null;
		super.dispose();
	}

}
