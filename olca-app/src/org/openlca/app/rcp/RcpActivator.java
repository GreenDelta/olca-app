package org.openlca.app.rcp;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.logging.Console;
import org.openlca.app.logging.LoggerConfig;
import org.openlca.app.preferences.Preferences;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class RcpActivator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "olca-app";

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private static RcpActivator plugin;

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static RcpActivator getDefault() {
		return plugin;
	}

	/**
	 * Returns the file found at the specified path as an input stream
	 *
	 * @param path
	 *            The path to the file to load an input stream for (relative to
	 *            the plugin)
	 * @return The file found at the specified path as an input stream
	 */
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
		super.start(context);
		plugin = this;
		// initialize the openLCA workspace; releases the default location
		// on initialization, thus, it is good to call this once at early
		// as possible
		var workspace = Workspace.root();
		LoggerConfig.setUp();
		log.info("start openLCA {}, install location={}, workspace={}",
				App.getVersion(), App.getInstallLocation(), workspace);
		WindowLayout.initialize();
		log.trace("initialize HTML folder");
		HtmlFolder.initialize(RcpActivator.getDefault().getBundle(),
				"html/base_html.zip");
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
		plugin = null;
		super.stop(context);
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
