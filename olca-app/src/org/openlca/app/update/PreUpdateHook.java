package org.openlca.app.update;


public interface PreUpdateHook {

	void customizeUpdater(Updater updater, VersionInfo newAppVersionInfo);
}
