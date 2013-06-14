package org.openlca.core.database.upgrades

import groovy.sql.Sql
import groovy.transform.PackageScope

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Upgrade of the database structure to version 1.3.0.
 * 
 * (1) Additional field 'fix' in tbl_cost_categories
 *
 */
@PackageScope
class Upgrade4 implements IUpgrade {

	private Logger log = LoggerFactory.getLogger(getClass())
	final List<String> fromVersions = ['1.2.9']
	final String toVersion = '1.3.0'

	void execute(Sql sql) {
		def util = new UpgradeUtil(sql)
		log.info('Run database upgrade to version {}', toVersion)
		updateCostCategories(sql);
	}

	private void updateCostCategories(Sql sql) {
		sql.execute("ALTER TABLE tbl_cost_categories ADD COLUMN fix BOOLEAN DEFAULT FALSE")
	}
}
