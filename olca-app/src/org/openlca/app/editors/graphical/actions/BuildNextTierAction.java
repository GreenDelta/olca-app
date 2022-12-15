package org.openlca.app.editors.graphical.actions;

import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.model.commands.MassCreationCommand;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.ArrayList;


public class BuildNextTierAction extends BuildAction {

	private ArrayList<RootDescriptor> newProviders;
	private ArrayList<ProcessLink> newConnections;

	public BuildNextTierAction(GraphEditor part) {
		super(part);
		setId(GraphActionIds.BUILD_NEXT_TIER);
		setText(M.BuildNextTier);
	}

	@Override
	public void run() {
		var graph = editor.getModel();

		newProviders = new ArrayList<>();
		newConnections = new ArrayList<>();
		collect();

		var command = MassCreationCommand.nextTier(newProviders, newConnections, graph);

		if (command.canExecute())
			execute(command);
	}

	private void collect() {
		for (var exchange : mapExchangeToProcess.keySet()) {
			var process = mapExchangeToProcess.get(exchange);
			var provider = findProvider(exchange);
			if (provider == null)
				continue;
			if (!newProviders.contains(provider)) {
				newProviders.add(provider);
			}
			var link = getLink(exchange, process, provider);

			if (!newConnections.contains(link)) {
				newConnections.add(link);
			}
		}
	}

	@Override
	protected boolean calculateEnabled() {
		return true;
	}

}
