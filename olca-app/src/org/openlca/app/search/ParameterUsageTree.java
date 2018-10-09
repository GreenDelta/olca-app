package org.openlca.app.search;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.openlca.app.db.Database;
import org.openlca.app.util.Labels;
import org.openlca.core.database.EntityCache;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Formula;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ParameterUsageTree {

	static class Node {
		BaseDescriptor context;
		String type;
		String formula;
		Node parent;
		final List<Node> childs = new ArrayList<>();

		void add(Node child) {
			if (child == null)
				return;
			child.parent = this;
			childs.add(child);
		}

		Node root() {
			if (parent == null)
				return this;
			return parent.root();
		}
	}

	final String param;
	final List<Node> nodes = new ArrayList<>();

	ParameterUsageTree(String param) {
		this.param = param;
	}

	static ParameterUsageTree build(String param) {
		ParameterUsageTree tree = new ParameterUsageTree(param);
		if (Strings.nullOrEmpty(param))
			return tree;
		Search search = new Search(param, Database.get());
		tree.nodes.addAll(search.doIt());
		return tree;
	}

	private static class Search {

		private final String param;
		private final IDatabase db;
		private final EntityCache cache;

		private final Map<Class<?>, Map<Long, Node>> contexts = new HashMap<>();

		Search(String param, IDatabase db) {
			this.param = param;
			this.db = db;
			this.cache = EntityCache.create(db);
		}

		List<Node> doIt() {
			exchanges();
			impacts();
			List<Node> roots = new ArrayList<>();
			contexts.forEach((clazz, map) -> {
				roots.addAll(map.values());
			});
			roots.sort((n1, n2) -> {
				return Strings.compare(
						Labels.getDisplayName(n1.context),
						Labels.getDisplayName(n2.context));
			});
			return roots;
		}

		private void exchanges() {
			String sql = "SELECT f_owner, f_flow, "
					+ "resulting_amount_formula FROM tbl_exchanges "
					+ " WHERE resulting_amount_formula IS NOT NULL";
			query(sql, r -> {
				String formula = string(r, 3);
				if (!matchesFormula(formula))
					return;
				Node pNode = context(int64(r, 1), ProcessDescriptor.class);
				pNode.add(flowRef(int64(r, 2), "exchange formula", formula));
			});
		}

		private void impacts() {
			String sql = "SELECT met.id AS method, cat.id AS CATEGORY," +
					"  fac.f_flow AS flow, fac.formula AS FORMULA" +
					"  FROM tbl_impact_factors fac" +
					"  INNER JOIN tbl_impact_categories cat" +
					"  ON fac.f_impact_category = cat.id" +
					"  INNER JOIN tbl_impact_methods met" +
					"  ON cat.f_impact_method = met.id" +
					"  WHERE fac.formula IS NOT NULL";
			query(sql, r -> {
				String formula = string(r, 4);
				if (!matchesFormula(formula))
					return;
				Node metNode = context(int64(r, 1),
						ImpactMethodDescriptor.class);
				Node catNode = child(metNode, int64(r, 2),
						ImpactCategoryDescriptor.class);
				catNode.add(flowRef(int64(r, 3),
						"characterization value", formula));
			});
		}

		private void query(String sql, Consumer<ResultSet> fn) {
			try {
				NativeSql.on(db).query(sql, r -> {
					fn.accept(r);
					return true;
				});
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(Search.class);
				log.error("error in query " + sql, e);
			}
		}

		private long int64(ResultSet r, int column) {
			try {
				return r.getLong(column);
			} catch (Exception e) {
				return 0L;
			}
		}

		private String string(ResultSet r, int column) {
			try {
				return r.getString(column);
			} catch (Exception e) {
				return null;
			}
		}

		private boolean matchesFormula(String formula) {
			if (formula == null)
				return false;
			Set<String> vars = Formula.getVariables(formula);
			for (String var : vars) {
				if (var.equalsIgnoreCase(param))
					return true;
			}
			return false;
		}

		private Node context(long id, Class<? extends BaseDescriptor> clazz) {
			Map<Long, Node> nodes = contexts.get(clazz);
			if (nodes == null) {
				nodes = new HashMap<>();
				contexts.put(clazz, nodes);
			}
			Node node = nodes.get(id);
			if (node != null)
				return node;
			node = new Node();
			node.context = cache.get(clazz, id);
			nodes.put(id, node);
			return node;
		}

		private Node child(Node root, long id, Class<? extends BaseDescriptor> clazz) {
			for (Node c : root.childs) {
				if (c.context != null && c.context.getId() == id)
					return c;
			}
			Node c = new Node();
			c.context = cache.get(clazz, id);
			root.add(c);
			return c;
		}

		private Node flowRef(long flowID, String type, String formula) {
			Node node = new Node();
			node.context = cache.get(FlowDescriptor.class, flowID);
			node.type = type;
			node.formula = formula;
			return node;
		}
	}
}
