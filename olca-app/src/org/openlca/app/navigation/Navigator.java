package org.openlca.app.navigation;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.browse.elements.EntryElement;
import org.openlca.app.collaboration.navigation.NavCache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.libraries.LibraryEditor;
import org.openlca.app.editors.sd.editor.SdModelEditor;
import org.openlca.app.navigation.actions.OpenMappingAction;
import org.openlca.app.navigation.actions.db.DbActivateAction;
import org.openlca.app.navigation.actions.scripts.OpenScriptAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.LibraryElement;
import org.openlca.app.navigation.elements.MappingFileElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.app.navigation.elements.NavigationRoot;
import org.openlca.app.navigation.elements.ScriptElement;
import org.openlca.app.navigation.elements.SdModelElement;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Desktop;
import org.openlca.app.viewers.Selections;
import org.openlca.app.viewers.Viewers;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.Descriptor;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

public class Navigator extends CommonNavigator {

	public static final String ID = "views.navigation";

	/// The root of the navigation tree; this is the input of the viewer.
	private final NavigationRoot root = new NavigationRoot();

	@Override
	protected Object getInitialInput() {
		return root;
	}

	@Override
	protected CommonViewer createCommonViewer(Composite parent) {
		var viewer = super.createCommonViewer(parent);
		viewer.getTree().setBackground(
			Colors.systemColor(SWT.COLOR_WIDGET_BACKGROUND));
		viewer.getTree().addKeyListener(new RefreshListener());
		return viewer;
	}

	@Override
	protected void initListeners(TreeViewer viewer) {
		super.initListeners(viewer);
		viewer.setUseHashlookup(true);
		ColumnViewerToolTipSupport.enableFor(viewer);

		viewer.addDoubleClickListener(evt -> {
			switch (Selections.firstOf(evt)) {
				case ModelElement e -> App.open(e.getContent());
				case LibraryElement e -> LibraryEditor.open(e.getContent());
				case MappingFileElement e -> OpenMappingAction.run(e.getContent());
				case EntryElement e when e.isDataset() -> Desktop.browse(e.getUrl());
				case SdModelElement e -> SdModelEditor.open(e.getContent());
				case DatabaseElement e -> {
					var config = e.getContent();
					if (config != null && !Database.isActive(config)) {
						new DbActivateAction(config).run();
					}
				}
				case ScriptElement e -> {
					var file = e.getContent();
					if (file != null && !file.isDirectory()) {
						OpenScriptAction.run(file);
					}
				}
				case null, default -> {
				}
			}
		});

		// bind delete key
		viewer.getTree().addKeyListener(new KeyBinding(viewer));
	}

	/// Refresh the navigation view if it is available.
	public static void refresh() {
		try {
			var navigator = getInstance();
			if (navigator == null)
				return;
			var viewer = navigator.getCommonViewer();
			if (viewer == null)
				return;
			NavCache.refresh();
			if (viewer.getTree().isDisposed())
				return;
			navigator.root.update();
			refreshSafely(viewer, viewer::refresh);
		} catch (Exception e) {
			logRefreshError(e);
		}
	}

	/// Refreshes the content *under* the given element.
	public static void refresh(INavigationElement<?> element) {
		try {
			if (element instanceof ModelTypeElement e) {
				NavCache.refresh(e.getContent());
			} else {
				NavCache.refresh();
			}
			doRefresh(element);
		} catch (Exception e) {
			logRefreshError(e);
		}
	}

	private static void logRefreshError(Exception e) {
		var log = LoggerFactory.getLogger(Navigator.class);
		log.error("failed to refresh the navigation", e);
	}

	private static void doRefresh(INavigationElement<?> element) {
		var viewer = getNavigationViewer();
		if (viewer == null || element == null)
			return;
		if (viewer.getTree().isDisposed())
			return;
		element.update();
		refreshSafely(viewer, () -> viewer.refresh(element));
		updateLabels(viewer, element);

		// clear the category content cache for the respective model type
		var modelType = modelTypeOf(element);
		if (modelType == null)
			return;
		var dbElem = databaseElementOf(element);
		if (dbElem == null)
			return;
		var contentTest = dbElem.categoryContentTest();
		if (contentTest != null) {
			contentTest.clearCacheOf(modelType);
		}
	}

	/// Runs the given refresh of the viewer. With hash lookup enabled, JFace
	/// maintains an internal map from elements to tree items. If this map
	/// contains an entry that points to a disposed tree item, a refresh fails
	/// with a "Widget is disposed" error, e.g. when the viewer restores the
	/// selection after the refresh. In this case, we set the input again, which
	/// clears this map and rebuilds the tree from scratch.
	///
	/// Note that the refresh itself keeps the expansion state: JFace expands the
	/// new child elements that are equal to expanded old ones, and navigation
	/// elements are equal when their content along their path is equal. Only
	/// for the rebuild, we need to take the expanded elements before the
	/// refresh, as the tree is in an undefined state when the refresh failed.
	private static void refreshSafely(CommonViewer viewer, Runnable refresh) {
		var expanded = viewer.getExpandedElements();
		try {
			refresh.run();
		} catch (SWTException e) {
			if (e.code != SWT.ERROR_WIDGET_DISPOSED
				|| viewer.getTree().isDisposed())
				throw e;
			var log = LoggerFactory.getLogger(Navigator.class);
			log.warn("failed to refresh navigation", e);
			rebuildTree(viewer, expanded);
		}
	}

	private static ModelType modelTypeOf(INavigationElement<?> elem) {
		var content = elem.getContent();
		return switch (content) {
			case ModelType t -> t;
			case Descriptor d -> d.type;
			case Category c -> c.modelType;
			case null, default -> null;
		};
	}

	private static DatabaseElement databaseElementOf(INavigationElement<?> elem) {
		var e = elem;
		while (e != null) {
			if (e instanceof DatabaseElement dbe)
				return dbe;
			e = e.getParent();
		}
		return null;
	}

	private static void updateLabels(CommonViewer viewer,
		INavigationElement<?> element) {
		TreeItem item = findItem(viewer, element);
		if (item == null)
			return;
		do {
			viewer.doUpdateItem(item);
			item = item.getParentItem();
		} while (item != null);
	}

	private static TreeItem findItem(CommonViewer viewer,
		INavigationElement<?> element) {
		Stack<TreeItem> items = new Stack<>();
		items.addAll(Arrays.asList(viewer.getTree().getItems()));
		while (!items.empty()) {
			TreeItem next = items.pop();
			if (itemEqualsElement(next, element))
				return next;
			items.addAll(Arrays.asList(next.getItems()));
		}
		return null;
	}

	private static boolean itemEqualsElement(
		TreeItem item, INavigationElement<?> element
	) {
		INavigationElement<?> data = (INavigationElement<?>) item.getData();
		if (data == null)
			return false;
		return Objects.equal(data.getContent(), element.getContent());
	}

	/// Selects the navigation element with the given *content* in the tree.
	public static void select(Object element) {
		if (element == null)
			return;
		Navigator instance = getInstance();
		if (instance == null)
			return;
		INavigationElement<?> navElement = findElement(element);
		if (navElement == null)
			return;
		var viewer = instance.getCommonViewer();
		if (viewer == null || viewer.getTree().isDisposed())
			return;
		if (hasDisposedItems(viewer, navElement)) {
			rebuildTree(viewer, viewer.getExpandedElements());
		}
		instance.selectReveal(new StructuredSelection(navElement));
	}

	private static CommonViewer getNavigationViewer() {
		var instance = getInstance();
		return instance != null
			? instance.getCommonViewer()
			: null;
	}

	/// Returns the instance of the navigation view or NULL if there is no such
	/// instance available.
	public static Navigator getInstance() {
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
		return part instanceof Navigator navi
			? navi
			: null;
	}

	/// Returns the navigation element with the given content if it exists.
	public static INavigationElement<?> findElement(Object content) {
		if (content == null)
			return null;
		var navigator = getInstance();
		if (navigator == null)
			return null;
		var queue = new ArrayDeque<INavigationElement<?>>();
		queue.add(navigator.root);
		while (!queue.isEmpty()) {
			var next = queue.poll();
			if (Objects.equal(next.getContent(), content))
				return next;
			queue.addAll(next.getChildren());
		}
		return null;
	}

	public static void revealFirstChild(INavigationElement<?> navElem) {
		if (navElem == null || navElem.getChildren().isEmpty())
			return;
		var first = navElem.getChildren().getFirst();
		var viewer = getNavigationViewer();
		if (viewer == null || viewer.getTree().isDisposed())
			return;
		if (hasDisposedItems(viewer, first)) {
			rebuildTree(viewer, viewer.getExpandedElements());
		}
		viewer.reveal(first);
	}

	/// Returns `true` if the element map of the viewer contains a disposed tree
	/// item for the given element or one of its parents. With hash lookup,
	/// JFace takes the items for `reveal`, `expand`, `setSelection` etc. from
	/// this map (and not from the tree), starting at the element and walking up
	/// its parents until it finds a mapped item. A disposed item in this chain
	/// then results in a "Widget is disposed" error, also when the preceding
	/// refresh of the tree went fine (a refresh walks the existing tree items,
	/// it does not look up the map entries of elements that are not shown).
	private static boolean hasDisposedItems(
		CommonViewer viewer, INavigationElement<?> element
	) {
		var e = element;
		while (e != null) {
			for (var item : viewer.testFindItems(e)) {
				if (item != null && item.isDisposed())
					return true;
			}
			e = e.getParent();
		}
		return false;
	}

	/// Sets the input of the viewer again, which clears its element map and
	/// rebuilds the tree from scratch. As this collapses the tree, the given
	/// elements are expanded again (those that are still in the tree).
	private static void rebuildTree(CommonViewer viewer, Object[] expanded) {
		var log = LoggerFactory.getLogger(Navigator.class);
		log.warn("navigation tree contains disposed items; rebuild the tree");
		viewer.setInput(viewer.getInput());
		if (expanded != null && expanded.length > 0) {
			viewer.setExpandedElements(expanded);
		}
	}

	public INavigationElement<?> getFirstSelected() {
		var all = getAllSelected();
		return all.isEmpty()
			? null
			: all.getFirst();
	}

	public List<INavigationElement<?>> getAllSelected() {
		return Viewers.getAllSelected(getNavigationViewer());
	}

	/// Collects content from the navigation elements, to skip an element return
	/// null in the unwrap function
	public static <T> Set<T> collect(
		Collection<INavigationElement<?>> elements,
		Predicate<INavigationElement<?>> filter,
		Function<INavigationElement<?>, T> unwrap
	) {
		Set<T> set = new HashSet<>();
		for (INavigationElement<?> element : elements) {
			if (filter != null && !filter.test(element))
				continue;
			T content = unwrap.apply(element);
			if (content != null) {
				set.add(content);
			}
			set.addAll(collect(element.getChildren(), filter, unwrap));
		}
		return set;
	}

	/// Search for the navigation element with the given content in the given
	/// tree. This assumes that the viewer has the `NavigationContentProvider`
	/// assigned. You should use this function instead of the search function of
	/// the full navigation tree if your viewer contains custom navigation trees,
	/// and you want to select elements etc.
	@SuppressWarnings("unchecked")
	public static <T> INavigationElement<T> find(TreeViewer viewer, T content) {
		if (viewer == null || content == null)
			return null;
		var cp = viewer.getContentProvider();
		if (!(cp instanceof NavigationContentProvider provider))
			return null;
		var roots = provider.getElements(viewer.getInput());
		if (roots == null)
			return null;
		var queue = new ArrayDeque<>();
		for (var root : roots) {
			if (root != null) {
				queue.add(root);
			}
		}
		while (!queue.isEmpty()) {
			var obj = queue.poll();
			if (obj instanceof INavigationElement<?> elem) {
				if (Objects.equal(content, elem.getContent()))
					return (INavigationElement<T>) elem;
			}

			if (!provider.hasChildren(obj))
				continue;
			var childs = provider.getChildren(obj);
			if (childs == null)
				continue;
			for (var child : childs) {
				if (child != null) {
					queue.add(child);
				}
			}
		}
		return null;
	}

	private static class RefreshListener implements KeyListener {

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.keyCode == SWT.F5) {
				if (Repository.isConnected()) {
					App.exec(M.UpdatingLocalIndex, Repository.CURRENT.index::reload);
				}
				Navigator.refresh();
			}
		}

	}

}
