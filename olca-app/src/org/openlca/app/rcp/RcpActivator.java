package org.openlca.app.rcp;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.logging.Console;
import org.openlca.app.logging.LoggerConfig;
import org.openlca.app.preferences.AppConfig;
import org.openlca.app.preferences.Preferences;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The activator class controls the plug-in life cycle.
///
/// This is intentionally a plain [BundleActivator] and not an
/// `AbstractUIPlugin`: loading a sub-class of `AbstractUIPlugin` would activate
/// the workbench bundle before our `start` method is called, which initializes
/// the workbench messages and the text orientation with the language of the
/// `openLCA.ini` file. Thus, the user's language from the [AppConfig] must be
/// applied before any workbench class is loaded.
public class RcpActivator implements BundleActivator {

	public static final String PLUGIN_ID = "olca-app";

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private static RcpActivator plugin;

	private Bundle bundle;
	private ScopedPreferenceStore preferenceStore;

	/// Returns the shared instance.
	public static RcpActivator getDefault() {
		return plugin;
	}

	/// Returns the file found at the specified path, relative to the plugin, as
	/// an input stream, or `null` if it could not be opened.
	public static InputStream getStream(String path) {
		try {
			return FileLocator.openStream(
					plugin.getBundle(), new Path(path), false);
		} catch (final IOException e) {
			return null;
		}
	}

	@Override
	public void start(BundleContext context) throws Exception {
		// This must be the first call: it sets the user's language before any
		// message class (NLS) is initialized. The plugin.xml labels are
		// translated lazily as the extension registry runs in multi-language
		// mode (-Declipse.registry.MultiLanguage=true in openLCA.ini).
		AppConfig.read().applyOnStartup();

		bundle = context.getBundle();
		plugin = this;

		// initialize the openLCA workspace; releases the default location on
		// initialization, thus, it is good to call this once as early as possible
		var workspace = Workspace.root();

		LoggerConfig.setUp();
		log.info("start openLCA {}, install location={}, workspace={}",
				App.getVersion(), App.getInstallLocation(), workspace);
		WindowLayout.initialize();
		log.trace("initialize HTML folder");
		HtmlFolder.initialize(bundle, "html/base_html.zip");
		SslCertificates.load();
		Preferences.init();
		App.getSolver();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		log.trace("Stop bundle {}", PLUGIN_ID);
		Console.dispose();
		try {
			log.info("close database");
			Database.close();
		} catch (Exception e) {
			log.error("Failed to close database", e);
		}
		savePreferenceStore();
		plugin = null;
	}

	public Bundle getBundle() {
		return bundle;
	}

	/// Returns the preference store of this plugin; the same as provided by
	/// `AbstractUIPlugin` before.
	public synchronized IPreferenceStore getPreferenceStore() {
		if (preferenceStore == null) {
			preferenceStore = new ScopedPreferenceStore(
					InstanceScope.INSTANCE, PLUGIN_ID);
		}
		return preferenceStore;
	}

	private synchronized void savePreferenceStore() {
		if (preferenceStore == null || !preferenceStore.needsSaving())
			return;
		try {
			preferenceStore.save();
		} catch (IOException e) {
			log.error("failed to save preferences", e);
		}
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return ResourceLocator.imageDescriptorFromBundle(PLUGIN_ID, path)
				.orElse(null);
	}
}
