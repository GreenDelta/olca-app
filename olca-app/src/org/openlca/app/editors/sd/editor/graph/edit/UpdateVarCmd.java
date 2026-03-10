package org.openlca.app.editors.sd.editor.graph.edit;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.Stock;
import org.openlca.sd.model.Var;

import java.util.ArrayList;

public class UpdateVarCmd extends Command {

	private final SdGraph graph;
	private final Id name;
	private final Var data;

	public UpdateVarCmd(SdGraph graph, Id name, Var data) {
		this.graph = graph;
		this.name = name;
		this.data = data;
		setLabel("Update variable");
	}

	@Override
	public boolean canExecute() {
		return graph != null && name != null && data != null;
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void execute() {
		var origin = graph.getNode(name);
		if (origin == null) return;
		graph.remove(origin);

		var v = origin.variable();
		v.setName(data.name());
		v.setDef(data.def());
		v.setUnit(data.unit());

		if (v instanceof Stock stock && data instanceof Stock stockData) {
			stock.setInFlows(new ArrayList<>(stockData.inFlows()));
			stock.setOutFlows(new ArrayList<>(stockData.outFlows()));
		}
		graph.add(origin);
	}
}
