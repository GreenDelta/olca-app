package org.openlca.core.database.upgrades;

import java.util.List;

import org.openlca.core.database.Version;

/**
 * The interface for a database upgrade.
 */
interface IUpgrade {

	/**
	 * The start versions that the upgrade accepts.
	 */
	List<String> getFromVersions();

	/**
	 * The final version of the upgrade. The last upgrade should set the version
	 * to the current version of the database (see {@link Version#CURRENT}).
	 */
	String getToVersion();

}
