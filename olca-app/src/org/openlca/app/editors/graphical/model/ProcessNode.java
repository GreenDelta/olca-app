package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.layout.LayoutManager;
import org.openlca.app.editors.graphical.layout.NodeLayoutInfo;
import org.openlca.app.editors.graphical.search.MutableProcessLinkSearchMap;
import org.openlca.app.editors.graphical.view.NodeFigure;
import org.openlca.app.util.UI;
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
	private ProcessExpander inputProcessExpander = null;
	private ProcessExpander outputProcessExpander = null;

	public static final int MINIMUM_WIDTH = 250;
	public static int MINIMUM_HEIGHT_MINIMIZED = 34;
	private static final int MINIMUM_HEIGHT_MAXIMIZED = 250;
	static {
		try {
			var boldFont = UI.boldFont();
			if (boldFont != null) {
				var data = boldFont.getFontData();
				int max = 0;
				for (var datum : data) {
					max = Math.max(datum.getHeight(), max);
				}
				MINIMUM_HEIGHT_MINIMIZED = Math.max(max + 4 + 10, MINIMUM_HEIGHT_MINIMIZED);
			}
		} catch (Exception ignored) {
		}
	}

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

	public void apply(NodeLayoutInfo info) {
		if (minimized != info.minimized) {
			minimized = info.minimized;
		}

		if (!minimized && getChildren().isEmpty()) {
			add(new IONode(this));
		}

		marked = info.marked;
		box = info.box;
		figure.setBounds(info.box);

		if (ProcessExpander.canExpand(this, Side.INPUT)) {
			inputProcessExpander = new ProcessExpander(Side.INPUT);
			inputProcessExpander.expanded = info.expandedLeft;
		}
		if (ProcessExpander.canExpand(this, Side.OUTPUT)) {
			outputProcessExpander = new ProcessExpander(Side.OUTPUT);
			outputProcessExpander.expanded = info.expandedRight;
		}
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

	public void setIsMinimized(boolean minimized) {
		this.minimized = minimized;
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
			? MINIMUM_HEIGHT_MINIMIZED
			: MINIMUM_HEIGHT_MAXIMIZED;
	}

	public int getMinimumWidth() {
		return MINIMUM_WIDTH;
	}

	public ProcessExpander processExpanderOf(Side side) {
		return side.equals(Side.INPUT)
			? inputProcessExpander
			: outputProcessExpander;
	}

	public void collapse() {
		for (Side side: Side.values())
			collapse(side);
	}

	public void collapse(Side side) {
		if (!isExpanded(side))
			return;
		processExpanderOf(side).collapse(this);
	}

	/**
	 * Used to avoid removing the initial node while collapsing, should only be
	 * called from within ProcessExpander.collapse
	 */
	public void collapse(Side side, ProcessNode initialNode) {
		if (!isExpanded(side))
			return;
		processExpanderOf(side).collapse(initialNode);
	}

	public void expand() {
		for (Side side: Side.values())
			if (processExpanderOf(side) != null)
				expand(side);
	}

	public void expand(Side side) {
		processExpanderOf(side).expand();
	}

	public boolean isExpanded(Side side) {
		if (processExpanderOf(side) == null)
			return false;
		else return processExpanderOf(side).isExpanded();
	}

	public boolean shouldProcessExpanderBeVisible(Side side) {
		if (processExpanderOf(side) == null)
			return false;
		else return processExpanderOf(side).shouldBeVisible();
	}

	public void layout() {
		LayoutManager m = (LayoutManager) parent().figure.getLayoutManager();
		m.layout(figure, parent().editor.getLayoutType());
	}

	private NodeFigure figure() {
		return (NodeFigure) figure;
	}

	public void select() {
		parent().editor.getGraphicalViewer().select(editPart);
	}

	public void reveal() {
		parent().editor.getGraphicalViewer().reveal(editPart);
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

	public enum Side {
		INPUT, OUTPUT
	}

	private class ProcessExpander {

		private final Side side;
		private boolean expanded;
		// isCollapsing is used to prevent endless recursion in collapse()
		private boolean isCollapsing;

		private ProcessExpander(Side side) {
			this.side = side;
		}

		public boolean isExpanded() {
			return expanded;
		}

		public void setExpanded(boolean value) {
			expanded = value;
		}

		public boolean shouldBeVisible() {
			ProductSystemNode sysNode = ProcessNode.this.parent();
			MutableProcessLinkSearchMap linkSearch = sysNode.linkSearch;
			long processId = ProcessNode.this.process.id;
			for (ProcessLink link : linkSearch.getLinks(processId)) {
				FlowType type = sysNode.flows.type(link.flowId);
				boolean isProvider = link.providerId == processId;
				if (side == Side.INPUT) {
					if (type == FlowType.PRODUCT_FLOW && !isProvider)
						return true;
					if (type == FlowType.WASTE_FLOW && isProvider)
						return true;
				} else if (side == Side.OUTPUT) {
					if (type == FlowType.PRODUCT_FLOW && isProvider)
						return true;
					if (type == FlowType.WASTE_FLOW && !isProvider)
						return true;
				}
			}
			return false;
		}

		static boolean canExpand(ProcessNode node, Side side) {
			ProductSystemNode sysNode = node.parent();
			MutableProcessLinkSearchMap linkSearch = sysNode.linkSearch;
			long processId = node.process.id;
			for (ProcessLink link : linkSearch.getLinks(processId)) {
				FlowType type = sysNode.flows.type(link.flowId);
				boolean isProvider = link.providerId == processId;
				if (side == ProcessNode.Side.INPUT) {
					if (type == FlowType.PRODUCT_FLOW && !isProvider)
						return true;
					if (type == FlowType.WASTE_FLOW && isProvider)
						return true;
				} else if (side == ProcessNode.Side.OUTPUT) {
					if (type == FlowType.PRODUCT_FLOW && isProvider)
						return true;
					if (type == FlowType.WASTE_FLOW && !isProvider)
						return true;
				}
			}
			return false;
		}

		private void expand() {
			createNecessaryNodes();
			expanded = true;

			// set expanded nodes visible
			List<ProcessNode> nodes = new ArrayList<>();
			for (Link link : links) {
				ProcessNode match = getMatchingNode(link);
				if (match == null || nodes.contains(match))
					continue;
				match.setVisible(true);
				nodes.add(match);
			}
			// then the links of the nodes because
			// there visibility depends on the
			// visibility of the nodes
			for (ProcessNode n : nodes) {
				for (Link link : n.links) {
					link.updateVisibility();
				}
			}
		}

		private void collapse(ProcessNode initialNode) {
			if (isCollapsing)
				return;
			isCollapsing = true;
			// need to copy the links otherwise we get a
			// concurrent modification exception
			var links = ProcessNode.this.links.toArray(new Link[0]);
			for (var link : links) {
				var thisNode = side == Side.INPUT
					? link.inputNode
					: link.outputNode;
				var otherNode = side == Side.INPUT
					? link.outputNode
					: link.inputNode;
				if (!thisNode.equals(ProcessNode.this))
					continue;
				link.unlink();
				otherNode.collapse(Side.INPUT, initialNode);
				otherNode.collapse(Side.OUTPUT, initialNode);
				if (otherNode.equals(initialNode))
					continue;
				if (!otherNode.links.isEmpty())
					continue;
				ProcessNode.this.parent().remove(otherNode);
			}
			isCollapsing = false;
			expanded = false;
		}

		private void createNecessaryNodes() {
			ProductSystemNode sysNode = parent();
			long processID = process.id;
			List<ProcessLink> links = sysNode.linkSearch.getLinks(processID);
			for (ProcessLink pLink : links) {
				FlowType type = sysNode.flows.type(pLink.flowId);
				if (type == null || type == FlowType.ELEMENTARY_FLOW)
					continue;
				boolean isProvider = processID == pLink.providerId;
				long otherID = isProvider ? pLink.processId : pLink.providerId;
				ProcessNode outNode;
				ProcessNode inNode;
				if (isInputNode(type, isProvider)) {
					inNode = ProcessNode.this;
					outNode = sysNode.getOrCreateProcessNode(otherID);
					sysNode.add(outNode);
				} else if (isOutputNode(type, isProvider)) {
					outNode = ProcessNode.this;
					inNode = parent().getOrCreateProcessNode(otherID);
					sysNode.add(outNode);
				} else {
					continue;
				}
				Link link = new Link();
				link.outputNode = outNode;
				link.inputNode = inNode;
				link.processLink = pLink;
				link.link();
			}
		}

		private ProcessNode getMatchingNode(Link link) {
			ProcessNode source = link.outputNode;
			ProcessNode target = link.inputNode;
			if (side == Side.INPUT)
				if (target.equals(ProcessNode.this))
					if (!source.equals(ProcessNode.this))
						return source;
			if (side == Side.OUTPUT)
				if (source.equals(ProcessNode.this))
					if (!target.equals(ProcessNode.this))
						return target;
			return null;
		}

		private boolean isInputNode(FlowType type, boolean isProvider) {
			if (side != Side.INPUT)
				return false;
			if (isProvider && type == FlowType.WASTE_FLOW)
				return true; // waste input
			return !isProvider && type == FlowType.PRODUCT_FLOW; // product input
		}

		private boolean isOutputNode(FlowType type, boolean isProvider) {
			if (side != Side.OUTPUT)
				return false;
			if (isProvider && type == FlowType.PRODUCT_FLOW)
				return true; // product output
			return !isProvider && type == FlowType.WASTE_FLOW; // waste output
		}
	}
}
