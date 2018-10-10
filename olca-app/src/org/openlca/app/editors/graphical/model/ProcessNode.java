package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.layout.LayoutManager;
import org.openlca.app.editors.graphical.layout.NodeLayoutInfo;
import org.openlca.app.editors.graphical.search.MutableProcessLinkSearchMap;
import org.openlca.app.util.Labels;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProcessType;
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

	@Override
	@SuppressWarnings("unchecked")
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
		if (equals(link.outputNode))
			editPart().refreshSourceConnections();
		if (equals(link.inputNode))
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
		if (equals(link.outputNode))
			editPart().refreshSourceConnections();
		if (equals(link.inputNode))
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
			if (link.outputNode.equals(this)) {
				otherNode = link.inputNode;
				isSource = true;
			} else if (link.inputNode.equals(this)) {
				otherNode = link.outputNode;
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
		return Labels.getDisplayName(process);
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
		ProcessDao dao = new ProcessDao(Database.get());
		Process process = dao.getForId(this.process.getId());
		List<Exchange> list = new ArrayList<>();
		for (Exchange e : process.getExchanges()) {
			if (e.flow.getFlowType() == FlowType.ELEMENTARY_FLOW)
				continue;
			list.add(e);
		}
		add(new IONode(list));
	}

	public void refresh() {
		Point location = figure().getLocation();
		if (xyLayoutConstraints != null)
			location = xyLayoutConstraints.getLocation();
		xyLayoutConstraints = new Rectangle(location, figure.getPreferredSize());
		figure().refresh();
	}

	public ExchangeNode getOutput(ProcessLink link) {
		if (link == null)
			return null;
		FlowType type = parent().flowTypes.get(link.flowId);
		if (type == null || type == FlowType.ELEMENTARY_FLOW)
			return null;
		for (ExchangeNode n : getExchangeNodes()) {
			Exchange e = n.exchange;
			if (e == null || e.isInput ||
					e.flow == null || e.flow.getId() != link.flowId)
				continue;
			if (type == FlowType.PRODUCT_FLOW)
				return n;
			if (type == FlowType.WASTE_FLOW
					&& e.getId() == link.exchangeId)
				return n;
		}
		return null;
	}

	public ExchangeNode getInput(ProcessLink link) {
		if (link == null)
			return null;
		FlowType type = parent().flowTypes.get(link.flowId);
		if (type == null || type == FlowType.ELEMENTARY_FLOW)
			return null;
		for (ExchangeNode n : getExchangeNodes()) {
			Exchange e = n.exchange;
			if (e == null || !e.isInput ||
					e.flow == null || e.flow.getId() != link.flowId)
				continue;
			if (type == FlowType.PRODUCT_FLOW
					&& e.getId() == link.exchangeId)
				return n;
			if (type == FlowType.WASTE_FLOW)
				return n;
		}
		return null;
	}

	public List<ExchangeNode> getExchangeNodes() {
		List<ExchangeNode> list = new ArrayList<>();
		for (IONode io : getChildren()) {
			for (ExchangeNode n : io.getChildren()) {
				list.add(n);
			}
		}
		return list;
	}

	public List<ExchangeNode> loadExchangeNodes() {
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

	/**
	 * Returns true if the exchange with the given ID is already connected by a
	 * process link.
	 */
	public boolean isConnected(long exchangeId) {
		MutableProcessLinkSearchMap linkSearch = parent().linkSearch;
		List<ProcessLink> links = linkSearch.getConnectionLinks(
				process.getId());
		for (ProcessLink link : links) {
			if (link.exchangeId == exchangeId)
				return true;
		}
		return false;
	}

	public int getMinimumHeight() {
		if (isMinimized())
			if (process.getProcessType() == ProcessType.LCI_RESULT)
				return ProcessFigure.MINIMUM_HEIGHT + 3;
			else
				return ProcessFigure.MINIMUM_HEIGHT;
		return figure().getMinimumHeight();
	}

	public int getMinimumWidth() {
		return ProcessFigure.MINIMUM_WIDTH;
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

	@Override
	public String toString() {
		String id = process != null
				? Long.toString(process.getId())
				: "null";
		return "ProcessNode [ id =" + id + " name = "
				+ Labels.getDisplayName(process) + " ]";
	}

}
