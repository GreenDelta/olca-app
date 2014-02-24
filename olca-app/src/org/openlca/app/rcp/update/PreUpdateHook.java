package org.openlca.app.rcp.update;


public interface PreUpdateHook {

	void customizeUpdater(Updater updater, VersionInfo newAppVersionInfo);
}
