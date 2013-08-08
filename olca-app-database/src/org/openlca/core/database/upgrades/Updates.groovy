package org.openlca.core.database.upgrades


import org.openlca.core.database.ConnectionData;

class Updates {

	static void checkAndRun(ConnectionData data) {
		def updater = new Updater(data)
		updater.run()
	}
}
