package org.openlca.app.collaboration.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.openlca.app.collaboration.ui.viewers.diff.CompareViewer;
import org.openlca.app.collaboration.ui.viewers.diff.DiffNode;
import org.openlca.app.collaboration.ui.viewers.diff.DiffNodeBuilder;
import org.openlca.app.collaboration.ui.viewers.diff.DiffResult;
import org.openlca.app.collaboration.util.WorkspaceDiffs;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.util.UI;
import org.openlca.git.model.Commit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompareView extends ViewPart {

	public final static String ID = "views.collaboration.compare";
	private final static Logger log = LoggerFactory.getLogger(CompareView.class);
	private CompareViewer viewer;
	private DiffNode input;
//	private List<INavigationElement<?>> currentSelection;
//	private Commit commit;

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
//			view.commit = null;
//			view.currentSelection = null;
			view.input = null;
			view.viewer.setInput(new ArrayList<>());
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite body = new Composite(parent, SWT.NONE);
		UI.gridLayout(body, 1, 0, 0);
		viewer = new CompareViewer(body);
		// Actions.bind(viewer.getViewer(), new OverwriteAction());
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
//		this.currentSelection = elements;
//		this.commit = commit;
		input = buildNode();
		viewer.setInput(input != null ? Collections.singleton(input) : new ArrayList<>());
	}

	private DiffNode buildNode() {
		var headCommit = Repository.get().commits.head();
		var diffs = WorkspaceDiffs.get(headCommit);
		// TODO also find "remote" diff
		var differences = diffs.stream()
				.map(d -> new DiffResult(d, null))
				.toList();
		return new DiffNodeBuilder(Database.get()).build(differences);
	}

	@Override
	public void setFocus() {

	}

	// TODO
	// private class OverwriteAction extends Action {
	//
	// private Exception error;
	//
	// private OverwriteAction() {
	// setText("Overwrite local changes");
	// }
	//
	// @Override
	// public void run() {
	// // List<DiffNode> selected =
	// // Viewers.getAllSelected(viewer.getViewer());
	// var dialog = new ProgressMonitorDialog(UI.shell());
	// // TODO collect refs from selection
	// try {
	// dialog.run(true, false, new IRunnableWithProgress() {
	//
	// @Override
	// public void run(IProgressMonitor m) throws InvocationTargetException,
	// InterruptedException {
	// // TODO run import
	// }
	// });
	// } catch (Exception e) {
	// error = e;
	// } finally {
	// Navigator.refresh();
	// }
	// if (error != null)
	// MsgBox.error("Error during download", error.getMessage());
	// else {
	// update(commit, currentSelection);
	// }
	// }
	//
	// }

}
