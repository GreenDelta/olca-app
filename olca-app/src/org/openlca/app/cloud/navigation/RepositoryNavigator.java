package org.openlca.app.cloud.navigation;

import java.io.File;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.openlca.app.App;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffIndexer;
import org.openlca.app.events.DatabaseEvent;
import org.openlca.app.events.ModelEvent;

import com.google.common.eventbus.Subscribe;
import com.greendelta.cloud.api.RepositoryConfig;
import com.greendelta.cloud.model.data.DatasetIdentifier;

public class RepositoryNavigator extends CommonNavigator {

	public static String ID = "views.repository.navigation";
	private NavigationRoot root;
	private RepositoryConfig config;
	private DiffIndex index;

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		App.getEventBus().register(this);
	}

	@Override
	public void dispose() {
		App.getEventBus().unregister(this);
		super.dispose();
	}

	@Override
	protected Object getInitialInput() {
		root = new NavigationRoot();
		return root;
	}

	private NavigationRoot getRoot() {
		return root;
	}

	public static void refresh() {
		CommonViewer viewer = getNavigationViewer();
		if (viewer == null)
			return;
		RepositoryNavigator instance = getInstance();
		if (instance == null)
			return;
		NavigationRoot root = instance.root;
		if (root == null)
			return;
		root.update();
		viewer.refresh();
		viewer.expandToLevel(2);
	}

	public static CommonViewer getNavigationViewer() {
		RepositoryNavigator instance = getInstance();
		if (instance == null)
			return null;
		return instance.getCommonViewer();
	}

	private static RepositoryNavigator getInstance() {
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
		if (part instanceof RepositoryNavigator)
			return (RepositoryNavigator) part;
		return null;
	}

	public static NavigationRoot getNavigationRoot() {
		NavigationRoot root = null;
		RepositoryNavigator navigator = getInstance();
		if (navigator != null)
			root = navigator.getRoot();
		return root;
	}

	public static RepositoryConfig getConfig() {
		RepositoryNavigator instance = getInstance();
		if (instance == null)
			return null;
		if (instance.config != null)
			return instance.config;
		NavigationRoot root = instance.getRoot();
		if (root == null)
			return null;
		if (root.getChildren().isEmpty())
			root.update();
		if (root.getChildren().isEmpty())
			return null;
		RepositoryElement element = (RepositoryElement) root.getChildren().get(
				0);
		return instance.config = element.getContent();
	}

	public static DiffIndex getDiffIndex() {
		RepositoryNavigator instance = getInstance();
		if (instance == null)
			return null;
		if (instance.index != null)
			return instance.index;
		RepositoryConfig config = getConfig();
		if (config == null)
			return null;
		instance.index = new DiffIndex(new File(config.getDatabase()
				.getFileStorageLocation(), "cloud/" + config.getRepositoryId()));
		return instance.index;
	}

	@Subscribe
	public void handleModelChange(ModelEvent event) {
		if (getConfig() == null)
			return;
		DiffIndexer indexHelper = new DiffIndexer(index);
		DatasetIdentifier identifier = CloudUtil.toIdentifier(event.model,
				event.category);
		switch (event.type) {
		case CREATE:
			indexHelper.indexCreate(identifier);
			break;
		case MODIFY:
			indexHelper.indexModify(identifier);
			break;
		case DELETE:
			indexHelper.indexDelete(identifier);
			break;
		}
		refresh();
	}

	@Subscribe
	public void handleDatabaseState(DatabaseEvent event) {
		if (!event.type.isOneOf(DatabaseEvent.Type.ACTIVATE,
				DatabaseEvent.Type.CLOSE))
			return;
		if (event.type == DatabaseEvent.Type.CLOSE)
			disconnect();
		refresh();
	}

	public static void disconnect() {
		RepositoryNavigator instance = getInstance();
		if (instance.index != null)
			instance.index.close();
		instance.index = null;
		instance.config = null;
	}

}
