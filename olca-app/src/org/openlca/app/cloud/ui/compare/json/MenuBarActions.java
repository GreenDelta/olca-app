package org.openlca.app.cloud.ui.compare.json;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ISelection;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer;
import org.openlca.app.util.Popup;

import com.google.gson.JsonElement;

class MenuBarActions {

	private JsonNode root;
	private List<JsonNode> nodes = new ArrayList<>();
	private IDependencyResolver dependencyResolver;
	private JsonTreeViewer leftTree;
	private JsonTreeViewer rightTree;

	MenuBarActions(JsonNode root, JsonTreeViewer leftTree,
			JsonTreeViewer rightTree, IDependencyResolver dependencyResolver) {
		this.root = root;
		this.leftTree = leftTree;
		this.rightTree = rightTree;
		this.dependencyResolver = dependencyResolver;
		addChildrenToList(root);
	}

	private void addChildrenToList(JsonNode node) {
		for (JsonNode child : node.children) {
			nodes.add(child);
			addChildrenToList(child);
		}
	}

	void copySelection() {
		applySelection(leftTree.getSelection(), false);
	}

	void copyAll() {
		applySelection(root.children, false);
	}

	void resetSelection() {
		applySelection(leftTree.getSelection(), true);
	}

	void resetAll() {
		applySelection(root.children, true);
	}

	void selectNext() {
		int selected = getLastSelected();
		JsonNode node = findNext(selected + 1, nodes.size());
		if (node == null)
			node = findNext(0, selected);
		select(node);
	}

	void selectPrevious() {
		int selected = getLastSelected();
		if (selected == -1)
			selected = nodes.size();
		JsonNode node = findPrevious(selected - 1, -1);
		if (node == null)
			node = findPrevious(nodes.size() - 1, selected);
		select(node);
	}

	private int getLastSelected() {
		List<JsonNode> selection = leftTree.getSelection();
		if (selection == null || selection.isEmpty())
			return -1;
		JsonNode last = selection.get(selection.size() - 1);
		return nodes.indexOf(last);
	}

	private JsonNode findNext(int index, int limit) {
		JsonNode select = null;
		while (select == null && index < limit) {
			JsonNode node = nodes.get(index);
			if (!node.hasEqualValues())
				select = node;
			index++;
		}
		return select;
	}

	private JsonNode findPrevious(int index, int limit) {
		JsonNode select = null;
		while (select == null && index > limit) {
			JsonNode node = nodes.get(index);
			if (!node.hasEqualValues())
				select = node;
			index--;
		}
		return select;
	}

	private void select(JsonNode node) {
		if (node != null)
			leftTree.select(node);
		else
			Popup.info("No more changes found");
	}

	private void applySelection(List<JsonNode> selection, boolean leftToRight) {
		ISelection s = leftTree.getViewer().getSelection();
		for (JsonNode node : selection)
			applyTo(node, leftToRight);
		leftTree.refresh();
		rightTree.refresh();
		leftTree.getViewer().setSelection(s);
	}

	private void applyTo(JsonNode node, boolean leftToRight) {
		if (node.readOnly)
			return;
		if (!leftToRight && node.hasEqualValues())
			return;
		if (leftToRight)
			if (!node.hasEqualValues())
				return;
			else if (!node.hadDifferences())
				return;
		JsonElement element = leftToRight ? node.originalElement
				: node.rightElement;
		node.setValue(element, leftToRight);
		Set<JsonNode> dependent = getDependent(node);
		if (dependent.isEmpty())
			return;
		for (JsonNode d : dependent) {
			applyTo(d, leftToRight);
		}
	}

	private Set<JsonNode> getDependent(JsonNode node) {
		JsonElement parent = node.parent.getElement();
		if (!parent.isJsonObject())
			return new HashSet<>();
		Set<String> dependent = dependencyResolver.resolve(parent, node.property);
		if (dependent == null)
			return new HashSet<>();
		Set<JsonNode> dependencies = new HashSet<>();
		for (JsonNode child : node.parent.children)
			for (String property : dependent)
				if (child.property.equals(property))
					dependencies.add(child);
		return dependencies;
	}

}
