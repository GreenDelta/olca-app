package org.openlca.core.database.refdata

import java.sql.Connection;

import org.slf4j.LoggerFactory;

import groovy.json.JsonSlurper
import groovy.sql.Sql

class MappingImport {

	def Sql sql
	private def log = LoggerFactory.getLogger(getClass())

	MappingImport(Connection con) {
		sql = new Sql(con)
	}

	MappingImport(Sql sql) {
		this.sql = sql
	}

	void run() {
		log.debug 'import data mappings'
		def files = [
			"es1_flow_flow_map.json",
			"ilcd_flow_flow_map.json",
			"es2_unit_unit_map.json",
			"es2_unit_property_map.json"
		]
		files.each {
			try {
				importMappingFile(it)
			} catch (def e) {
				log.error "import mapping file $it failed", e
			}
		}
	}

	private void importMappingFile(def file) {
		log.trace 'import mapping file {}', file
		def is = getClass().getResourceAsStream(file)
		def reader = new InputStreamReader(is)
		def parser = new JsonSlurper()
		def list = parser.parse(reader)
		list.each {
			def id = UUID.randomUUID().toString()
			sql.execute """
				insert into tbl_mappings (id, map_type, format, external_key,
				external_name, olca_id, factor) values
				($id, $it.mapType, $it.format, $it.externalKey, $it.externalName,
				$it.olcaId, $it.factor)
			"""
		}
	}
}
