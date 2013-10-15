package org.openlca.app.editors.graphical.command;

import static org.openlca.app.editors.graphical.command.ExpansionCommand.COLLAPSE;
import static org.openlca.app.editors.graphical.command.ExpansionCommand.EXPAND;
import static org.openlca.app.editors.graphical.command.ExpansionCommand.LEFT;
import static org.openlca.app.editors.graphical.command.ExpansionCommand.RIGHT;
import static org.openlca.app.editors.graphical.command.HideShowCommand.HIDE;
import static org.openlca.app.editors.graphical.command.HideShowCommand.SHOW;
import static org.openlca.app.editors.graphical.command.XYLayoutCommand.MOVE;
import static org.openlca.app.editors.graphical.command.XYLayoutCommand.RESIZE;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.editors.graphical.layout.GraphLayoutManager;
import org.openlca.app.editors.graphical.layout.GraphLayoutType;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class CommandFactory {

	public static ExpansionCommand createExpandLeftCommand(ProcessNode node) {
		ExpansionCommand command = new ExpansionCommand(EXPAND, LEFT);
		command.setNode(node);
		return command;
	}

	public static ExpansionCommand createExpandRightCommand(ProcessNode node) {
		ExpansionCommand command = new ExpansionCommand(EXPAND, RIGHT);
		command.setNode(node);
		return command;
	}

	public static ExpansionCommand createCollapseLeftCommand(ProcessNode node) {
		ExpansionCommand command = new ExpansionCommand(COLLAPSE, LEFT);
		command.setNode(node);
		return command;
	}

	public static ExpansionCommand createCollapseRightCommand(ProcessNode node) {
		ExpansionCommand command = new ExpansionCommand(COLLAPSE, RIGHT);
		command.setNode(node);
		return command;
	}

	public static XYLayoutCommand createMoveCommand(ProcessNode node,
			Rectangle layout) {
		XYLayoutCommand command = new XYLayoutCommand(MOVE);
		command.setNode(node);
		command.setLayout(layout);
		return command;
	}

	public static XYLayoutCommand createResizeCommand(ProcessNode node,
			Rectangle layout) {
		XYLayoutCommand command = new XYLayoutCommand(RESIZE);
		command.setNode(node);
		command.setLayout(layout);
		return command;
	}

	public static ChangeStateCommand createChangeStateCommand(ProcessNode node) {
		ChangeStateCommand command = new ChangeStateCommand();
		command.setNode(node);
		return command;
	}

	public static CreateLinkCommand createCreateLinkCommand(long flowId) {
		CreateLinkCommand command = new CreateLinkCommand();
		command.setFlowId(flowId);
		return command;
	}

	public static CreateProcessCommand createCreateProcessCommand(
			ProductSystemNode model, ProcessDescriptor process) {
		CreateProcessCommand command = new CreateProcessCommand();
		command.setModel(model);
		command.setProcess(process);
		return command;
	}

	public static DeleteLinkCommand createDeleteLinkCommand(ConnectionLink link) {
		DeleteLinkCommand command = new DeleteLinkCommand();
		command.setLink(link);
		return command;
	}

	public static DeleteProcessCommand createDeleteProcessCommand(
			ProcessNode node) {
		DeleteProcessCommand command = new DeleteProcessCommand();
		command.setNode(node);
		return command;
	}

	public static ReconnectLinkCommand createReconnectLinkCommand(
			ConnectionLink link, ProcessNode sourceNode, ProcessNode targetNode) {
		ReconnectLinkCommand command = new ReconnectLinkCommand();
		command.setLink(link);
		command.setSourceNode(sourceNode);
		command.setTargetNode(targetNode);
		return command;
	}

	public static HideShowCommand createShowCommand(ProcessDescriptor process,
			ProductSystemNode model) {
		HideShowCommand command = new HideShowCommand(SHOW);
		command.setProcess(process);
		command.setModel(model);
		return command;
	}

	public static HideShowCommand createHideCommand(ProcessDescriptor process,
			ProductSystemNode model) {
		HideShowCommand command = new HideShowCommand(HIDE);
		command.setProcess(process);
		command.setModel(model);
		return command;
	}

	public static LayoutCommand createLayoutCommand(ProductSystemNode model,
			GraphLayoutManager layoutManager, GraphLayoutType layoutType) {
		LayoutCommand command = new LayoutCommand();
		command.setModel(model);
		command.setLayoutManager(layoutManager);
		command.setLayoutType(layoutType);
		return command;
	}
	
	public static MarkingCommand createMarkingCommand(ProcessNode node) {
		MarkingCommand command = new MarkingCommand();
		command.setNode(node);
		return command;
	}
	
}
