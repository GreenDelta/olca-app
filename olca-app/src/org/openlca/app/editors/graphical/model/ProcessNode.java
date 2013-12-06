package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.ProcessLinks;
import org.openlca.app.editors.graphical.layout.GraphLayoutManager;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Location;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.ProcessDescriptor;

import com.google.common.base.Objects;

public class ProcessNode extends Node {

	private ProcessDescriptor process;
	private List<ConnectionLink> links = new ArrayList<>();
	private Rectangle xyLayoutConstraints;
	private boolean minimized = true;
	private boolean marked = false;

	public ProcessNode(ProcessDescriptor process) {
		this.process = process;
	}

	@Override
	public ProductSystemNode getParent() {
		return (ProductSystemNode) super.getParent();
	}

	@Override
	ProcessPart getEditPart() {
		return (ProcessPart) super.getEditPart();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<InputOutputNode> getChildren() {
		return (List<InputOutputNode>) super.getChildren();
	}

	private ProcessFigure getProcessFigure() {
		return (ProcessFigure) getFigure();
	}

	@Override
	protected void setFigure(IFigure figure) {
		xyLayoutConstraints = new Rectangle(0, 0, figure.getPreferredSize(-1,
				-1).width, figure.getPreferredSize(-1, -1).height);
		super.setFigure(figure);
	}

	public void add(ConnectionLink link) {
		if (!links.contains(link)) {
			links.add(link);
			if (equals(link.getSourceNode()))
				getEditPart().refreshSourceConnections();
			if (equals(link.getTargetNode()))
				getEditPart().refreshTargetConnections();
			getProcessFigure().refresh();
		}
	}

	public void remove(ConnectionLink link) {
		if (links.contains(link)) {
			links.remove(link);
			if (equals(link.getSourceNode()))
				getEditPart().refreshSourceConnections();
			if (equals(link.getTargetNode()))
				getEditPart().refreshTargetConnections();
			getProcessFigure().refresh();
		}
	}

	public List<ConnectionLink> getLinks() {
		return links;
	}

	public ConnectionLink getLink(ProcessLink link) {
		for (ConnectionLink l : links)
			if (l.getProcessLink().equals(link))
				return l;
		return null;
	}

	public void removeAllLinks() {
		for (ConnectionLink link : links)
			if (!link.getSourceNode().getFigure().isVisible()
					|| !link.getTargetNode().getFigure().isVisible())
				link.unlink();
	}

	@Override
	public String getName() {
		String text = process.getName();
		if (process.getLocation() != null)
			text += " ["
					+ Cache.getEntityCache()
							.get(Location.class, process.getLocation())
							.getCode() + "]";
		return text;
	}

	public ProcessDescriptor getProcess() {
		return process;
	}

	public boolean isMinimized() {
		return minimized;
	}

	public void minimize() {
		this.minimized = true;
		refresh();
	}

	public void maximize() {
		this.minimized = false;
		if (getChildren().isEmpty())
			initializeExchangeNodes();
		refresh();
	}

	public void mark() {
		this.marked = true;
		refresh();
	}

	public void unmark() {
		this.marked = false;
		refresh();
	}

	public boolean isMarked() {
		return marked;
	}

	private void initializeExchangeNodes() {
		Process process = new ProcessDao(Database.get()).getForId(this.process
				.getId());
		List<Exchange> technologies = new ArrayList<>();
		for (Exchange exchange : process.getExchanges())
			if (exchange.getFlow().getFlowType() == FlowType.ELEMENTARY_FLOW)
				continue;
			else
				technologies.add(exchange);
		Exchange[] technologyArray = technologies
				.toArray(new Exchange[technologies.size()]);
		add(new InputOutputNode(technologyArray));
	}

	private void refresh() {
		xyLayoutConstraints = new Rectangle(getProcessFigure().getLocation(),
				getFigure().getPreferredSize());
		getProcessFigure().refresh();
	}

	public ExchangeNode getInputNode(long flowId) {
		for (ExchangeNode node : getExchangeNodes())
			if (!node.isDummy())
				if (node.getExchange().isInput())
					if (node.getExchange().getFlow().getId() == flowId)
						return node;
		return null;
	}

	public ExchangeNode getOutputNode(long flowId) {
		for (ExchangeNode node : getExchangeNodes())
			if (!node.isDummy())
				if (!node.getExchange().isInput())
					if (node.getExchange().getFlow().getId() == flowId)
						return node;
		return null;
	}

	public ExchangeNode[] getExchangeNodes() {
		List<ExchangeNode> exchangesNodes = new ArrayList<>();
		for (InputOutputNode node : getChildren())
			for (ExchangeNode node2 : node.getChildren())
				exchangesNodes.add(node2);
		ExchangeNode[] result = new ExchangeNode[exchangesNodes.size()];
		exchangesNodes.toArray(result);
		return result;
	}

	public ExchangeNode[] loadExchangeNodes() {
		if (getChildren().isEmpty())
			initializeExchangeNodes();
		return getExchangeNodes();
	}

	public Rectangle getXyLayoutConstraints() {
		return xyLayoutConstraints;
	}

	public void setXyLayoutConstraints(Rectangle xyLayoutConstraints) {
		this.xyLayoutConstraints = xyLayoutConstraints;
		getEditPart().revalidate();
	}

	public boolean hasIncomingConnection(long flowId) {
		for (ProcessLink link : ProcessLinks.getIncoming(getParent()
				.getProductSystem(), getProcess().getId()))
			if (link.getFlowId() == flowId)
				return true;
		return false;
	}

	public int getMinimumHeight() {
		if (isMinimized())
			return ProcessFigure.MINIMUM_HEIGHT;
		return getProcessFigure().getMinimumHeight();
	}

	public int getMinimumWidth() {
		return ProcessFigure.MINIMUM_WIDTH;
	}

	public void setLinksHighlighted(boolean value) {
		for (ConnectionLink link : links)
			if (value)
				link.setSelected(1);
			else
				link.setSelected(0);
	}

	public boolean hasConnections() {
		if (links.size() > 0)
			return true;
		return false;
	}

	public boolean hasOutgoingConnections() {
		for (ConnectionLink link : links)
			if (link.getSourceNode().equals(this))
				return true;
		return false;
	}

	public boolean hasIncomingConnections() {
		for (ConnectionLink link : links)
			if (link.getTargetNode().equals(this))
				return true;
		return false;
	}

	public void collapseLeft() {
		getProcessFigure().getLeftExpander().collapse();
	}

	public void collapseRight() {
		getProcessFigure().getRightExpander().collapse();
	}

	public void expandLeft() {
		getProcessFigure().getLeftExpander().expand();
	}

	public void expandRight() {
		getProcessFigure().getRightExpander().expand();
	}

	public boolean isExpandedLeft() {
		return getProcessFigure().getLeftExpander().isExpanded();
	}

	public boolean isExpandedRight() {
		return getProcessFigure().getRightExpander().isExpanded();
	}

	public void layout() {
		GraphLayoutManager layoutManager = (GraphLayoutManager) getParent()
				.getFigure().getLayoutManager();
		layoutManager.layout(getFigure(), getParent().getEditor()
				.getLayoutType());
	}

	public void setSelected(int value) {
		getEditPart().setSelected(value);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ProcessNode))
			return false;
		ProcessNode other = (ProcessNode) obj;
		return Objects.equal(getProcess(), other.getProcess());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getProcess());
	}

}