package org.openlca.core.application;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.openlca.core.application.db.ServerRegistry;
import org.openlca.core.application.plugin.Activator;
import org.openlca.core.application.plugin.Workspace;
import org.openlca.core.application.views.ModelEditorInput;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.IDatabaseServer;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.ui.Editors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

public class App {

	static Logger log = LoggerFactory.getLogger(App.class);
	private static Map<Class<?>, String> editorIds = new HashMap<>();
	private static AppCache cache = new AppCache();
	private static EventBus eventBus = new EventBus();

	private App() {
	}

	/**
	 * Returns the version of the openLCA application. If there is a version
	 * defined in the ini-file (-olcaVersion argument) this is returned.
	 * Otherwise the version of the application bundle is returned.
	 */
	public static String getVersion() {
		String version = CommandArgument.VERSION.getValue();
		if (version != null)
			return version;
		return Activator.getDefault().getBundle().getVersion().toString();
	}

	/**
	 * Returns the absolute path to the installed XUL-Runner. Returns null if no
	 * XUL runner installation could be found.
	 */
	public static String getXulRunnerPath() {
		Location location = Platform.getInstallLocation();
		if (location == null)
			return null;
		try {
			URL url = location.getURL();
			File installDir = new File(url.getFile());
			File xulRunnerDir = new File(installDir, "xulrunner");
			log.trace("search for XULRunner at {}", xulRunnerDir);
			if (xulRunnerDir.exists())
				return xulRunnerDir.getAbsolutePath();
			return null;
		} catch (Exception e) {
			log.error("Error while searching for XUL-Runner", e);
			return null;
		}
	}

	public static AppCache getCache() {
		return cache;
	}

	public static EventBus getEventBus() {
		return eventBus;
	}

	public static void openEditor(IModelComponent model, IDatabase db) {
		BaseDescriptor descriptor = Descriptors.toDescriptor(model);
		openEditor(descriptor, db);
	}

	public static void openEditor(BaseDescriptor modelDescriptor, IDatabase db) {
		if (modelDescriptor == null || db == null) {
			log.error("model or db is null, could not open editor");
			return;
		}
		log.trace("open editor for {} in {}", modelDescriptor, db);
		String editorId = getEditorId(modelDescriptor.getModelType());
		if (editorId == null)
			log.error("could not find editor for model {}", modelDescriptor);
		else {
			ModelEditorInput input = new ModelEditorInput(modelDescriptor, db);
			Editors.open(input, editorId);
		}
	}

	private static String getEditorId(ModelType type) {
		if (type == null || type.getModelClass() == null)
			return null;
		String editorId = editorIds.get(type.getModelClass());
		if (editorId == null) {
			editorId = findEditorId(type.getModelClass());
			editorIds.put(type.getModelClass(), editorId);
		}
		return editorId;
	}

	private static String findEditorId(Class<?> modelClass) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry
				.getConfigurationElementsFor("org.openlca.core.application.editors");
		for (IConfigurationElement elem : elements) {
			String clazz = elem.getAttribute("componentClass");
			if (clazz != null && clazz.equals(modelClass.getCanonicalName()))
				return elem.getAttribute("editorID");
		}
		return null;
	}

	public static void runInUI(String name, Runnable runnable) {
		WrappedUIJob job = new WrappedUIJob(name, runnable);
		job.setUser(true);
		job.schedule();
	}

	/**
	 * Wraps a runnable in a job and executes it using the Eclipse jobs
	 * framework. No UI access is allowed for the runnable.
	 */
	public static void run(String name, Runnable runnable) {
		run(name, runnable, null);
	}

	/**
	 * See {@link App#run(String, Runnable)}. Additionally, this method allows
	 * to give a callback which is executed in the UI thread when the runnable
	 * is finished.
	 */
	public static void run(String name, Runnable runnable, Runnable callback) {
		WrappedJob job = new WrappedJob(name, runnable);
		if (callback != null)
			job.setCallback(callback);
		job.setUser(true);
		job.schedule();
	}

	public static List<IDatabaseServer> getDatabaseServers() {
		return ServerRegistry.getInstance().getServers();
	}

	public static void addDatabaseServer(IDatabaseServer server) {
		ServerRegistry.getInstance().addServer(server);
	}

	public static void removeDatabaseServer(IDatabaseServer server) {
		ServerRegistry.getInstance().removeServer(server);
	}

	public static File getWorkspace() {
		return Workspace.getDir();
	}

}
