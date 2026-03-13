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

		for (var sys : model.lca().systemBindings()) {
			var node = new SystemNode(sys);
			g.systemNodes.add(node);
		}
		g.rebuildLinks();

		return g;
	}

	private void link(VarNode source, SdNode target, LinkType type) {
		if (source == null || target == null) return;
		var link = new VarLink(source, target, type);
		source.addSourceLink(link);
		target.addTargetLink(link);
	}

	private void rebuildLinks() {
		clearLinks();
		addStockLinks();
		addEquationLinks();
		addSystemBindingLinks();
	}

	private void clearLinks() {
		for (var node : varNodes.values()) {
			node.clearLinks();
		}
		for (var node : systemNodes) {
			node.clearLinks();
		}
	}

	private void addStockLinks() {
		for (var node : varNodes.values()) {
			if (!(node.variable() instanceof Stock stock)) {
				continue;
			}
			for (var flowId : stock.inFlows()) {
				link(varNodes.get(flowId), node, LinkType.STOCK_IO);
			}
			for (var flowId : stock.outFlows()) {
				link(node, varNodes.get(flowId), LinkType.STOCK_IO);
			}
		}
	}

	private void addEquationLinks() {
		for (var node : varNodes.values()) {
			var deps = EvaluationOrder.dependenciesOf(node.variable());
			for (var depId : deps) {
				link(varNodes.get(depId), node, LinkType.EQN_LINK);
			}
		}
	}

	private void addSystemBindingLinks() {
		for (var node : systemNodes) {
			var binding = node.binding();
			link(varNodes.get(binding.amountVar()), node, LinkType.SYS_LINK);
			for (var varBinding : binding.varBindings()) {
				if (varBinding == null) continue;
				link(varNodes.get(varBinding.varId()), node, LinkType.SYS_LINK);
			}
		}
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

	public List<SdNode> children() {
		var children = new ArrayList<SdNode>();
		children.addAll(varNodes.values());
		children.addAll(systemNodes);
		return children;
	}

	public void add(VarNode node) {
		if (node == null) return;
		var v = node.variable();
		varNodes.put(v.name(), node);
		model.vars().add(v);
		var b = node.bounds();
		model.positions().put(v.name(), new Rect(b.x, b.y, b.width, b.height));
		rebuildLinks();
		notifier.fire();
	}

	/// Updates a node after its variable properties (name, equation, stock
	/// flows, etc.) have been changed. The `oldName` is the name the node was
	/// registered under before the update. This re-maps the node and
	/// rebuilds all its links.
	public void update(VarNode node, Id oldName) {
		if (node == null) return;
		varNodes.remove(oldName);
		var v = node.variable();
		varNodes.put(v.name(), node);
		var b = node.bounds();
		model.positions().remove(oldName);
		model.positions().put(v.name(), new Rect(b.x, b.y, b.width, b.height));
		rebuildLinks();
		notifier.fire();
	}

	public void remove(VarNode node) {
		if (node == null) return;
		unlinkFlowsOf(node);
		model.vars().remove(node.variable());
		model.positions().remove(node.variable().name());
		varNodes.remove(node.variable().name());
		rebuildLinks();
		notifier.fire();
	}

	public void addSystem(SystemNode node) {
		if (node == null) return;
		systemNodes.add(node);
		model.lca().systemBindings().add(node.binding());
		rebuildLinks();
		notifier.fire();
	}

	public void removeSystem(SystemNode node) {
		if (node == null) return;
		systemNodes.remove(node);
		model.lca().systemBindings().remove(node.binding());
		rebuildLinks();
		notifier.fire();
	}

	public void update(SystemNode node) {
		if (node == null) return;
		rebuildLinks();
		notifier.fire();
	}

	private void unlinkFlowsOf(VarNode node) {
		for (var link : node.sourceLinks()) {
			unlinkFlows(link);
		}
		for (var link : node.targetLinks()) {
			unlinkFlows(link);
		}
	}

	private void unlinkFlows(VarLink link) {
		if (link.type() != LinkType.STOCK_IO
			|| !(link.source() instanceof VarNode source)
			|| !(link.target() instanceof VarNode target)) {
			return;
		}
		if (source.variable() instanceof Stock stock
			&& target.variable() instanceof Rate rate) {
			stock.outFlows().remove(rate.name());
		} else if (target.variable() instanceof Stock stock
			&& source.variable() instanceof Rate rate) {
			stock.inFlows().remove(rate.name());
		}

	}
}
