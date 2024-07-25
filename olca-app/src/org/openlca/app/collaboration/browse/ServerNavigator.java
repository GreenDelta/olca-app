package org.openlca.app.collaboration.browse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.openlca.app.collaboration.browse.actions.UnregisterServerAction;
import org.openlca.app.collaboration.browse.elements.EntryElement;
import org.openlca.app.collaboration.browse.elements.IServerNavigationElement;
import org.openlca.app.collaboration.browse.elements.ServerElement;
import org.openlca.app.collaboration.browse.elements.ServerNavigationRoot;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Desktop;
import org.openlca.app.viewers.Selections;
import org.openlca.app.viewers.Viewers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

public class ServerNavigator extends CommonNavigator {

	private static final Logger log = LoggerFactory.getLogger(ServerNavigator.class);
	public static String ID = "views.server.navigation";
	private ServerNavigationRoot root;

	public static void open() {
		IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow()
				.getActivePage();
		if (page == null)
			return;
		try {
			page.showView(ID);
		} catch (PartInitException e) {
			log.error("Error opening server navigator view", e);
		}
	}

	@Override
	protected Object getInitialInput() {
		root = new ServerNavigationRoot();
		return root;
	}

	@Override
	protected CommonViewer createCommonViewer(Composite aParent) {
		var viewer = super.createCommonViewer(aParent);
		viewer.getTree().setBackground(Colors.systemColor(SWT.COLOR_WIDGET_BACKGROUND));
		viewer.getTree().addKeyListener(new RefreshListener(viewer));
		return viewer;
	}

	@Override
	protected void initListeners(TreeViewer viewer) {
		super.initListeners(viewer);
		viewer.setUseHashlookup(true);
		ColumnViewerToolTipSupport.enableFor(viewer);

		viewer.addDoubleClickListener(evt -> {
			var elem = Selections.firstOf(evt);
			if (elem instanceof ServerElement serverElem && !serverElem.isActive()) {
				serverElem.activate();
				viewer.refresh(elem);
				viewer.setExpandedState(elem, true);
			} else if (elem instanceof EntryElement entryElem && entryElem.isDataset()) {
				var url = entryElem.getUrl();
				Desktop.browse(url);
			}
		});

		// bind delete key
		viewer.getTree().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.character == SWT.DEL && event.stateMask == 0) {
					var selection = viewer.getSelection();
					if (selection.isEmpty())
						return;
					List<IServerNavigationElement<?>> elems = Selections.allOf(selection);
					var unregister = new UnregisterServerAction();
					if (unregister.accept(elems)) {
						unregister.run();
					}
				}
			}
		});
	}

	private static ServerNavigator getInstance() {
		var workbench = PlatformUI.getWorkbench();
		if (workbench == null)
			return null;
		var window = workbench.getActiveWorkbenchWindow();
		if (window == null)
			return null;
		var page = window.getActivePage();
		if (page == null)
			return null;
		var part = page.findView(ID);
		if (!(part instanceof ServerNavigator navigator))
			return null;
		return navigator;
	}

	public static void refresh() {
		var instance = getInstance();
		if (instance == null)
			return;
		instance.doRefresh();
	}

	private void doRefresh() {
		var viewer = getCommonViewer();
		if (viewer == null || root == null)
			return;
		if (viewer.getTree().isDisposed())
			return;
		var oldExpansion = viewer.getExpandedElements();
		root.update();
		viewer.refresh();
		setRefreshedExpansion(viewer, oldExpansion);
	}

	private void setRefreshedExpansion(
			CommonViewer viewer, Object[] oldExpansion) {
		if (viewer == null || oldExpansion == null)
			return;
		var newExpanded = new ArrayList<IServerNavigationElement<?>>();
		for (var e : oldExpansion) {
			if (!(e instanceof IServerNavigationElement<?> oldElem))
				continue;
			var newElem = findElement(oldElem.getContent());
			if (newElem != null) {
				newExpanded.add(newElem);
			}
		}
		viewer.setExpandedElements(newExpanded.toArray());
	}

	private IServerNavigationElement<?> findElement(Object content) {
		if (content == null || root == null)
			return null;
		var queue = new ArrayDeque<IServerNavigationElement<?>>();
		queue.add(root);
		while (!queue.isEmpty()) {
			var next = queue.poll();
			if (Objects.equal(next.getContent(), content))
				return next;
			queue.addAll(next.getChildren());
		}
		return null;
	}

	private class RefreshListener implements KeyListener {

		private final CommonViewer viewer;

		private RefreshListener(CommonViewer viewer) {
			this.viewer = viewer;
		}

		@Override
		public void keyReleased(KeyEvent e) {

		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.keyCode == SWT.F5) {
				List<IServerNavigationElement<?>> selection = Viewers.getAllSelected(viewer);
				for (var selected : selection) {
					selected.update();
					viewer.refresh(selected);
				}
			}
		}

	}

}
