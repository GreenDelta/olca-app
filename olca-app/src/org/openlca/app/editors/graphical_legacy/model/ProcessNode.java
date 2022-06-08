package org.openlca.app.editors.graphical_legacy.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.layout.LayoutManager;
import org.openlca.app.editors.graphical_legacy.layout.NodeLayoutInfo;
import org.openlca.app.editors.graphical_legacy.view.ProcessFigure;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;

import com.google.common.base.Objects;
import org.openlca.core.model.descriptors.ResultDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

/**
 * A {@link ProcessNode} represents a unit process, a library process, a result
 * or a product system with its list of input or output flows (see
 * {@link IONode}).
 */
public class ProcessNode extends Node {

	public final RootDescriptor process;
	public final List<Link> links = new ArrayList<>();
	private Rectangle box;
	private boolean minimized = true;
	private boolean marked = false;

	public ProcessNode(GraphEditor editor, RootDescriptor d) {
		super(editor);
		this.process = d;
	}

	/**
	 * Creates the process node for the given ID. Note that the ID may refer to a
	 * process or product system (a sub-system). If it is an invalid ID we return
	 * null, so you need to check that.
	 */
	public static ProcessNode create(GraphEditor editor, long id) {
		var cache = Cache.getEntityCache();
		RootDescriptor d = cache.get(ProcessDescriptor.class, id);
		if (d == null) {
			d = cache.get(ProductSystemDescriptor.class, id);
		}
		if (d == null) {
			d = cache.get(ResultDescriptor.class, id);
		}
		return d != null
			? new ProcessNode(editor, d)
			: null;
	}

	public boolean isEditable() {
		if (process instanceof ProcessDescriptor p) {
			return !p.isFromLibrary() && p.processType == ProcessType.UNIT_PROCESS;
		}
		return false;
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
		if (box == null) {
			var prefSize = figure.getPreferredSize(-1, -1);
			box = new Rectangle(0, 0, prefSize.width, prefSize.height);
		}
		this.figure = figure;
	}

	public void apply(NodeLayoutInfo info) {
		minimized = info.minimized;
		marked = info.marked;
		if (!minimized && getChildren().isEmpty()) {
			add(new IONode(this));
		}
		box = info.box;
		figure.setBounds(info.box);
		figure().getLeftExpander().setExpanded(info.expandedLeft);
		figure().getRightExpander().setExpanded(info.expandedRight);
		figure().refresh();
		figure.getParent().setConstraint(figure, info.box);
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

	public ProcessPart editPart() {
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

	public boolean isMinimized() {
		return minimized;
	}

	public void minimize() {
		this.minimized = true;
		var childs = getChildren();
		if (!childs.isEmpty()) {
			var node = childs.get(0);
			remove(node);
		}
		refresh();
	}

	public void maximize() {
		this.minimized = false;
		if (getChildren().isEmpty()) {
			add(new IONode(this));
		}
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

	public void refresh() {
		Point location = figure().getLocation();
		if (box != null)
			location = box.getLocation();
		box = new Rectangle(location, figure.getPreferredSize());
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
		if (getChildren().isEmpty()) {
			add(new IONode(this));
		}
		return getExchangeNodes();
	}

	public Rectangle getBox() {
		return box;
	}

	public void setBox(Rectangle box) {
		this.box = box;
		var sysNode = parent();
		if (sysNode.figure != null) {
			sysNode.figure.revalidate();
		}
	}

	public int getMinimumHeight() {
		return isMinimized()
			? ProcessFigure.MINIMUM_HEIGHT
			: figure().getMinimumHeight();
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
	public void collapseLeft(ProcessNode initialNode) {
		if (!isExpandedLeft())
			return;
		figure().getLeftExpander().collapse(initialNode);
	}

	/**
	 * Used to avoid removing the initial node while collapsing, should only be
	 * called from within ProcessExpander.collapse
	 */
	public void collapseRight(ProcessNode initialNode) {
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
		if (!(obj instanceof ProcessNode other))
			return false;
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
		return "ProcessNode [ id =" + id + " content = "
			+ process + " ]";
	}

}
