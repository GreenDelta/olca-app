package org.openlca.core.database.upgrades

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.sql.Sql;

class UpgradeUtil {

	private Logger log = LoggerFactory.getLogger(getClass())
	private Sql sql

	UpgradeUtil(Sql sql) {
		this.sql = sql
	}

	/**
	 * Checks if a table with the given name exists in the 
	 * database. If not it is created using the given 
	 * table definition.
	 */
	void checkTable(String tableName, String tableDef) {
		log.trace('Check if table {} exists', tableName)
		if(tableExists(tableName))
			log.trace('table exists')
		else {
			log.info('create table {}', tableName)
			sql.execute(tableDef)
		}
	}

	private boolean tableExists(def tableName) {
		boolean exists = false
		sql.eachRow("SHOW TABLES") {
			if(it[0] == tableName)
				exists = true
		}
		return exists
	}

	/**
	 * Checks if the table with the given name has the given column. If not
	 * it is created using the given column  definition.
	 */
	void checkColumn(String tableName, String columnName, String columnDef) {
		log.trace('Check if column {} exists in {}', columnName, tableName)
		if(columnExists(tableName, columnName))
			log.trace('column exists')
		else {
			log.info('add column {} to {}', columnName, tableName)
			def stmt = 'ALTER TABLE ' + tableName + ' ADD COLUMN ' + columnDef
			sql.execute(stmt)
		}
	}

	private boolean columnExists(String tableName, String columnName) {
		boolean b = false
		def query = 'show columns in ' + tableName
		sql.eachRow(query) {
			if(it.Field == columnName)
				b = true
		}
		return b
	}
}
