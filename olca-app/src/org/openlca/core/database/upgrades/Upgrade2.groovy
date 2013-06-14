package org.openlca.core.database.upgrades


import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.sql.Sql;

import groovy.transform.PackageScope;

@PackageScope
class Upgrade2 implements IUpgrade {

	private Logger log = LoggerFactory.getLogger(getClass())
	final List<String> fromVersions = ['1.2.6', '1.2.7']
	final String toVersion = '1.2.8'

	void execute(Sql sql) {
		def util = new UpgradeUtil(sql)
		log.info('Run database upgrade to version {}', toVersion)
		checkMappingTable(util)
		checkCostCategoryTable(util)
		checkCostEntryTable(util)
		checkNewColumns(util)
	}

	private void checkMappingTable(UpgradeUtil util) {
		def tableDef = """
			CREATE TABLE tbl_mappings (
				id VARCHAR(50) NOT NULL,
				map_type VARCHAR(50),
				format VARCHAR(50),
				external_key VARCHAR(255),
				external_name VARCHAR(255),
				olca_id VARCHAR(36),
				factor DOUBLE,
				PRIMARY KEY (id)
			) ENGINE = MYISAM """
		util.checkTable('tbl_mappings', tableDef)
	}

	private void checkCostCategoryTable(UpgradeUtil util) {
		def tableDef = """
			CREATE TABLE tbl_cost_categories (	
				id VARCHAR(36) NOT NULL,
				name VARCHAR(255),
				description TEXT,
				PRIMARY KEY (id)
			) Engine = MyISAM """
		util.checkTable('tbl_cost_categories', tableDef)
	}

	private void checkCostEntryTable(UpgradeUtil util) {
		def tableDef = """
			CREATE TABLE tbl_product_cost_entries (
				id VARCHAR(36) NOT NULL,
				f_process VARCHAR(36),
				f_exchange VARCHAR(36),
				f_cost_category VARCHAR(36),
				amount DOUBLE,	
				PRIMARY KEY (id),
				INDEX (f_process),
				INDEX (f_exchange),
				INDEX (f_cost_category)
			) Engine = MyISAM """
		util.checkTable('tbl_product_cost_entries', tableDef)
	}

	private void checkNewColumns(UpgradeUtil util) {
		util.checkColumn('tbl_exchanges',
				'f_default_provider',
				'f_default_provider VARCHAR(36)')
		util.checkColumn('tbl_lciafactors',
				'uncertainy_type',
				'uncertainy_type VARCHAR(50)')
		util.checkColumn('tbl_lciafactors',
				'uncertainty_parameter_1',
				'uncertainty_parameter_1 DOUBLE')
		util.checkColumn('tbl_lciafactors',
				'uncertainty_parameter_2',
				'uncertainty_parameter_2 DOUBLE')
		util.checkColumn('tbl_lciafactors',
				'uncertainty_parameter_3',
				'uncertainty_parameter_3 DOUBLE')
	}
}
