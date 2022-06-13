package org.openlca.app.editors.graphical_legacy.model;

import java.util.Objects;

import org.eclipse.draw2d.Connection;
import org.openlca.core.model.ProcessLink;

/**
 * A model of the connection between the output flow {@link ExchangeNode} of a
 * {@link ProcessNode} to the input flow of another {@link ProcessNode}.
 */
public class Link {

	public ProcessLink processLink;
	public ProcessNode outputNode;
	public ProcessNode inputNode;

	/**
	 * @deprecated Link is the model and should not have a reference to the view
	 * (EditPart)
	 */
	@Deprecated
	public Connection figure;

	/**
	 * @deprecated Link is the model and should not have a reference to the
	 * controller (EditPart)
	 */
	@Deprecated
	LinkPart editPart;

	/**
	 * Returns the provider node of the link which is the output node in case
	 * of a production process and the input link in case of a waste treatment
	 * process.
	 */
	public ProcessNode provider() {
		if (processLink == null)
			return null;
		if (outputNode != null
				&& outputNode.process != null
				&& outputNode.process.id == processLink.providerId)
			return outputNode;
		if (inputNode != null
				&& inputNode.process != null
				&& inputNode.process.id == processLink.providerId)
			return inputNode;
		return null;
	}

	/**
	 * @deprecated call methods on the EditPart directly
	 */
	@Deprecated
	public void refreshSourceAnchor() {
		editPart.refreshSourceAnchor();
	}

	/**
	 * @deprecated call methods on the EditPart directly
	 */
	@Deprecated
	public void refreshTargetAnchor() {
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
		if (!(obj instanceof Link other))
			return false;
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
	public void updateVisibility() {
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
