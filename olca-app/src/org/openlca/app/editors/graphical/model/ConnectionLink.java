package org.openlca.app.editors.graphical.model;

import java.util.Objects;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Connection;
import org.eclipse.swt.graphics.Color;
import org.openlca.core.model.ProcessLink;

public class ConnectionLink {

	public static Color COLOR = ColorConstants.gray;
	public static Color HIGHLIGHT_COLOR = ColorConstants.blue;

	public ProcessLink processLink;
	public Connection figure;
	public ProcessNode sourceNode;
	public ProcessNode targetNode;

	private LinkPart editPart;

	void setEditPart(LinkPart editPart) {
		this.editPart = editPart;
	}

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
		sourceNode.add(this);
		targetNode.add(this);
		sourceNode.refresh();
		targetNode.refresh();
	}

	public void unlink() {
		editPart.setSelected(0);
		sourceNode.remove(this);
		targetNode.remove(this);
		sourceNode.getEditPart().refreshSourceConnections();
		targetNode.getEditPart().refreshTargetConnections();
		sourceNode.refresh();
		targetNode.refresh();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ConnectionLink))
			return false;
		ConnectionLink link = (ConnectionLink) obj;
		if (!Objects.equals(processLink, link.processLink))
			return false;
		if (!Objects.equals(sourceNode, link.sourceNode))
			return false;
		if (!Objects.equals(targetNode, link.targetNode))
			return false;
		return true;
	}

	public boolean isVisible() {
		return figure != null ? figure.isVisible() : false;
	}

	public void setVisible(boolean value) {
		if (figure != null)
			figure.setVisible(value);
	}

}
