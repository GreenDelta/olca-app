package org.openlca.app.editors.graphical.view;

import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.graphical.command.ExpansionCommand;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.rcp.images.Icon;

public class ProcessExpanderButton extends ImageFigure {

	private final ProcessNode node;
	private final ProcessNode.Side side;

	ProcessExpanderButton(ProcessNode node, ProcessNode.Side side) {
		this.node = node;
		this.side = side;
		setImage(Icon.PLUS.get());
		setVisible(node.shouldProcessExpanderBeVisible(side));
		addMouseListener(new ExpansionListener());
	}

	void refresh() {
		if (node.shouldProcessExpanderBeVisible(side))
			setVisible(false);
		else if (node.isExpanded(side))
			setImage(Icon.MINUS.get());
		else
			setImage(Icon.PLUS.get());
	}

	private class ExpansionListener implements MouseListener {

		@Override
		public void mouseDoubleClicked(MouseEvent me) {
		}

		@Override
		public void mousePressed(MouseEvent me) {
			Command command = getCommand();
			node.parent().editor.getCommandStack().execute(command);
		}

		private Command getCommand() {
			if (side == ProcessNode.Side.INPUT) {
				return node.isExpanded(side)
					? ExpansionCommand.collapseLeft(node)
					: ExpansionCommand.expandLeft(node);
			} else {
				return node.isExpanded(side)
					? ExpansionCommand.collapseRight(node)
					: ExpansionCommand.expandRight(node);
			}
		}

		@Override
		public void mouseReleased(MouseEvent me) {
		}
	}

}
