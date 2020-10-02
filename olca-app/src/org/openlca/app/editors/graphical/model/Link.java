package org.openlca.app.editors.graphical.model;

import java.util.Objects;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Connection;
import org.eclipse.swt.graphics.Color;
import org.openlca.core.model.ProcessLink;

public class Link {

	public static Color COLOR = ColorConstants.gray;
	public static Color HIGHLIGHT_COLOR = ColorConstants.blue;

	public ProcessLink processLink;
	public ProcessNode outputNode;
	public ProcessNode inputNode;

	public Connection figure;
	LinkPart editPart;

	void refreshSourceAnchor() {
		editPart.refreshSourceAnchor();
	}

	void refreshTargetAnchor() {
		editPart.refreshTargetAnchor();
	}

	void setSelected(int value) {
		editPart.setSelected(value);
	}

	public void link() {
		outputNode.add(this);
		inputNode.add(this);
		outputNode.editPart().refreshSourceConnections();
		inputNode.editPart().refreshTargetConnections();
		outputNode.refresh();
		inputNode.refresh();
	}

	public void unlink() {
		editPart.setSelected(0);
		outputNode.remove(this);
		inputNode.remove(this);
		outputNode.editPart().refreshSourceConnections();
		inputNode.editPart().refreshTargetConnections();
		outputNode.refresh();
		inputNode.refresh();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Link))
			return false;
		var other = (Link) obj;
		return Objects.equals(processLink, other.processLink)
				&& Objects.equals(outputNode, other.outputNode)
				&& Objects.equals(inputNode, other.inputNode);
	}

	public boolean isVisible() {
		return figure != null && figure.isVisible();
	}

	/**
	 * A link is visible when the respective processes are visible and at least
	 * one of the processes is expanded on the respective site.
	 */
	public void updateVisibilty() {
		if (figure == null)
			return;
		if (!inputNode.isVisible() || !outputNode.isVisible()) {
			figure.setVisible(false);
			return;
		}
		if (inputNode.isExpandedLeft() || outputNode.isExpandedRight()) {
			figure.setVisible(true);
			return;
		}
		figure.setVisible(false);
	}

}
