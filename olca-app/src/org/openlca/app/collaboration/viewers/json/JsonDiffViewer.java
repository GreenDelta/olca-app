package org.openlca.app.collaboration.viewers.json;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.collaboration.viewers.json.content.IDependencyResolver;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.collaboration.viewers.json.label.Direction;
import org.openlca.app.collaboration.viewers.json.label.IJsonNodeLabelProvider;
import org.openlca.app.util.UI;

public class JsonDiffViewer extends Composite {

	private final FormToolkit toolkit;
	private JsonNode root;
	private final boolean editMode;
	private final Direction direction;
	private JsonViewer leftTree;
	private JsonViewer rightTree;
	private String localLabel = M.LocalModel;
	private String remoteLabel = M.RemoteModel;

	public static JsonDiffViewer forEditing(Composite parent, FormToolkit toolkit, JsonNode root, Direction direction) {
		return new JsonDiffViewer(parent, toolkit, root, direction, true);
	}

	public static JsonDiffViewer forViewing(Composite parent, FormToolkit toolkit, JsonNode root, Direction direction) {
		return new JsonDiffViewer(parent, toolkit, root, direction, false);
	}

	private JsonDiffViewer(Composite parent, FormToolkit toolkit, JsonNode root, Direction direction,
			boolean editMode) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
		this.root = root;
		this.editMode = editMode;
		this.direction = direction;
	}

	public void setLabels(String local, String remote) {
		this.localLabel = local;
		this.remoteLabel = remote;
	}

	public void initialize(IJsonNodeLabelProvider labelProvider, IDependencyResolver dependencyResolver) {
		UI.gridLayout(this, 1, 0, 0);
		MenuBar menu = null;
		if (editMode && root != null && root.localElement != null && root.remoteElement != null) {
			menu = new MenuBar(this, root);
		}
		createTreeParts(labelProvider);
		if (menu != null) {
			// one listener is enough since trees are synced
			rightTree.getViewer().addSelectionChangedListener(menu::updateButtons);
			menu.initActions(leftTree, rightTree, dependencyResolver);
			menu.updateButtons(null);
		}
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

	private void createTreeParts(IJsonNodeLabelProvider labelProvider) {
		var comp = new Composite(this, SWT.BORDER);
		var layout = UI.gridLayout(comp, 2, 0, 0);
		layout.makeColumnsEqualWidth = true;
		UI.gridData(comp, true, true).widthHint = 1;
		leftTree = createTree(comp, localLabel, Side.LOCAL);
		rightTree = createTree(comp, remoteLabel, Side.REMOTE);
		leftTree.setCounterpart(rightTree);
		rightTree.setCounterpart(leftTree);
		leftTree.setLabelProvider(labelProvider);
		rightTree.setLabelProvider(labelProvider);
		if (toolkit != null) {
			toolkit.adapt(comp);
		}
	}

	private JsonViewer createTree(Composite container, String label, Side side) {
		var composite = UI.formComposite(container, toolkit);
		UI.gridLayout(composite, 1, 0, 0);
		UI.gridData(composite, true, true);
		UI.formLabel(composite, toolkit, label);
		return new JsonViewer(composite, side, direction);
	}

	public boolean leftDiffersFromRight() {
		return root.hasEqualValues();
	}

}
