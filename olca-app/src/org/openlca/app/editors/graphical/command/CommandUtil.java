package org.openlca.app.editors.graphical.command;

import java.util.Collections;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class CommandUtil {

	public static Command buildCreateProcessesCommand(
			List<ProcessDescriptor> toCreate, ProductSystemNode systemNode) {
		Command command = null;
		for (ProcessDescriptor process : toCreate) {
			if (systemNode.getProcessNode(process.getId()) != null)
				continue;
			CreateProcessCommand newCommand = CommandFactory
					.createCreateProcessCommand(systemNode, process);
			command = chain(command, newCommand);
		}
		return command;
	}

	public static Command buildConnectProvidersCommand(
			ProcessDescriptor toConnect, ExchangeNode connectTo) {
		return buildConnectLinksCommand(Collections.singletonList(toConnect),
				connectTo, true);
	}

	public static Command buildConnectRecipientsCommand(
			List<ProcessDescriptor> toConnect, ExchangeNode connectTo) {
		return buildConnectLinksCommand(toConnect, connectTo, false);
	}

	private static Command buildConnectLinksCommand(
			List<ProcessDescriptor> toConnect, ExchangeNode connectTo,
			boolean provider) {
		Command command = null;
		ProcessNode processNode = connectTo.getParent().getParent();
		ProductSystemNode systemNode = processNode.getParent();
		for (ProcessDescriptor process : toConnect) {
			CreateLinkCommand newCommand = CommandFactory
					.createCreateLinkCommand(connectTo.getExchange().getFlow()
							.getId());
			ProcessNode otherNode = systemNode.getProcessNode(process.getId());
			if (otherNode == null) {
				otherNode = new ProcessNode(process);
				systemNode.add(otherNode);
			}
			if (provider) {
				newCommand.setSourceNode(otherNode);
				newCommand.setTargetNode(processNode);
			} else {
				newCommand.setSourceNode(processNode);
				newCommand.setTargetNode(otherNode);
			}
			command = chain(command, newCommand);
		}
		return command;
	}

	public static Command chain(Command command, Command toChain) {
		if (command == null)
			return toChain;
		return command.chain(toChain);
	}

	public static void executeCommand(Command command,
			ProductSystemGraphEditor editor) {
		if (command == null)
			return;
		editor.getCommandStack().execute(command);

	}

}