package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
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
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.navigation.actions.db.DbActivateAction;
import org.openlca.app.util.Colors;
import org.openlca.app.util.viewers.Viewers;
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
		CommonViewer viewer = super.createCommonViewer(aParent);
		viewer.getTree().setBackground(Colors.systemColor(SWT.COLOR_WIDGET_BACKGROUND));
		return viewer;
	}

	@Override
	protected void initListeners(TreeViewer viewer) {
		super.initListeners(viewer);
		viewer.setUseHashlookup(true);
		ColumnViewerToolTipSupport.enableFor(viewer);
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				onDoubleClick(event.getSelection());
			}
		});
	}

	private void onDoubleClick(ISelection selection) {
		Object element = Viewers.getFirst(selection);
		if (element instanceof ModelElement) {
			ModelElement e = (ModelElement) element;
			App.openEditor(e.getContent());
		} else if (element instanceof DatabaseElement) {
			DatabaseElement e = (DatabaseElement) element;
			IDatabaseConfiguration config = e.getContent();
			if (config != null && !Database.isActive(config)) {
				new DbActivateAction(config).run();
			}
		}
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
		for (TreeItem item : viewer.getTree().getItems())
			items.add(item);
		while (!items.empty()) {
			TreeItem next = items.pop();
			if (itemEqualsElement(next, element))
				return next;
			for (TreeItem item : next.getItems())
				items.add(item);
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
		NavigationRoot root = getNavigationRoot();
		if (content == null || root == null)
			return null;
		Queue<INavigationElement<?>> queue = new LinkedList<>();
		queue.add(root);
		while (!queue.isEmpty()) {
			INavigationElement<?> next = queue.poll();
			if (Objects.equal(next.getContent(), content))
				return next;
			queue.addAll(next.getChildren());
		}
		return null;
	}

	public INavigationElement<?> getFirstSelected() {
		INavigationElement<?>[] all = getAllSelected();
		return all.length > 0 ? all[0] : null;
	}

	public INavigationElement<?>[] getAllSelected() {
		List<INavigationElement<?>> selection = Viewers
				.getAllSelected(getNavigationViewer());
		return selection.toArray(new INavigationElement[selection.size()]);
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
}
