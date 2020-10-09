package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.Collections;
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
import org.openlca.app.util.Labels;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;

import com.google.common.base.Objects;

public class ProcessNode extends Node {

	public final CategorizedDescriptor process;
	public final List<Link> links = new ArrayList<>();
	private Rectangle xyLayoutConstraints;
	private boolean minimized = true;
	private boolean marked = false;

	public ProcessNode(CategorizedDescriptor d) {
		this.process = d;
	}

	/**
	 * Creates the process node for the given ID. Note that the ID may refer to a
	 * process or product system (a sub-system). If it is an invalid ID we return
	 * null, so you need to check that.
	 */
	public static ProcessNode create(long id) {
		var cache = Cache.getEntityCache();
		CategorizedDescriptor d = cache.get(ProcessDescriptor.class, id);
		if (d == null) {
			d = cache.get(ProductSystemDescriptor.class, id);
		}
		return d != null ? new ProcessNode(d) : null;
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
		minimized = layout.minimized;
		marked = layout.marked;
		if (!minimized)
			if (getChildren().isEmpty())
				initializeExchangeNodes();
		Dimension prefSize = figure.getPreferredSize(-1, -1);
		xyLayoutConstraints = new Rectangle(layout.getLocation(), prefSize);
		figure.setBounds(getXyLayoutConstraints());
		figure().getLeftExpander().setExpanded(layout.expandedLeft);
		figure().getRightExpander().setExpanded(layout.expandedRight);
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

	@Override
	public String getName() {
		return Labels.name(process);
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
		if (this.process.type == ModelType.PROCESS) {
			ProcessDao dao = new ProcessDao(Database.get());
			Process p = dao.getForId(this.process.id);
			if (p == null)
				return;
			List<Exchange> list = new ArrayList<>();
			for (Exchange e : p.exchanges) {
				if (e.flow.flowType == FlowType.ELEMENTARY_FLOW)
					continue;
				list.add(e);
			}
			add(new IONode(list));
		} else if (this.process.type == ModelType.PRODUCT_SYSTEM) {
			ProductSystemDao dao = new ProductSystemDao(Database.get());
			ProductSystem s = dao.getForId(this.process.id);
			if (s != null && s.referenceExchange != null) {
				add(new IONode(Collections.singletonList(s.referenceExchange)));
			}
		}
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
		FlowType type = parent().flows.type(link.flowId);
		if (type == null || type == FlowType.ELEMENTARY_FLOW)
			return null;
		for (ExchangeNode n : getExchangeNodes()) {
			Exchange e = n.exchange;
			if (e == null || e.isInput ||
					e.flow == null || e.flow.id != link.flowId)
				continue;
			if (type == FlowType.PRODUCT_FLOW)
				return n;
			if (type == FlowType.WASTE_FLOW
					&& e.id == link.exchangeId)
				return n;
		}
		return null;
	}

	public ExchangeNode getInput(ProcessLink link) {
		if (link == null)
			return null;
		FlowType type = parent().flows.type(link.flowId);
		if (type == null || type == FlowType.ELEMENTARY_FLOW)
			return null;
		for (ExchangeNode n : getExchangeNodes()) {
			Exchange e = n.exchange;
			if (e == null || !e.isInput ||
					e.flow == null || e.flow.id != link.flowId)
				continue;
			if (type == FlowType.PRODUCT_FLOW
					&& e.id == link.exchangeId)
				return n;
			if (type == FlowType.WASTE_FLOW)
				return n;
		}
		return null;
	}

	public List<ExchangeNode> getExchangeNodes() {
		List<ExchangeNode> list = new ArrayList<>();
		for (IONode io : getChildren()) {
			list.addAll(io.getChildren());
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

	public void setXyLayoutConstraints(Rectangle constraints) {
		this.xyLayoutConstraints = constraints;
		editPart().revalidate();
	}

	/**
	 * Returns true if the exchange with the given ID is already connected by a
	 * process link.
	 */
	public boolean isConnected(long exchangeId) {
		MutableProcessLinkSearchMap linkSearch = parent().linkSearch;
		List<ProcessLink> links = linkSearch.getConnectionLinks(
				process.id);
		for (ProcessLink link : links) {
			if (link.exchangeId == exchangeId)
				return true;
		}
		return false;
	}

	public int getMinimumHeight() {
		if (isMinimized()) {
			// LCI results and product systems have an outer
			// border and this are a bit larger
			if (process instanceof ProductSystemDescriptor)
				return ProcessFigure.MINIMUM_HEIGHT + 3;
			if (process instanceof ProcessDescriptor) {
				var p = (ProcessDescriptor) process;
				return p.processType == ProcessType.LCI_RESULT
						? ProcessFigure.MINIMUM_HEIGHT + 3
						: ProcessFigure.MINIMUM_HEIGHT;
			}
		}
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
		LayoutManager m = (LayoutManager) parent().figure.getLayoutManager();
		m.layout(figure, parent().editor.getLayoutType());
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
				? Long.toString(process.id)
				: "null";
		return "ProcessNode [ id =" + id + " name = "
				+ Labels.name(process) + " ]";
	}

}
