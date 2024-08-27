package org.openlca.app.collaboration.browse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.function.Predicate;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
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
		var viewer = navigator.getCommonViewer();
		if (viewer == null || viewer.getTree().isDisposed() || navigator.root == null)
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
		var oldExpansion = viewer.getExpandedElements();
		root.update();
		viewer.refresh();
		setRefreshedExpansion(oldExpansion);
	}

	public static <T> void refresh(Class<? extends IServerNavigationElement<T>> clazz,
			Predicate<T> fits) {
		var instance = getInstance();
		if (instance == null)
			return;
		var element = instance.findElement(clazz, fits);
		if (element == null)
			return;
		instance.doRefresh(element);
	}

	private void doRefresh(IServerNavigationElement<?> element) {
		var viewer = getCommonViewer();
		element.update();
		var oldExpansion = viewer.getExpandedElements();
		viewer.refresh(element);
		updateLabels(element);
		setRefreshedExpansion(oldExpansion);
	}

	private void updateLabels(IServerNavigationElement<?> element) {
		var viewer = getCommonViewer();
		var item = findItem(element);
		if (item == null)
			return;
		do {
			viewer.doUpdateItem(item);
			item = item.getParentItem();
		} while (item != null);
	}

	private TreeItem findItem(IServerNavigationElement<?> element) {
		var viewer = getCommonViewer();
		var items = new Stack<TreeItem>();
		items.addAll(Arrays.asList(viewer.getTree().getItems()));
		while (!items.empty()) {
			var next = items.pop();
			if (itemEqualsElement(next, element))
				return next;
			items.addAll(Arrays.asList(next.getItems()));
		}
		return null;
	}

	private boolean itemEqualsElement(TreeItem item, IServerNavigationElement<?> element) {
		var data = (IServerNavigationElement<?>) item.getData();
		if (data == null)
			return false;
		return Objects.equal(data.getContent(), element.getContent());
	}

	@SuppressWarnings("unchecked")
	private void setRefreshedExpansion(Object[] oldExpansion) {
		if (oldExpansion == null || oldExpansion.length == 0)
			return;
		var viewer = getCommonViewer();
		var newExpanded = new ArrayList<IServerNavigationElement<?>>();
		for (var e : oldExpansion) {
			if (!(e instanceof IServerNavigationElement<?> oldElem))
				continue;
			var clazz = (Class<IServerNavigationElement<?>>) oldElem.getClass();
			var newElem = findElement(clazz, other -> Objects.equal(other, oldElem.getContent()));
			if (newElem != null) {
				newExpanded.add(newElem);
			}
		}
		viewer.setExpandedElements(newExpanded.toArray());
	}

	@SuppressWarnings("unchecked")
	private <T> IServerNavigationElement<?> findElement(Class<? extends IServerNavigationElement<T>> clazz,
			Predicate<T> fits) {
		var queue = new ArrayDeque<IServerNavigationElement<?>>();
		queue.add(root);
		while (!queue.isEmpty()) {
			var next = queue.poll();
			if (next.getClass().equals(clazz)) {
				if (fits.test((T) next.getContent()))
					return next;
			} else {
				queue.addAll(next.getChildren());
			}
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
