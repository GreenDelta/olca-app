package org.openlca.app.editors.graphical.model;

import java.util.Objects;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Connection;
import org.eclipse.swt.graphics.Color;
import org.openlca.core.model.ProcessLink;

public class ConnectionLink {

	public static Color COLOR = ColorConstants.gray;
	public static Color HIGHLIGHT_COLOR = ColorConstants.blue;

	private Connection figure;
	private ProcessLink processLink;
	private ProcessNode sourceNode;
	private ProcessNode targetNode;
	private LinkPart editPart;

	void setEditPart(LinkPart editPart) {
		this.editPart = editPart;
	}

	public void setSourceNode(ProcessNode sourceNode) {
		this.sourceNode = sourceNode;
	}

	public void setTargetNode(ProcessNode targetNode) {
		this.targetNode = targetNode;
	}

	public void setProcessLink(ProcessLink processLink) {
		this.processLink = processLink;
	}

	public ProcessLink getProcessLink() {
		return processLink;
	}

	public Connection getFigure() {
		return figure;
	}

	void setFigure(Connection figure) {
		this.figure = figure;
	}

	public ProcessNode getSourceNode() {
		return sourceNode;
	}

	void refreshSourceAnchor() {
		editPart.refreshSourceAnchor();
	}

	public ProcessNode getTargetNode() {
		return targetNode;
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
		if (!Objects.equals(getProcessLink(), link.getProcessLink()))
			return false;
		if (!Objects.equals(getSourceNode(), link.getSourceNode()))
			return false;
		if (!Objects.equals(getTargetNode(), link.getTargetNode()))
			return false;
		return true;
	}

	public boolean isVisible() {
		return getFigure() != null ? getFigure().isVisible() : false;
	}

	public void setVisible(boolean value) {
		if (getFigure() != null)
			getFigure().setVisible(value);
	}

}
