package org.openlca.app.editors.sd.editor.graph.model;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.sd.eqn.EvaluationOrder;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Rate;
import org.openlca.sd.model.Rect;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.Stock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SdGraph implements NotifySupport {

	private final SdModel model;
	private final Notifier notifier = new Notifier();
	private final HashMap<Id, VarNode> varNodes = new HashMap<>();
	private final List<SystemNode> systemNodes = new ArrayList<>();

	private SdGraph(SdModel model) {
		this.model = model;
	}

	public static SdGraph of(SdModel model) {

		var bounds = new HashMap<Id, Rectangle>();
		model.positions().forEach((id, r) ->
			bounds.put(id, new Rectangle(r.x(), r.y(), r.width(), r.height())));

		var g = new SdGraph(model);
		int i = 0;

		for (var variable : model.vars()) {
			var b = bounds.getOrDefault(
				variable.name(), new Rectangle(10 + i * 20, 10 + i * 20, 100, 50));
			var node = new VarNode(variable, model);
			node.moveTo(b);
			g.varNodes.put(variable.name(), node);
			i++;
		}
		for (var node : g.varNodes.values()) {
			g.addLinksOf(node);
		}

		for (var sys : model.lca().systemBindings()) {
			var node = new SystemNode(sys);
			g.systemNodes.add(node);
		}

		return g;
	}

	private void addLinksOf(VarNode node) {
		if (node == null || node.variable() == null) {
			return;
		}
		if (node.variable() instanceof Stock stock) {
			for (var flowId : stock.inFlows()) {
				link(varNodes.get(flowId), node, true);
			}
			for (var flowId : stock.outFlows()) {
				link(node, varNodes.get(flowId), true);
			}
		}
		var deps = EvaluationOrder.dependenciesOf(node.variable());
		for (var depId : deps) {
			link(varNodes.get(depId), node, false);
		}
	}

	private void link(VarNode source, VarNode target, boolean isFlow) {
		if (source == null || target == null)
			return;
		var link = new VarLink(source, target, isFlow);
		source.addSourceLink(link);
		target.addTargetLink(link);
	}

	@Override
	public Notifier notifier() {
		return notifier;
	}

	public SdModel model() {
		return model;
	}

	public VarNode getNode(Id name) {
		return varNodes.get(name);
	}

	public List<VarNode> nodes() {
		return new ArrayList<>(varNodes.values());
	}

	public List<Object> children() {
		var children = new ArrayList<>();
		children.addAll(varNodes.values());
		children.addAll(systemNodes);
		return children;
	}

	public void add(VarNode node) {
		if (node == null || node.variable() == null) return;
		var v = node.variable();
		varNodes.put(v.name(), node);
		addLinksOf(node);
		addReverseLinksOf(node);
		model.vars().add(v);
		var b = node.bounds();
		model.positions().put(v.name(), new Rect(b.x, b.y, b.width, b.height));
		notifier.fire();
	}

	/// Updates a node after its variable properties (name, equation, stock
	/// flows, etc.) have been changed. The `oldName` is the name the node was
	/// registered under before the update. This re-maps the node and
	/// rebuilds all its links.
	public void update(VarNode node, Id oldName) {
		if (node == null || node.variable() == null) {
			return;
		}
		removeLinksOf(node, false);
		varNodes.remove(oldName);
		var v = node.variable();
		varNodes.put(v.name(), node);
		addLinksOf(node);
		addReverseLinksOf(node);
		var b = node.bounds();
		model.positions().remove(oldName);
		model.positions().put(v.name(), new Rect(b.x, b.y, b.width, b.height));
		notifier.fire();
	}

	public void remove(VarNode node) {
		if (node == null || node.variable() == null) return;
		model.vars().remove(node.variable());
		model.positions().remove(node.variable().name());
		varNodes.remove(node.variable().name());
		removeLinksOf(node, true);
		notifier.fire();
	}

	public List<SystemNode> systemNodes() {
		return systemNodes;
	}

	public void addSystem(SystemNode node) {
		if (node == null) return;
		systemNodes.add(node);
		model.lca().systemBindings().add(node.binding());
		notifier.fire();
	}

	public void removeSystem(SystemNode node) {
		if (node == null) return;
		systemNodes.remove(node);
		model.lca().systemBindings().remove(node.binding());
		notifier.fire();
	}

	/// Creates links from the given node to other nodes that reference it
	/// in their equations or stock flow lists.
	private void addReverseLinksOf(VarNode node) {
		var nodeId = node.variable().name();
		if (nodeId == null)
			return;
		for (var other : varNodes.values()) {
			if (other == node) continue;
			var deps = EvaluationOrder.dependenciesOf(other.variable());
			if (deps.contains(nodeId)) {
				link(node, other, false);
			}
			if (other.variable() instanceof Stock stock) {
				if (stock.inFlows().contains(nodeId)) {
					link(node, other, true);
				}
				if (stock.outFlows().contains(nodeId)) {
					link(other, node, true);
				}
			}
		}
	}

	private void removeLinksOf(VarNode node, boolean withFlows) {
		if (node == null) return;
		for (var link : node.sourceLinks()) {
			if (withFlows) {
				unlinkFlows(link);
			}
			link.target().removeTargetLink(link);
		}
		for (var link : node.targetLinks()) {
			if (withFlows) {
				unlinkFlows(link);
			}
			link.source().removeSourceLink(link);
		}
		node.clearLinks();
	}

	private void unlinkFlows(VarLink link) {
		if (!link.isStockFlow()) return;
		if (link.source().variable() instanceof Stock stock
			&& link.target().variable() instanceof Rate rate) {
			stock.outFlows().remove(rate.name());
		} else if (link.target().variable() instanceof Stock stock
			&& link.source().variable() instanceof Rate rate) {
			stock.inFlows().remove(rate.name());
		}
	}
}
