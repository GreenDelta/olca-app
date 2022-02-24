package org.openlca.app.collaboration.viewers.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.collaboration.viewers.json.content.IDependencyResolver;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.util.Popup;

class MenuBarActions {

	private JsonNode root;
	private List<JsonNode> nodes = new ArrayList<>();
	private IDependencyResolver dependencyResolver;
	private JsonViewer leftTree;
	private JsonViewer rightTree;

	MenuBarActions(JsonNode root, JsonViewer leftTree,
			JsonViewer rightTree, IDependencyResolver dependencyResolver) {
		this.root = root;
		this.leftTree = leftTree;
		this.rightTree = rightTree;
		this.dependencyResolver = dependencyResolver;
		addChildrenToList(root);
	}

	private void addChildrenToList(JsonNode node) {
		node.children.forEach(child -> {
			nodes.add(child);
			addChildrenToList(child);
		});
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
		var selected = getLastSelected();
		var node = findNext(selected + 1, nodes.size());
		if (node == null) {
			node = findNext(0, selected);
		}
		select(node);
	}

	void selectPrevious() {
		var selected = getLastSelected();
		if (selected == -1) {
			selected = nodes.size();
		}
		var node = findPrevious(selected - 1, -1);
		if (node == null) {
			node = findPrevious(nodes.size() - 1, selected);
		}
		select(node);
	}

	private int getLastSelected() {
		var selection = leftTree.getSelection();
		if (selection == null || selection.isEmpty())
			return -1;
		var last = selection.get(selection.size() - 1);
		return nodes.indexOf(last);
	}

	private JsonNode findNext(int index, int limit) {
		JsonNode select = null;
		while (select == null && index < limit) {
			var node = nodes.get(index);
			if (!node.hasEqualValues()) {
				select = node;
			}
			index++;
		}
		return select;
	}

	private JsonNode findPrevious(int index, int limit) {
		JsonNode select = null;
		while (select == null && index > limit) {
			var node = nodes.get(index);
			if (!node.hasEqualValues()) {
				select = node;
			}
			index--;
		}
		return select;
	}

	private void select(JsonNode node) {
		if (node != null) {
			leftTree.select(node);
		} else {
			Popup.info("No more changes found");
		}
	}

	private void applySelection(List<JsonNode> selection, boolean leftToRight) {
		var s = leftTree.getViewer().getSelection();
		selection.forEach(node -> applyTo(node, leftToRight));
		leftTree.refresh();
		rightTree.refresh();
		leftTree.getViewer().setSelection(s);
	}

	private void applyTo(JsonNode node, boolean leftToRight) {
		if (node.readOnly)
			return;
		if (!leftToRight && node.hasEqualValues())
			return;
		if (leftToRight) {
			if (!node.hasEqualValues())
				return;
			if (!node.hadDifferences())
				return;
		}
		var element = leftToRight
				? node.original
				: node.right;
		node.setValue(element, leftToRight);
		getDependent(node).forEach(d -> applyTo(d, leftToRight));
	}

	private List<JsonNode> getDependent(JsonNode node) {
		var parent = node.parent.element();
		if (!parent.isJsonObject())
			return Collections.emptyList();
		var dependent = dependencyResolver.resolve(node);
		if (dependent == null)
			return Collections.emptyList();
		return node.parent.children.stream()
				.filter(child -> dependent.contains(child.property))
				.distinct()
				.toList();
	}

}
