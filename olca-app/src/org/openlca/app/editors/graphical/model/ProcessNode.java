package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.layout.LayoutManager;
import org.openlca.app.editors.graphical.layout.NodeLayoutInfo;
import org.openlca.app.editors.graphical.search.MutableProcessLinkSearchMap;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Location;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.ProcessDescriptor;

import com.google.common.base.Objects;

public class ProcessNode extends Node {

	public final ProcessDescriptor process;
	public final List<Link> links = new ArrayList<>();
	private Rectangle xyLayoutConstraints;
	private boolean minimized = true;
	private boolean marked = false;

	public ProcessNode(ProcessDescriptor process) {
		this.process = process;
	}

	@Override
	public ProductSystemNode parent() {
		return (ProductSystemNode) super.parent();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<IONode> getChildren() {
		return (List<IONode>) super.getChildren();
	}

	void setFigure(IFigure figure) {
		Dimension prefSize = figure.getPreferredSize(-1, -1);
		if (xyLayoutConstraints == null)
			xyLayoutConstraints = new Rectangle(0, 0, prefSize.width, prefSize.height);
		this.figure = figure;
	}

	public void apply(NodeLayoutInfo layout) {
		minimized = layout.isMinimized();
		if (!minimized)
			if (getChildren().isEmpty())
				initializeExchangeNodes();
		Dimension prefSize = figure.getPreferredSize(-1, -1);
		xyLayoutConstraints = new Rectangle(layout.getLocation(), prefSize);
		figure.setBounds(getXyLayoutConstraints());
		figure().getLeftExpander().setExpanded(layout.isExpandedLeft());
		figure().getRightExpander().setExpanded(layout.isExpandedRight());
		figure().refresh();
	}

	public void add(Link link) {
		if (links.contains(link))
			return;
		links.add(link);
		if (equals(link.sourceNode))
			editPart().refreshSourceConnections();
		if (equals(link.targetNode))
			editPart().refreshTargetConnections();
		editPart.refresh();
	}

	ProcessPart editPart() {
		return (ProcessPart) editPart;
	}

	public void remove(Link link) {
		if (!links.contains(link))
			return;
		links.remove(link);
		if (equals(link.sourceNode))
			editPart().refreshSourceConnections();
		if (equals(link.targetNode))
			editPart().refreshTargetConnections();
		editPart.refresh();
	}

	public Link getLink(ProcessLink link) {
		for (Link l : links)
			if (l.processLink.equals(link))
				return l;
		return null;
	}

	public void showLinks() {
		for (Link link : links) {
			ProcessNode otherNode = null;
			boolean isSource = false;
			if (link.sourceNode.equals(this)) {
				otherNode = link.targetNode;
				isSource = true;
			} else if (link.targetNode.equals(this)) {
				otherNode = link.sourceNode;
			}
			if (!otherNode.isVisible())
				continue;
			if (isSource && otherNode.isExpandedLeft())
				link.setVisible(true);
			else if (!isSource && otherNode.isExpandedRight())
				link.setVisible(true);
		}
	}

	@Override
	public String getName() {
		String text = process.getName();
		if (process.getLocation() != null)
			text += " [" + Cache.getEntityCache().get(Location.class, process.getLocation()).getCode() + "]";
		return text;
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
		Process process = new ProcessDao(Database.get()).getForId(this.process.getId());
		List<Exchange> technologies = new ArrayList<>();
		for (Exchange exchange : process.getExchanges())
			if (exchange.getFlow().getFlowType() == FlowType.ELEMENTARY_FLOW)
				continue;
			else
				technologies.add(exchange);
		Exchange[] technologyArray = technologies.toArray(new Exchange[technologies.size()]);
		add(new IONode(technologyArray));
	}

	public void refresh() {
		Point location = figure().getLocation();
		if (xyLayoutConstraints != null)
			location = xyLayoutConstraints.getLocation();
		xyLayoutConstraints = new Rectangle(location, figure.getPreferredSize());
		figure().refresh();
	}

	public List<ExchangeNode> getInputs(long flowId) {
		List<ExchangeNode> nodes = new ArrayList<>();
		for (ExchangeNode node : getExchangeNodes())
			if (!node.isDummy())
				if (node.exchange.isInput())
					if (node.exchange.getFlow().getId() == flowId)
						nodes.add(node);
		return nodes;
	}

	public ExchangeNode getOutput(long flowId) {
		for (ExchangeNode node : getExchangeNodes())
			if (!node.isDummy())
				if (!node.exchange.isInput())
					if (node.exchange.getFlow().getId() == flowId)
						return node;
		return null;
	}

	public ExchangeNode getNode(long exchangeId) {
		for (ExchangeNode node : getExchangeNodes())
			if (!node.isDummy())
				if (node.exchange.getId() == exchangeId)
					return node;
		return null;

	}

	public ExchangeNode[] getExchangeNodes() {
		List<ExchangeNode> exchangesNodes = new ArrayList<>();
		for (IONode node : getChildren())
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
		editPart().revalidate();
	}

	public boolean hasIncoming(long exchangeId) {
		MutableProcessLinkSearchMap linkSearch = parent().linkSearch;
		for (ProcessLink link : linkSearch.getIncomingLinks(process.getId()))
			if (link.exchangeId == exchangeId)
				return true;
		return false;
	}

	public int getMinimumHeight() {
		if (isMinimized())
			return ProcessFigure.MINIMUM_HEIGHT;
		return figure().getMinimumHeight();
	}

	public int getMinimumWidth() {
		return ProcessFigure.MINIMUM_WIDTH;
	}

	public boolean hasConnections() {
		return links.size() > 0;
	}

	public int countOutgoing() {
		int count = 0;
		for (Link link : links)
			if (link.sourceNode.equals(this))
				count++;
		return count;
	}

	public int countIncoming() {
		int count = 0;
		for (Link link : links)
			if (link.targetNode.equals(this))
				count++;
		return count;
	}

	public void collapseLeft() {
		if (!isExpandedLeft())
			return;
		figure().getLeftExpander().collapse(this);
	}

	public void collapseRight() {
		if (!isExpandedRight())
			return;
		figure().getRightExpander().collapse(this);
	}

	/**
	 * Used to avoid removing the initial node while collapsing, should only be
	 * called from within ProcessExpander.collapse
	 */
	void collapseLeft(ProcessNode initialNode) {
		if (!isExpandedLeft())
			return;
		figure().getLeftExpander().collapse(initialNode);
	}

	/**
	 * Used to avoid removing the initial node while collapsing, should only be
	 * called from within ProcessExpander.collapse
	 */
	void collapseRight(ProcessNode initialNode) {
		if (!isExpandedRight())
			return;
		figure().getRightExpander().collapse(initialNode);
	}

	public void expandLeft() {
		figure().getLeftExpander().expand();
	}

	public void expandRight() {
		figure().getRightExpander().expand();
	}

	public boolean isExpandedLeft() {
		return figure().getLeftExpander().isExpanded();
	}

	public boolean isExpandedRight() {
		return figure().getRightExpander().isExpanded();
	}

	public void layout() {
		LayoutManager layoutManager = (LayoutManager) parent().figure.getLayoutManager();
		layoutManager.layout(figure, parent().editor.getLayoutType());
	}

	public void select() {
		parent().editor.getGraphicalViewer().select(editPart);
	}

	public void reveal() {
		parent().editor.getGraphicalViewer().reveal(editPart);
	}

	private ProcessFigure figure() {
		return (ProcessFigure) figure;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ProcessNode))
			return false;
		ProcessNode other = (ProcessNode) obj;
		return Objects.equal(process, other.process);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(process);
	}

}
