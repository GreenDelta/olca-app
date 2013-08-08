package org.openlca.core.database.upgrades

import groovy.json.JsonSlurper
import groovy.sql.Sql
import groovy.transform.PackageScope

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Upgrade of the database structure to version 1.2.9.
 * 
 * (1) New table for sets of process groups
 * 
 * (2) Information of table 'tbl_flowinformation' is moved to table 'tbl_flows' (we do not yet delete this table).
 *
 */
@PackageScope
class Upgrade3 implements IUpgrade {

	private Logger log = LoggerFactory.getLogger(getClass())
	final List<String> fromVersions = ['1.2.8']
	final String toVersion = '1.2.9'

	void execute(Sql sql) {
		def util = new UpgradeUtil(sql)
		log.info('Run database upgrade to version {}', toVersion)
		createProcessGroupSetTable(util)
		mergeFlowTables(util, sql)
		try {
			updateLocations(sql)
		} catch (Exception e) {
			log.error("Failed to update locations", e)
		}
	}

	private void createProcessGroupSetTable(UpgradeUtil util) {
		def tableDef = """
			CREATE TABLE tbl_process_group_sets (
				id VARCHAR(36) NOT NULL,
				name VARCHAR(255), 
				groups_blob MEDIUMBLOB,		
				PRIMARY KEY (id)	
			) Engine = MyISAM
		"""
		util.checkTable('tbl_process_group_sets', tableDef)
	}
	
	private void mergeFlowTables(UpgradeUtil util, Sql sql) {
		addFlowColumns(util)	
		copyValues(sql)	
	}
	
	private void addFlowColumns(UpgradeUtil util) {
		log.trace('add new flow columns')
		util.checkColumn('tbl_flows',
			'infrastructure_flow',
			'infrastructure_flow TINYINT(1) default 0')
		util.checkColumn('tbl_flows',
			'cas_number',
			'cas_number VARCHAR(255)')
		util.checkColumn('tbl_flows',
			'formula',
			'formula VARCHAR(255)')
		util.checkColumn('tbl_flows',
			'f_reference_flow_property',
			'f_reference_flow_property VARCHAR(36)')
		util.checkColumn('tbl_flows',
			'f_location',
			'f_location VARCHAR(36)')
	}
	
	private void copyValues(Sql sql) {
		log.trace('merge flow information to flow table')
		long start = System.currentTimeMillis()
		sql.execute """
			update tbl_flows, tbl_flowinformations set
				tbl_flows.infrastructure_flow = tbl_flowinformations.infrastructureflow,
				tbl_flows.cas_number = tbl_flowinformations.casnumber,
				tbl_flows.formula = tbl_flowinformations.formula,
				tbl_flows.f_reference_flow_property = tbl_flowinformations.f_referenceflowproperty,
				tbl_flows.f_location = tbl_flowinformations.f_location
				where tbl_flows.id = tbl_flowinformations.id
		"""
		long time = System.currentTimeMillis() - start 
		log.trace('flow tables merged; took {} msec', time)
		sql.execute('drop table tbl_flowinformations')
	}
	
	private void updateLocations(Sql sql) {
		log.trace('update locations')
		def parser = new JsonSlurper()
		def is = getClass().getResourceAsStream('locations.json')
		def reader = new InputStreamReader(is, 'UTF-8')
		def locations = parser.parse(reader)
		locations.each { updateLocation(it, sql) }
	}
	
	private void updateLocation(def location, Sql sql) {		
		def row = sql.firstRow("select id from tbl_locations where code = ${location.code}")		
		if(row != null) {
			def oldId = row.id
			if(oldId != location.uuid) {
				insertLocation(location, sql)
				replaceLocation(oldId, location, sql)
			}
		} else {
			insertLocation(location, sql)
		}
	}
	
	private void insertLocation(def loc, Sql sql) {
		log.trace('insert location {}', loc.name)
		def stmt = """
			insert into tbl_locations (id, code, name, description, longitude, latitude)
			values (${loc.uuid}, ${loc.code}, ${loc.name}, ${loc.description},
			${loc.longitude}, ${loc.latitude})
		"""
		sql.execute(stmt)
	}
	
	private void replaceLocation(def oldId, def location, Sql sql) {
		log.trace('replace location {} with new version', location.name)
		sql.execute("update tbl_processes set f_location = ${location.uuid} where f_location = $oldId")
		sql.execute("update tbl_flows set f_location = ${location.uuid} where f_location = $oldId")
		sql.execute("delete from tbl_locations where id = $oldId")
	}
}
