package org.openlca.app.editors.sd.editor.graph.model;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.sd.eqn.EvaluationOrder;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Rate;
import org.openlca.sd.model.Rect;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.Stock;
import org.openlca.sd.model.SystemBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SdGraph implements NotifySupport {

	private final SdModel model;
	private final Notifier notifier = new Notifier();
	private final HashMap<Id, VarNode> nodes = new HashMap<>();
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
			g.nodes.put(variable.name(), node);
			i++;
		}
		for (var node : g.nodes.values()) {
			g.addLinksOf(node);
		}
		g.syncSystemBindings(false);

		return g;
	}

	public static Rectangle defaultSystemBounds(SdModel model, int index) {
		int width = 140;
		int height = 60;
		int right = 40;
		int top = Integer.MAX_VALUE;

		for (var rect : model.positions().values()) {
			if (rect == null)
				continue;
			right = Math.max(right, rect.x() + rect.width());
			top = Math.min(top, rect.y());
		}
		for (var binding : model.lca().systemBindings()) {
			var view = binding.view();
			if (view == null)
				continue;
			right = Math.max(right, view.x() + view.width());
			top = Math.min(top, view.y());
		}

		int x = right + 80;
		int y = top != Integer.MAX_VALUE
			? top + index * (height + 20)
			: 40 + index * (height + 20);
		return new Rectangle(x, y, width, height);
	}

	public void syncSystemBindings() {
		syncSystemBindings(true);
	}

	private void syncSystemBindings(boolean notify) {
		systemNodes.clear();
		var bindings = model.lca().systemBindings();
		for (int i = 0; i < bindings.size(); i++) {
			var binding = bindings.get(i);
			systemNodes.add(new SystemNode(binding, boundsOf(binding, i)));
		}
		if (notify) {
			notifier.fire();
		}
	}

	private Rectangle boundsOf(SystemBinding binding, int index) {
		if (binding != null && binding.view() != null) {
			var view = binding.view();
			return new Rectangle(view.x(), view.y(), view.width(), view.height());
		}
		return defaultSystemBounds(model, index);
	}

	private void addLinksOf(VarNode node) {
		if (node == null || node.variable() == null) {
			return;
		}
		if (node.variable() instanceof Stock stock) {
			for (var flowId : stock.inFlows()) {
				link(nodes.get(flowId), node, true);
			}
			for (var flowId : stock.outFlows()) {
				link(node, nodes.get(flowId), true);
			}
		}
		var deps = EvaluationOrder.dependenciesOf(node.variable());
		for (var depId : deps) {
			link(nodes.get(depId), node, false);
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
		return nodes.get(name);
	}

	public List<VarNode> nodes() {
		return new ArrayList<>(nodes.values());
	}

	public List<Object> children() {
		var children = new ArrayList<>();
		children.addAll(nodes.values());
		children.addAll(systemNodes);
		return children;
	}

	public void add(VarNode node) {
		if (node == null || node.variable() == null) return;
		var v = node.variable();
		nodes.put(v.name(), node);
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
		nodes.remove(oldName);
		var v = node.variable();
		nodes.put(v.name(), node);
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
		nodes.remove(node.variable().name());
		removeLinksOf(node, true);
		notifier.fire();
	}


	/// Creates links from the given node to other nodes that reference it
	/// in their equations or stock flow lists.
	private void addReverseLinksOf(VarNode node) {
		var nodeId = node.variable().name();
		if (nodeId == null)
			return;
		for (var other : nodes.values()) {
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
