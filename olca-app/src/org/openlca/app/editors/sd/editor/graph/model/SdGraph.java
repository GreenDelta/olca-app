package org.openlca.app.editors.sd.editor.graph.model;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.sd.eqn.EvaluationOrder;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.Stock;

import java.util.HashMap;

public class SdGraph implements NotifySupport {

	private final SdModel model;
	private final Notifier notifier = new Notifier();
	private final HashMap<Id, SdVarNode> nodes = new HashMap<>();

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
			var node = new SdVarNode(variable, model);
			node.moveTo(b);
			g.nodes.put(variable.name(), node);
			i++;
		}
		for (var node : g.nodes.values()) {
			g.addLinksOf(node);
		}

		return g;
	}

	private void addLinksOf(SdVarNode node) {
		if (node == null || node.variable() == null) {
			return;
		}
		if (node.variable() instanceof  Stock stock) {
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

	private void link(SdVarNode source, SdVarNode target, boolean isFlow) {
		if (source == null || target == null)
			return;
		var link = new SdVarLink(source, target, isFlow);
		source.sourceLinks().add(link);
		target.targetLinks().add(link);
	}

	private void removeLinksOf(SdVarNode node) {
		if (node == null) return;
		for (var link : node.sourceLinks()) {
			var target = link.target();
			if (target != null) {
				target.targetLinks().remove(link);
			}
		}
		for (var link : node.targetLinks()) {
			var source = link.source();
			if (source != null) {
				source.sourceLinks().remove(link);
			}
		}
		node.sourceLinks().clear();
		node.targetLinks().clear();
	}

	@Override
	public Notifier notifier() {
		return notifier;
	}

	public SdModel model() {
		return model;
	}

	public void add(SdVarNode node) {
		if (node == null || node.variable() == null) return;
		nodes.put(node.variable().name(), node);
		addLinksOf(node);
		notifier.fire();
	}

	public void remove(SdVarNode node) {
		if (node == null || node.variable() == null) return;
		nodes.remove(node.variable().name());
		removeLinksOf(node);
		notifier.fire();
	}
}
