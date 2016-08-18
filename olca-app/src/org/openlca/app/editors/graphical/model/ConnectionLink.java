package org.openlca.app.editors.graphical.model;

import java.util.Objects;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Connection;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.editors.graphical.GraphUtil;
import org.openlca.core.model.ProcessLink;

public class ConnectionLink {

	public static Color COLOR = ColorConstants.gray;
	public static Color HIGHLIGHT_COLOR = ColorConstants.blue;

	public ProcessLink processLink;
	public Connection figure;

	public ExchangeNode provider;
	public ExchangeNode exchange;

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
		ProcessNode providerProcess = GraphUtil.getProcessNode(provider);
		providerProcess.add(this);
		providerProcess.refresh();
		ProcessNode exchangeProcess = GraphUtil.getProcessNode(exchange);
		exchangeProcess.add(this);
		exchangeProcess.refresh();
	}

	public void unlink() {
		editPart.setSelected(0);
		ProcessNode providerProcess = GraphUtil.getProcessNode(provider);
		providerProcess.remove(this);
		providerProcess.getEditPart().refreshSourceConnections();
		providerProcess.refresh();

		ProcessNode exchangeProcess = GraphUtil.getProcessNode(exchange);
		exchangeProcess.remove(this);
		exchangeProcess.getEditPart().refreshTargetConnections();
		exchangeProcess.refresh();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ConnectionLink))
			return false;
		ConnectionLink link = (ConnectionLink) obj;
		if (!Objects.equals(processLink, link.processLink))
			return false;
		if (!Objects.equals(provider, link.provider))
			return false;
		if (!Objects.equals(exchange, link.exchange))
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
