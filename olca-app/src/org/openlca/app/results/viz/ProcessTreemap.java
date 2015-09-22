package org.openlca.app.results.viz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.app.util.Labels;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.Category;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionResultProvider;

import com.google.gson.Gson;

public class ProcessTreemap {

	private Object selection;
	private ContributionResultProvider<?> result;
	private EntityCache cache;
	private TreeMapNode root;
	private Map<Long, TreeMapNode> categoryNodes = new HashMap<>();

	public static String calculate(ContributionResultProvider<?> result,
			FlowDescriptor flow) {
		ProcessTreemap treemap = new ProcessTreemap(result, flow);
		Gson gson = new Gson();
		return gson.toJson(treemap.root);
	}

	public static String calculate(ContributionResultProvider<?> result,
			ImpactCategoryDescriptor impact) {
		ProcessTreemap treemap = new ProcessTreemap(result, impact);
		Gson gson = new Gson();
		return gson.toJson(treemap.root);
	}

	private ProcessTreemap(ContributionResultProvider<?> result,
			Object selection) {
		this.result = result;
		this.cache = result.cache;
		this.selection = selection;
		calculate();
	}

	private void calculate() {
		root = new TreeMapNode();
		root.name = "Process results";
		root.children = new ArrayList<>();
		for (ProcessDescriptor process : result.getProcessDescriptors()) {
			TreeMapNode processNode = makeProcessNode(process);
			if (process.getCategory() == null)
				root.children.add(processNode);
			else {
				TreeMapNode categoryNode = getCategoryNode(process
						.getCategory());
				categoryNode.children.add(processNode);
			}
		}
	}

	private TreeMapNode getCategoryNode(long categoryId) {
		Category category = cache.get(Category.class, categoryId);
		if (category == null)
			return root;
		return getCategoryNode(category);
	}

	private TreeMapNode getCategoryNode(Category category) {
		TreeMapNode node = categoryNodes.get(category.getId());
		if (node != null)
			return node;
		node = new TreeMapNode();
		node.children = new ArrayList<>();
		node.name = category.getName();
		categoryNodes.put(category.getId(), node);
		if (category.getCategory() == null)
			root.children.add(node);
		else {
			TreeMapNode parent = getCategoryNode(category.getCategory());
			parent.children.add(node);
		}
		return node;
	}

	private TreeMapNode makeProcessNode(ProcessDescriptor process) {
		TreeMapNode node = new TreeMapNode();
		node.name = Labels.getDisplayName(process);
		if (selection instanceof FlowDescriptor)
			node.value = result.getSingleFlowResult(process,
					(FlowDescriptor) selection).value;
		else if (selection instanceof ImpactCategoryDescriptor) {
			node.value = result.getSingleImpactResult(process,
					(ImpactCategoryDescriptor) selection).value;
		}
		return node;
	}

	@SuppressWarnings("unused")
	private class TreeMapNode {
		String name;
		Double value;
		List<TreeMapNode> children;
	}

}
