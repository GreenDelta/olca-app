package org.openlca.app.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.openlca.app.db.Database;
import org.openlca.core.database.EntityCache;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
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
		final List<Node> childs = new ArrayList<>();
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

		Search(String param, IDatabase db) {
			this.param = param;
			this.db = db;
			this.cache = EntityCache.create(db);
		}

		Collection<Node> doIt() {
			HashMap<Long, Node> processNodes = new HashMap<>();
			try {
				String query = "SELECT f_owner, f_flow, "
						+ "resulting_amount_formula FROM tbl_exchanges";
				NativeSql.on(db).query(query, r -> {
					String formula = r.getString(3);
					if (!matchesFormula(formula))
						return true;
					Node pNode = getProcessNode(r.getLong(1),
							processNodes);
					pNode.childs.add(forExchangeAmount(
							r.getLong(2), formula));
					return true;
				});
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(Search.class);
				log.error("failed to search in exchange formulas");
			}
			return processNodes.values();
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

		private Node getProcessNode(long id, HashMap<Long, Node> nodes) {
			Node node = nodes.get(id);
			if (node != null)
				return node;
			node = new Node();
			node.context = cache.get(ProcessDescriptor.class, id);
			nodes.put(id, node);
			return node;
		}

		private Node forExchangeAmount(long flowID, String formula) {
			Node node = new Node();
			node.context = cache.get(FlowDescriptor.class, flowID);
			node.type = "exchange formula";
			node.formula = formula;
			return node;
		}

	}
}
