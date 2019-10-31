package org.openlca.app.cloud.ui.compare.json;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer;
import org.openlca.app.cloud.ui.compare.json.viewer.label.IJsonNodeLabelProvider;
import org.openlca.app.cloud.ui.diff.ActionType;
import org.openlca.app.cloud.ui.diff.Site;
import org.openlca.app.util.UI;

public class DiffEditor extends Composite {

	private FormToolkit toolkit;
	private JsonTreeViewer leftTree;
	private JsonTreeViewer rightTree;
	private JsonNode root;
	private boolean editMode;
	private String localLabel = M.LocalModel;
	private String remoteLabel = M.RemoteModel;

	static DiffEditor forEditing(Composite parent) {
		return forEditing(parent, null);
	}

	static DiffEditor forEditing(Composite parent, FormToolkit toolkit) {
		DiffEditor editor = new DiffEditor(parent, toolkit);
		editor.editMode = true;
		return editor;
	}

	public static DiffEditor forViewing(Composite parent) {
		return forViewing(parent, null);
	}

	public static DiffEditor forViewing(Composite parent, FormToolkit toolkit) {
		DiffEditor editor = new DiffEditor(parent, toolkit);
		editor.editMode = false;
		return editor;
	}

	private DiffEditor(Composite parent, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
	}

	public void setLabels(String local, String remote) {
		this.localLabel = local;
		this.remoteLabel = remote;
	}

	public void initialize(JsonNode root, IJsonNodeLabelProvider labelProvider, IDependencyResolver dependencyResolver,
			ActionType action) {
		UI.gridLayout(this, 1, 0, 0);
		MenuBar menu = null;
		if (editMode && root != null && root.localElement != null && root.remoteElement != null) {
			menu = new MenuBar(this, dependencyResolver);
		}
		createTreeParts(action);
		if (menu != null) {
			rightTree.getViewer().addSelectionChangedListener(
					menu::updateButtons);
			menu.initActions(root, leftTree, rightTree, dependencyResolver);
			menu.updateButtons(null);
		}
		leftTree.setLabelProvider(labelProvider);
		rightTree.setLabelProvider(labelProvider);
		setInput(root);
		if (toolkit == null)
			return;
		toolkit.adapt(this);
		if (menu != null) {
			menu.apply(toolkit);
		}
	}

	public void setInput(JsonNode node) {
		this.root = node;
		if (node == null) {
			leftTree.setInput(new JsonNode[0]);
			rightTree.setInput(new JsonNode[0]);
		} else {
			leftTree.setInput(new JsonNode[] { node });
			rightTree.setInput(new JsonNode[] { node });
		}
	}

	private void createTreeParts(ActionType action) {
		Composite comp = new Composite(this, SWT.BORDER);
		GridLayout layout = UI.gridLayout(comp, 2, 0, 0);
		layout.makeColumnsEqualWidth = true;
		UI.gridData(comp, true, true).widthHint = 1;
		leftTree = createTree(comp, localLabel, Site.LOCAL, action);
		rightTree = createTree(comp, remoteLabel, Site.REMOTE, action);
		leftTree.setCounterpart(rightTree);
		rightTree.setCounterpart(leftTree);
		// one listener is enough since trees are synced
		if (toolkit != null) {
			toolkit.adapt(comp);
		}
	}

	private JsonTreeViewer createTree(Composite container, String label, Site site, ActionType action) {
		Composite composite = UI.formComposite(container, toolkit);
		UI.gridLayout(composite, 1, 0, 0);
		UI.gridData(composite, true, true);
		UI.formLabel(composite, toolkit, label);
		return new JsonTreeViewer(composite, site, action);
	}

	JsonNode getRootNode() {
		return root;
	}

}
