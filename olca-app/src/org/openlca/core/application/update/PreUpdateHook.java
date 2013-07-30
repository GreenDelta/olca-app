package org.openlca.core.application.update;

import org.openlca.app.updater.Updater;

public interface PreUpdateHook {

	void customizeUpdater(Updater updater, VersionInfo newAppVersionInfo);
}
