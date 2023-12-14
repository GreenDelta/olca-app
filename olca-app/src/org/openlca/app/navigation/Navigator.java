package org.openlca.app.navigation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.openlca.app.App;
import org.openlca.app.collaboration.navigation.NavRoot;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.DeleteMappingAction;
import org.openlca.app.navigation.actions.DeleteModelAction;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.actions.OpenMappingAction;
import org.openlca.app.navigation.actions.db.DbActivateAction;
import org.openlca.app.navigation.actions.db.DbDeleteAction;
import org.openlca.app.navigation.actions.libraries.DeleteLibraryAction;
import org.openlca.app.navigation.actions.scripts.DeleteScriptAction;
import org.openlca.app.navigation.actions.scripts.OpenScriptAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.LibraryElement;
import org.openlca.app.navigation.elements.MappingFileElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.NavigationRoot;
import org.openlca.app.navigation.elements.ScriptElement;
import org.openlca.app.editors.libraries.LibraryEditor;
import org.openlca.app.util.Colors;
import org.openlca.app.viewers.Selections;
import org.openlca.app.viewers.Viewers;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.Descriptor;

import com.google.common.base.Objects;

public class Navigator extends CommonNavigator {

	public static String ID = "views.navigation";
	private NavigationRoot root;

	@Override
	protected Object getInitialInput() {
		root = new NavigationRoot();
		return root;
	}

	@Override
	protected CommonViewer createCommonViewer(Composite aParent) {
		var viewer = super.createCommonViewer(aParent);
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
			var elem = Selections.firstOf(evt);
			if (elem instanceof ModelElement) {
				var model = ((ModelElement) elem).getContent();
				App.open(model);
			} else if (elem instanceof DatabaseElement) {
				var config = ((DatabaseElement) elem).getContent();
				if (config != null && !Database.isActive(config)) {
					new DbActivateAction(config).run();
				}
			} else if (elem instanceof ScriptElement) {
				var file = ((ScriptElement) elem).getContent();
				if (file.isDirectory())
					return;
				OpenScriptAction.run(file);
			} else if (elem instanceof LibraryElement) {
				var library = ((LibraryElement) elem).getContent();
				LibraryEditor.open(library);
			} else if (elem instanceof MappingFileElement) {
				var mapping = ((MappingFileElement) elem).getContent();
				OpenMappingAction.run(mapping);
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
					List<INavigationElement<?>> elems = Selections.allOf(selection);
					Stream.of(
							new DbDeleteAction(),
							new DeleteLibraryAction(),
							new DeleteModelAction(),
							new DeleteMappingAction(),
							new DeleteScriptAction())
							.filter((INavigationAction a) -> a.accept(elems))
							.findFirst()
							.ifPresent(INavigationAction::run);
				}
			}
		});
	}

	/**
	 * Get the root of the navigation tree.
	 */
	public NavigationRoot getRoot() {
		return root;
	}

	/**
	 * Refresh the navigation view if it is available.
	 */
	public static void refresh() {
		var viewer = getNavigationViewer();
		var root = getNavigationRoot();
		if (viewer == null || root == null)
			return;
		NavRoot.refresh(() -> {
			if (viewer.getTree().isDisposed())
				return;
			var oldExpansion = viewer.getExpandedElements();
			root.update();
			viewer.refresh();
			setRefreshedExpansion(viewer, oldExpansion);
		});
	}

	/**
	 * Refreshes the content *under* the given element.
	 */
	public static void refresh(INavigationElement<?> element) {
		var viewer = getNavigationViewer();
		if (viewer == null || element == null)
			return;
		NavRoot.refresh(() -> {
			element.update();
			Object[] oldExpansion = viewer.getExpandedElements();
			viewer.refresh(element);
			updateLabels(viewer, element);
			setRefreshedExpansion(viewer, oldExpansion);

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
		});
	}

	private static ModelType modelTypeOf(INavigationElement<?> elem) {
		var content = elem.getContent();
		if (content == null)
			return null;
		if (content instanceof ModelType t)
			return t;
		if (content instanceof Descriptor d)
			return d.type;
		if (content instanceof Category c)
			return c.modelType;
		return null;
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

	private static boolean itemEqualsElement(TreeItem item,
			INavigationElement<?> element) {
		INavigationElement<?> data = (INavigationElement<?>) item.getData();
		if (data == null)
			return false;
		return Objects.equal(data.getContent(), element.getContent());
	}

	/**
	 * Expands the elements in the viewer that have the same content as in the
	 * elements of the <code>oldExpansion</code> array.
	 */
	private static void setRefreshedExpansion(
			CommonViewer viewer, Object[] oldExpansion) {
		if (viewer == null || oldExpansion == null)
			return;
		var newExpanded = new ArrayList<INavigationElement<?>>();
		for (var e : oldExpansion) {
			if (!(e instanceof INavigationElement<?> oldElem))
				continue;
			var newElem = findElement(oldElem.getContent());
			if (newElem != null) {
				newExpanded.add(newElem);
			}
		}
		viewer.setExpandedElements(newExpanded.toArray());
	}

	/**
	 * Selects the navigation element with the given *content* in the tree.
	 */
	public static void select(Object element) {
		if (element == null)
			return;
		Navigator instance = getInstance();
		if (instance == null)
			return;
		INavigationElement<?> navElement = findElement(element);
		if (navElement == null)
			return;
		instance.selectReveal(new StructuredSelection(navElement));
	}

	private static CommonViewer getNavigationViewer() {
		var instance = getInstance();
		return instance != null
				? instance.getCommonViewer()
				: null;
	}

	/**
	 * Returns the instance of the navigation view or NULL if there is# no such
	 * instance available.
	 */
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

	/**
	 * Returns the root of the navigation tree or NULL if there is no such root
	 * available.
	 */
	public static NavigationRoot getNavigationRoot() {
		var navigator = getInstance();
		return navigator != null
				? navigator.getRoot()
				: null;
	}

	/**
	 * Returns the navigation element with the given content if it exists.
	 */
	public static INavigationElement<?> findElement(Object content) {
		var root = getNavigationRoot();
		if (content == null || root == null)
			return null;
		var queue = new ArrayDeque<INavigationElement<?>>();
		queue.add(root);
		while (!queue.isEmpty()) {
			var next = queue.poll();
			if (Objects.equal(next.getContent(), content))
				return next;
			queue.addAll(next.getChildren());
		}
		return null;
	}

	public INavigationElement<?> getFirstSelected() {
		var all = getAllSelected();
		return all.isEmpty()
				? null
				: all.get(0);
	}

	public List<INavigationElement<?>> getAllSelected() {
		return Viewers.getAllSelected(getNavigationViewer());
	}

	/**
	 * Collects content from the navigation elements, to skip an element return
	 * null in the unwrap function
	 */
	public static <T> Set<T> collect(Collection<INavigationElement<?>> elements,
			Predicate<INavigationElement<?>> filter,
			Function<INavigationElement<?>, T> unwrap) {
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

	/**
	 * Search for the navigation element with the given content in the given
	 * tree. This assumes that the viewer has the `NavigationContentProvider`
	 * assigned. You should use this function instead of the search function of
	 * the full navigation tree if your viewer contains custom navigation trees
	 * and you want to select elements etc.
	 */
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

	private class RefreshListener implements KeyListener {

		@Override
		public void keyReleased(KeyEvent e) {

		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.keyCode == SWT.F5) {
				for (var elem : getAllSelected()) {
					refresh(elem);
				}
			}
		}

	}

}
