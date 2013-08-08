package org.openlca.core.database.upgrades

import groovy.sql.Sql
import groovy.transform.PackageScope

import org.openlca.core.database.ConnectionData
import org.slf4j.LoggerFactory

@PackageScope
class Updater {

	private def updates = [
		new Upgrade1(),
		new Upgrade2(),
		new Upgrade3(),
		new Upgrade4()
	]

	private ConnectionData db
	private def log = LoggerFactory.getLogger(getClass())

	Updater(ConnectionData db) {
		this.db = db
	}

	void run() {
		def url = "jdbc:mysql://${db.host}:${db.port}/${db.databaseName}"
		log.info 'check for updates for database {}', url
		try {
			Sql sql = Sql.newInstance(url, db.user, db.password)
			def nextUpdate = null
			while(nextUpdate = getNextUpdate(sql)) {
				nextUpdate.execute sql
				incVersion nextUpdate.toVersion, sql
			}
			log.trace 'no more updates'
		} catch(Throwable t) {
			log.error "Database update failed", t
		}
	}

	private def getNextUpdate(Sql sql) {
		def version = getVersion sql
		def nextUpdate = updates.find { version in it.fromVersions }
		if(nextUpdate)
			updates.remove nextUpdate
		return nextUpdate
	}

	private def getVersion(Sql sql) {
		return sql.firstRow("select no from openlca_version").no
	}

	private def incVersion(def version, Sql sql) {
		sql.execute("update openlca_version set no = $version")
		log.info "updated to version {}", version
	}
}
