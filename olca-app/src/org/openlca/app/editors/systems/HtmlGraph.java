package org.openlca.app.editors.systems;

import gnu.trove.list.linked.TLongLinkedList;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.database.EntityCache;
import org.openlca.core.matrix.LongIndex;
import org.openlca.core.matrix.ProcessLinkSearchMap;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

class HtmlGraph extends FormPage implements HtmlPage {

	private Logger log = LoggerFactory.getLogger(getClass());

	private ProductSystem productSystem;
	private Browser browser;

	public HtmlGraph(FormEditor editor, ProductSystem productSystem) {
		super(editor, "system.HtmlGraph", "HTML Graph");
		this.productSystem = productSystem;
	}

	@Override
	public String getUrl() {
		return HtmlView.GRAPH_VIEW.getUrl();
	}

	@Override
	public void onLoaded() {
		try {
			String graph = buildGraph().toJson();
			String command = "setData(" + graph + ")";
			browser.evaluate(command);
		} catch (Exception e) {
			log.error("failed to set graph data", e);
		}
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "HTML Graph");
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		body.setLayout(new FillLayout());
		browser = UI.createBrowser(body, this);
		form.reflow(true);
	}

	private Graph buildGraph() {
		Graph graph = new Graph();
		LongIndex nodeIndex = createNodes(graph);
		ProcessLinkSearchMap map = new ProcessLinkSearchMap(
				productSystem.getProcessLinks());
		long startId = productSystem.getReferenceProcess().getId();
		TLongLinkedList queue = new TLongLinkedList();
		queue.add(startId);
		TLongHashSet handled = new TLongHashSet();
		while (!queue.isEmpty()) {
			long recipient = queue.removeAt(0);
			handled.add(recipient);
			int recipId = nodeIndex.getIndex(recipient);
			for (ProcessLink link : map.getIncomingLinks(recipient)) {
				long provider = link.getProviderId();
				if (handled.contains(provider) || queue.contains(provider))
					continue;
				queue.add(provider);
				int provId = nodeIndex.getIndex(provider);
				Link lnk = new Link();
				lnk.source = provId;
				lnk.target = recipId;
				graph.links.add(lnk);
			}
		}
		return graph;
	}

	private LongIndex createNodes(Graph graph) {
		LongIndex idx = new LongIndex();
		EntityCache cache = Cache.getEntityCache();
		Map<Long, ProcessDescriptor> descriptors = cache.getAll(
				ProcessDescriptor.class, productSystem.getProcesses());
		for (Long processId : productSystem.getProcesses()) {
			Node node = createNode(processId, descriptors);
			graph.nodes.add(node);
			idx.put(processId);
		}
		return idx;
	}

	private Node createNode(Long processId,
			Map<Long, ProcessDescriptor> descriptors) {
		Node node = new Node();
		ProcessDescriptor descriptor = descriptors.get(processId);
		if (descriptor != null) {
			node.name = Labels.getDisplayName(descriptor);
			Long group = descriptor.getCategory();
			node.group = group == null ? 0 : group.intValue();
		} else {
			node.name = "unknown";
			node.group = 0;
		}
		return node;
	}

	@SuppressWarnings("unused")
	private class Node {
		String name;
		int group;
	}

	private class Link {
		int source;
		int target;
	}

	private class Graph {
		List<Node> nodes = new ArrayList<>();
		List<Link> links = new ArrayList<>();

		String toJson() {
			Gson gson = new Gson();
			return gson.toJson(this);
		}
	}
}
