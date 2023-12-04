package org.openlca.app.editors.graphical.model.commands;

import org.eclipse.gef.commands.Command;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.util.Question;
import org.openlca.core.model.ProcessLink;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class RemoveSupplyChainCommand extends Command {

	private final Graph graph;
	private final ArrayList<ProcessLink> providerLinks;
	private int answer;

	public RemoveSupplyChainCommand(ArrayList<ProcessLink> links, Graph graph) {
		this.graph = graph;
		providerLinks = links;
		setLabel(M.RemoveSupplyChain.toLowerCase(Locale.ROOT));
	}

	@Override
	public boolean canExecute() {
		return (!providerLinks.isEmpty() && graph != null);
	}

	@Override
	public void execute() {
		answer = Question.ask("Deleting the supply chain",
				DeleteManager.QUESTION,
				Arrays.stream(DeleteManager.Answer.values()).map(Enum::name).toArray(String[]::new));

		if (answer != DeleteManager.Answer.Cancel.ordinal()) {
			redo();
		}
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public void redo() {
		App.runInUI("Removing the supply chain", () -> {
			var graphLinks = providerLinks.stream().map(graph::getLink).toList();
			DeleteManager.on(graph).graphLinks(graphLinks, answer);
		});
	}

}
