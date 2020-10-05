package org.openlca.app.cloud.ui.compare.json.viewer;

import java.util.List;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.openlca.app.cloud.ui.compare.json.JsonNode;
import org.openlca.app.cloud.ui.compare.json.viewer.label.IJsonNodeLabelProvider;
import org.openlca.app.cloud.ui.compare.json.viewer.label.JsonTreeLabelProvider;
import org.openlca.app.cloud.ui.compare.json.viewer.label.TextDiffDialog;
import org.openlca.app.cloud.ui.diff.ActionType;
import org.openlca.app.cloud.ui.diff.Site;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.AbstractViewer;
import org.openlca.app.viewers.Viewers;

public class JsonTreeViewer extends AbstractViewer<JsonNode, TreeViewer> {

	private Site site;
	ActionType action;

	public JsonTreeViewer(Composite parent, Site site, ActionType action) {
		super(parent, site);
		this.site = site;
		this.action = action;
	}

	@Override
	public TreeViewer getViewer() {
		return super.getViewer();
	}

	public void setCounterpart(JsonTreeViewer counterpart) {
		TreeViewer viewer = counterpart.getViewer();
		getViewer().addTreeListener(new ExpansionListener(viewer));
		getViewer().addSelectionChangedListener(new SelectionChangedListener(viewer));
		ScrollBar vBar = getViewer().getTree().getVerticalBar();
		vBar.addSelectionListener(new ScrollListener(viewer));
	}

	public void setLabelProvider(IJsonNodeLabelProvider labelProvider) {
		getViewer().setLabelProvider(new JsonTreeLabelProvider(labelProvider, site));
	}

	@Override
	protected TreeViewer createViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent, SWT.MULTI | SWT.NO_FOCUS | SWT.HIDE_SELECTION | SWT.BORDER);
		viewer.setContentProvider(new ContentProvider());
		Tree tree = viewer.getTree();
		if (viewerParameters[0] == Site.LOCAL)
			tree.getVerticalBar().setVisible(false);
		UI.gridData(tree, true, true);
		viewer.addDoubleClickListener((e) -> onDoubleClick(e));
		return viewer;
	}

	private void onDoubleClick(DoubleClickEvent e) {
		IStructuredSelection sel = (IStructuredSelection) e.getSelection();
		JsonNode node = (JsonNode) sel.getFirstElement();
		if (!node.getElement().isJsonPrimitive())
			return;
		new TextDiffDialog(node, action).open();
	}

	public List<JsonNode> getSelection() {
		return Viewers.getAllSelected(getViewer());
	}

	@Override
	public void setInput(JsonNode[] input) {
		super.setInput(input);
	}

}