package org.openlca.core.database.upgrades;

import org.slf4j.LoggerFactory;

import groovy.sql.Sql;

import groovy.transform.PackageScope;

/**
 * First database update in the 1.2 series: adds pedigree uncertainties to 
 * exchanges.
 */
@PackageScope
class Upgrade1 implements IUpgrade {

	private def log = LoggerFactory.getLogger(getClass())
	final List<String> fromVersions = ['1.2']
	final String toVersion = '1.2.6'

	void execute(Sql sql) {
		log.info "Run database upgrade to version {}", toVersion
		if(!hasColum(sql, 'pedigree_uncertainty'))
			addField(sql, 'pedigree_uncertainty VARCHAR(50)')
		if(!hasColum(sql, 'base_uncertainty'))
			addField(sql, 'base_uncertainty DOUBLE')
	}

	private boolean hasColum(Sql sql, String name) {
		boolean b = false
		def query = "show columns in tbl_exchanges"
		sql.eachRow(query) { row ->
			if(row.Field == name) {
				b = true
			}
		}
		log.trace('field {} exists? -> {}', name, b)
		return b
	}

	private void addField(Sql sql, String fieldDef) {
		log.trace('add field {}', fieldDef)
		def statement = 'ALTER TABLE tbl_exchanges ADD COLUMN ' + fieldDef
		sql.execute(statement)
	}
}
