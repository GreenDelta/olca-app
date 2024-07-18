package org.openlca.app.collaboration.browse;

import java.util.List;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
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

public class ServerNavigator extends CommonNavigator {

	public static String ID = "views.server.navigation";
	private ServerNavigationRoot root;

	@Override
	protected Object getInitialInput() {
		root = new ServerNavigationRoot();
		return root;
	}

	@Override
	protected CommonViewer createCommonViewer(Composite aParent) {
		var viewer = super.createCommonViewer(aParent);
		viewer.getTree().setBackground(Colors.systemColor(SWT.COLOR_WIDGET_BACKGROUND));
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

	public ServerNavigationRoot getRoot() {
		return root;
	}

	private static CommonViewer getNavigationViewer() {
		var instance = getInstance();
		if (instance == null)
			return null;
		return instance.getCommonViewer();
	}

	public static ServerNavigator getInstance() {
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

	private class RefreshListener implements KeyListener {

		@Override
		public void keyReleased(KeyEvent e) {

		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.keyCode == SWT.F5) {
				var viewer = getNavigationViewer();
				List<IServerNavigationElement<?>> selection = Viewers.getAllSelected(viewer);
				for (var selected : selection) {
					selected.update();
					viewer.refresh(selected);
				}
			}
		}

	}

}
