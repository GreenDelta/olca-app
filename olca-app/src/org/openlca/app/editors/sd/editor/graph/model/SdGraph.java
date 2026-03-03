package org.openlca.app.editors.sd.editor.graph.model;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.sd.eqn.EvaluationOrder;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.Stock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class SdGraph {

	private final SdModel model;
	private final List<SdVarNode> nodes = new ArrayList<>();
	private final List<Runnable> listeners = new ArrayList<>();

	private SdGraph(SdModel model) {
		this.model = model;
	}

	public static SdGraph of(SdModel model) {

		var bounds = new HashMap<Id, Rectangle>();
		model.positions().forEach((id, r) ->
			bounds.put(id, new Rectangle(r.x(), r.y(), r.width(), r.height())));

		var g = new SdGraph(model);
		int i = 0;
		var modelMap = new HashMap<Id, SdVarNode>();
		for (var variable : model.vars()) {
			var b = bounds.getOrDefault(
				variable.name(), new Rectangle(10 + i * 20, 10 + i * 20, 100, 50));
			var m = new SdVarNode(variable, model);
			m.moveTo(b);
			modelMap.put(variable.name(), m);
			g.nodes.add(m);
			i++;
		}

		for (var m : g.nodes) {
			if (m.variable() instanceof Stock stock) {
				for (var flowId : stock.inFlows()) {
					link(modelMap.get(flowId), m, true);
				}
				for (var flowId : stock.outFlows()) {
					link(m, modelMap.get(flowId), true);
				}
			}
			var deps = EvaluationOrder.dependenciesOf(m.variable());
			for (var depId : deps) {
				link(modelMap.get(depId), m, false);
			}
		}

		return g;
	}

	private static void link(SdVarNode source, SdVarNode target, boolean isFlow) {
		if (source == null || target == null)
			return;
		var link = new SdVarLink(source, target, isFlow);
		source.sourceLinks().add(link);
		target.targetLinks().add(link);
	}

	public SdModel model() {
		return model;
	}

	public List<SdVarNode> nodes() {
		return nodes;
	}

	public void addListener(Runnable listener) {
		listeners.add(listener);
	}

	public void removeListener(Runnable listener) {
		listeners.remove(listener);
	}

	public void add(SdVarNode var) {
		nodes.add(var);
		for (var listener : listeners) {
			listener.run();
		}
	}

	public void remove(SdVarNode var) {
		nodes.remove(var);
		for (var listener : listeners) {
			listener.run();
		}
	}

}
