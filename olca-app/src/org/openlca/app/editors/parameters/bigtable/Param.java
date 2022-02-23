package org.openlca.app.editors.parameters.bigtable;

import java.util.Collections;
import java.util.List;

import org.openlca.app.M;
import org.openlca.app.util.Labels;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactCategoryDao;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

import gnu.trove.map.hash.TLongLongHashMap;

/**
 * Stores a parameter object and its owner.
 */
class Param implements Comparable<Param> {

	/**
	 * If null, it is a global parameter.
	 */
	final RootDescriptor owner;

	Parameter parameter;

	boolean evalError;

	private Param(Parameter p) {
		this(p, null);
	}

	private Param(Parameter p, RootDescriptor owner) {
		this.parameter = p;
		this.owner = owner;
	}

	static void fetchAll(IDatabase db, List<Param> params) {
		if (db == null || params == null)
			return;

		// collect the owner relations
		var processes = new ProcessDao(db).descriptorMap();
		var impacts = new ImpactCategoryDao(db).descriptorMap();
		var owners = new TLongLongHashMap();
		try {
			var sql = "select id, f_owner from tbl_parameters";
			NativeSql.on(db).query(sql, r -> {
				var ownerId = r.getLong(2);
				if (r.wasNull() || ownerId == 0)
					return true;
				owners.put(r.getLong(1), ownerId);
				return true;
			});
		} catch (Exception e) {
			var log = LoggerFactory.getLogger(Param.class);
			log.error("Failed to query parameter onwers", e);
		}

		new ParameterDao(db).getAll().forEach(p -> {

			var ownerId = owners.get(p.id);

			// global parameters
			if (p.scope == ParameterScope.GLOBAL
					|| ownerId == 0) {
				params.add(new Param(p));
				return;
			}

			// local parameters
			var owner = p.scope == ParameterScope.IMPACT
				? impacts.get(ownerId)
				: processes.get(ownerId);
			if (owner == null) {
				var log = LoggerFactory.getLogger(Param.class);
				log.error("invalid owner in parameter {}", p);
				return;
			}
			params.add(new Param(p, owner));
		});

		Collections.sort(params);
	}

	@Override
	public int compareTo(Param other) {
		int c = Strings.compare(
			this.parameter.name,
			other.parameter.name);
		if (c != 0)
			return c;

		if (this.owner == null && other.owner == null)
			return 0;
		if (this.owner == null)
			return -1;
		if (other.owner == null)
			return 1;

		return Strings.compare(
			Labels.name(this.owner),
			Labels.name(other.owner));
	}

	boolean matches(String filter, int type) {
		if (type == FilterCombo.ERRORS)
			return evalError;
		if (parameter == null)
			return false;
		if (filter == null)
			return true;
		String f = filter.trim().toLowerCase();
		if (Strings.nullOrEmpty(f))
			return true;

		if (type == FilterCombo.ALL || type == FilterCombo.NAMES) {
			if (parameter.name != null) {
				String n = parameter.name.toLowerCase();
				if (n.contains(f))
					return true;
			}
		}

		if (type == FilterCombo.ALL || type == FilterCombo.SCOPES) {
			String scope = owner != null
				? Labels.name(owner)
				: M.GlobalParameter;
			scope = scope == null ? "" : scope.toLowerCase();
			if (scope.contains(f))
				return true;
		}

		if (type == FilterCombo.ALL || type == FilterCombo.FORMULAS) {
			if (parameter.formula != null) {
				String formula = parameter.formula.toLowerCase();
				if (formula.contains(f)) {
					return true;
				}
			}
		}

		if (type == FilterCombo.ALL || type == FilterCombo.DESCRIPTIONS) {
			if (parameter.description != null) {
				String d = parameter.description.toLowerCase();
				return d.contains(f);
			}
		}

		return false;
	}

	long ownerId() {
		return owner == null ? 0 : owner.id;
	}

	boolean isGlobal() {
		return scope() == ParameterScope.GLOBAL;
	}

	ParameterScope scope() {
		if (owner == null)
			return ParameterScope.GLOBAL;
		if (owner.type == ModelType.PROCESS)
			return ParameterScope.PROCESS;
		if (owner.type == ModelType.IMPACT_CATEGORY)
			return ParameterScope.IMPACT;
		return parameter == null || parameter.scope == null
			? ParameterScope.GLOBAL
			: parameter.scope;
	}

}
