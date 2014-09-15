package org.openlca.app.editors.systems;

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
		LongIndex nodeIndex = new LongIndex();
		Graph graph = new Graph();
		EntityCache cache = Cache.getEntityCache();
		Map<Long, ProcessDescriptor> descriptors = cache.getAll(
				ProcessDescriptor.class, productSystem.getProcesses());
		for (Long processId : productSystem.getProcesses()) {
			Node node = createNode(processId, descriptors);
			graph.nodes.add(node);
			nodeIndex.put(processId);
		}
		for (ProcessLink processLink : productSystem.getProcessLinks()) {
			Link link = new Link();
			link.source = nodeIndex.getIndex(processLink.getProviderId());
			link.target = nodeIndex.getIndex(processLink.getRecipientId());
			if (link.source >= 0 && link.target >= 0)
				graph.links.add(link);
		}
		return graph;
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
