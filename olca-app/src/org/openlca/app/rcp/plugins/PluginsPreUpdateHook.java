package org.openlca.app.rcp.plugins;

import org.openlca.app.update.PreUpdateHook;
import org.openlca.app.update.Updater;
import org.openlca.app.update.VersionInfo;

public class PluginsPreUpdateHook implements PreUpdateHook {

	@Override
	public void customizeUpdater(Updater updater, VersionInfo newAppVersionInfo) {
		if (true) {
			throw new RuntimeException("Operation currently unsupported");
			// currently not supporting dependencies. See
			// pluginsService.installOrUpdate - dependency check and
			// creation of list of dependent plugins: plugin dep chain
			// building.
		}
		// try {
		// log.debug("Beginning to add plugin update requirements to updater.");
		// PluginsService pluginsService = new PluginsService();
		// PluginListWrapper pluginsFromServer = pluginsService
		// .loadPluginsFromServer();
		//
		// PluginListWrapper mergedPluginInfo = pluginsService
		// .mergePluginInfo(pluginsService.getInstalledPlugins(),
		// pluginsFromServer, newAppVersionInfo.getVersion());
		// PluginListWrapper updateablePlugins = PluginsService
		// .retainUpdateablePlugins(mergedPluginInfo);
		// if (updateablePlugins == null
		// || updateablePlugins.getPlugins() == null
		// || updateablePlugins.getPlugins().isEmpty()) {
		// log.debug("No openLCA plugin updates found");
		// return;
		// }
		// List<String> pluginFilesToDeleteBeforeUpdate = pluginsService
		// .determinePluginFilesToDeleteBeforeUpdate(updateablePlugins);
		//
		// File pluginsZip = File.createTempFile("openLCApluginsToUpdate",
		// "tmp");
		//
		// pluginsService.downloadPlugins(updateablePlugins, pluginsZip);
		//
		// updater.getPathsToDelete().addAll(pluginFilesToDeleteBeforeUpdate);
		// updater.getUnzipRequests().add(
		// new UnzipRequest(pluginsService
		// .getDropinsDirEnsuringPresence().getAbsolutePath(),
		// pluginsZip.getAbsolutePath(), 0));
		// } catch (Exception e) {
		// log.error("Preparation of plugin update failed. "
		// + "Plugins not being updated.", e);
		// }

	}
}
