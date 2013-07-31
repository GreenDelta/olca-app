package org.openlca.app.update;

import org.openlca.app.update.execution.Updater;

public interface PreUpdateHook {

	void customizeUpdater(Updater updater, VersionInfo newAppVersionInfo);
}
