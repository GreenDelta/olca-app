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

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.OpenMappingAction;
import org.openlca.app.navigation.actions.db.DbActivateAction;
import org.openlca.app.navigation.actions.scripts.OpenScriptAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.LibraryElement;
import org.openlca.app.navigation.elements.MappingFileElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.NavigationRoot;
import org.openlca.app.navigation.elements.ScriptElement;
import org.openlca.app.tools.libraries.LibraryInfoPage;
import org.openlca.app.util.Colors;
import org.openlca.app.viewers.Selections;
import org.openlca.app.viewers.Viewers;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

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
				LibraryInfoPage.show(library);
			} else if (elem instanceof MappingFileElement) {
				var mapping = ((MappingFileElement) elem).getContent();
				OpenMappingAction.run(mapping);
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
		CommonViewer viewer = getNavigationViewer();
		NavigationRoot root = getNavigationRoot();
		if (viewer == null || root == null)
			return;
		Object[] oldExpansion = viewer.getExpandedElements();
		root.update();
		viewer.refresh();
		setRefreshedExpansion(viewer, oldExpansion);
	}

	/**
	 * Refreshes the content *under* the given element.
	 */
	public static void refresh(INavigationElement<?> element) {
		CommonViewer viewer = getNavigationViewer();
		if (viewer == null || element == null)
			return;
		element.update();
		Object[] oldExpansion = viewer.getExpandedElements();
		viewer.refresh(element);
		updateLabels(viewer, element);
		if (oldExpansion == null)
			return;
		setRefreshedExpansion(viewer, oldExpansion);
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
	private static void setRefreshedExpansion(CommonViewer viewer,
																						Object[] oldExpansion) {
		List<INavigationElement<?>> newExpanded = new ArrayList<>();
		for (Object expandedElem : oldExpansion) {
			if (!(expandedElem instanceof INavigationElement))
				continue;
			INavigationElement<?> oldElem = (INavigationElement<?>) expandedElem;
			INavigationElement<?> newElem = findElement(oldElem.getContent());
			if (newElem != null)
				newExpanded.add(newElem);
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
		CommonViewer viewer = null;
		Navigator instance = getInstance();
		if (instance != null) {
			viewer = instance.getCommonViewer();
		}
		return viewer;
	}

	/**
	 * Returns the instance of the navigation view or NULL if there is# no such
	 * instance available.
	 */
	public static Navigator getInstance() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null)
			return null;
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		if (window == null)
			return null;
		IWorkbenchPage page = window.getActivePage();
		if (page == null)
			return null;
		IViewPart part = page.findView(ID);
		if (part instanceof Navigator)
			return (Navigator) part;
		return null;
	}

	/**
	 * Returns the root of the navigation tree or NULL if there is no such root
	 * available.
	 */
	public static NavigationRoot getNavigationRoot() {
		NavigationRoot root = null;
		Navigator navigator = getInstance();
		if (navigator != null)
			root = navigator.getRoot();
		return root;
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

	public static Set<CategorizedDescriptor> collectDescriptors(
		Collection<INavigationElement<?>> elements) {
		return collect(elements, e -> {
			if (!(e instanceof ModelElement))
				return null;
			return ((ModelElement) e).getContent();
		});
	}

	/**
	 * Collects content from the navigation elements, to skip an element return
	 * null in the unwrap function
	 */
	public static <T> Set<T> collect(Collection<INavigationElement<?>> elements,
																	 Function<INavigationElement<?>, T> unwrap) {
		Set<T> set = new HashSet<>();
		for (INavigationElement<?> element : elements) {
			T content = unwrap.apply(element);
			if (content != null) {
				set.add(content);
			}
			set.addAll(collect(element.getChildren(), unwrap));
		}
		return set;
	}

	/**
	 * Search for the navigation element with the given content in the given
	 * tree. This assumes that the viewer has the `NavigationContentProvider`
	 * assigned. You should use this function instead of the search function
	 * of the full navigation tree if your viewer contains custom navigation
	 * trees and you want to select elements etc.
	 */
	@SuppressWarnings("unchecked")
	public static <T> INavigationElement<T> find(TreeViewer viewer, T content) {
		if (viewer == null || content == null)
			return null;
		var cp = viewer.getContentProvider();
		if (!(cp instanceof NavigationContentProvider))
			return null;
		var provider = (NavigationContentProvider) cp;
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
			if (obj instanceof INavigationElement) {
				var elem = (INavigationElement<?>) obj;
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
}
