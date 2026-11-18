package org.openlca.app.navigation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.openlca.app.App;
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
import org.openlca.app.util.ErrorReporter;
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
		viewer.setUseHashlookup(true);
		ColumnViewerToolTipSupport.enableFor(viewer);
		viewer.getTree().setBackground(
			Colors.systemColor(SWT.COLOR_WIDGET_BACKGROUND));
		return viewer;
	}

	@Override
	protected void initListeners(TreeViewer viewer) {
		super.initListeners(viewer);

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

	public List<INavigationElement<?>> getAllSelected() {
		return Viewers.getAllSelected(getCommonViewer());
	}

	/// Runs the given function with the navigator and its viewer if the
	/// navigation view is available. Errors are reported with the given action in
	/// the title of the error dialog.
	private static void withViewer(
		String action, BiConsumer<Navigator, CommonViewer> fn
	) {
		var navigator = getInstance();
		if (navigator == null)
			return;
		var viewer = navigator.getCommonViewer();
		if (viewer == null || viewer.getTree().isDisposed())
			return;
		try {
			fn.accept(navigator, viewer);
		} catch (Exception e) {
			ErrorReporter.on("Navigation action failed: " + action, e);
		}
	}

	/// Refresh the navigation view if it is available.
	public static void refresh() {
		withViewer("refresh", (navigator, viewer) -> {
			NavCache.refresh();
			navigator.root.update();
			refreshSafely(viewer, viewer::refresh);
		});
	}

	/// Refreshes the content *under* the given element.
	public static void refresh(INavigationElement<?> element) {
		if (element == null)
			return;

		// the repository state cache is also used when the navigation view
		// is closed, so we refresh it in any case
		if (element instanceof ModelTypeElement e) {
			NavCache.refresh(e.getContent());
		} else {
			NavCache.refresh();
		}

		withViewer("refresh element", (_, viewer) -> {
			// the library content of the categories is used in the refresh (child
			// elements, labels, filters), so we need to clear it before
			clearCategoryContentCache(element);
			element.update();
			refreshSafely(viewer, () -> viewer.refresh(element));
			updateParentLabels(viewer, element);
		});
	}

	/// Clears the cached library content of the categories of the model type
	/// of the given element.
	private static void clearCategoryContentCache(INavigationElement<?> element) {
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

	private static void updateParentLabels(
		CommonViewer viewer, INavigationElement<?> element
	) {
		var parents = new ArrayList<>();
		var p = element.getParent();
		while (p != null && !(p instanceof NavigationRoot)) {
			parents.add(p);
			p = p.getParent();
		}
		if (!parents.isEmpty()) {
			viewer.update(parents.toArray(), null);
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

	/// Selects the navigation element with the given *content* in the tree.
	public static void select(Object content) {
		if (content == null)
			return;
		withViewer("select", (navigator, viewer) -> {
			var elem = findElement(content);
			if (elem == null)
				return;
			if (hasDisposedItems(viewer, elem)) {
				rebuildTree(viewer, viewer.getExpandedElements());
			}
			navigator.selectReveal(new StructuredSelection(elem));
		});
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
		withViewer("reveal", (_, viewer) -> {
			if (hasDisposedItems(viewer, first)) {
				rebuildTree(viewer, viewer.getExpandedElements());
			}
			viewer.reveal(first);
		});
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
		// this is not an error as we recover from it, but we log it to
		// see when this happens
		var log = LoggerFactory.getLogger(Navigator.class);
		log.warn("navigation tree contains disposed items; rebuild the tree");
		viewer.setInput(viewer.getInput());
		if (expanded != null && expanded.length > 0) {
			viewer.setExpandedElements(expanded);
		}
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
}
