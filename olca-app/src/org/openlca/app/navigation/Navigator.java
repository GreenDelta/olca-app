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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.jspecify.annotations.Nullable;
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

		// bind key handlers
		viewer.getTree().addKeyListener(new KeyBinding(viewer));
	}

	public List<INavigationElement<?>> getAllSelected() {
		return Viewers.getAllSelected(getCommonViewer());
	}

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

	/// Tries to refresh the entire navigation view.
	public static void refresh() {
		NavCache.refresh();
		withViewer("refresh", (navigator, viewer) -> {
			navigator.root.update();
			DisposalCheck.beforeRefresh(viewer);
			viewer.refresh();
		});
	}


	/// Refreshes the given element: the element reloads its content and children,
	/// and the labels of the element, its descendants, and its parents are
	/// updated.
	public static void refresh(INavigationElement<?> element) {
		if (element == null)
			return;
		if (element instanceof ModelTypeElement e) {
			NavCache.refresh(e.getContent());
		} else {
			NavCache.refresh();
		}

		withViewer("refresh element", (_, viewer) -> {
			clearCategoryContentCache(element);
			element.update();
			DisposalCheck.beforeRefresh(viewer, element);
			viewer.refresh(element);
			updateParentLabels(viewer, element);
		});
	}

	/// The category content cache basically contains whether a category contains
	/// library data. This needs to be updated when a single element is refreshed.
	/// When the complete tree is refreshed, this cache is anyhow newly created.
	private static void clearCategoryContentCache(INavigationElement<?> elem) {
		if (elem == null)
			return;

		var modelType = switch (elem.getContent()) {
			case ModelType t -> t;
			case Descriptor d -> d.type;
			case Category c -> c.modelType;
			case null, default -> null;
		};
		if (modelType == null)
			return;

		var dbElem = databaseElementOf(elem);
		if (dbElem == null)
			return;
		var contentTest = dbElem.categoryContentTest();
		if (contentTest != null) {
			contentTest.clearCacheOf(modelType);
		}
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

	/// Selects the navigation element with the given *content* in the tree.
	public static void select(Object content) {
		if (content == null)
			return;
		withViewer("select", (navigator, viewer) -> {
			var elem = findElement(content);
			if (elem == null)
				return;
			DisposalCheck.beforeReveal(viewer, elem);
			navigator.selectReveal(new StructuredSelection(elem));
		});
	}

	/// Returns the instance of the navigation view or `null` when there is no such
	/// instance available.
	@Nullable
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
	@Nullable
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

	public static void expand(INavigationElement<?> navElem) {
		if (navElem == null || navElem.getChildren().isEmpty())
			return;
		// we can expand the given element by revealing its first child node
		var first = navElem.getChildren().getFirst();
		withViewer("expand", (_, viewer) -> {
			DisposalCheck.beforeReveal(viewer, first);
			viewer.reveal(first);
		});
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
