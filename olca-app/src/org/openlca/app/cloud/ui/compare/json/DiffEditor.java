package org.openlca.app.cloud.ui.compare.json;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Side;
import org.openlca.app.cloud.ui.compare.json.viewer.label.IJsonNodeLabelProvider;
import org.openlca.app.util.UI;

class DiffEditor extends Composite {

	private FormToolkit toolkit;
	private JsonTreeViewer leftTree;
	private JsonTreeViewer rightTree;
	private JsonNode root;
	private boolean editMode;

	static DiffEditor forEditing(Composite parent) {
		return forEditing(parent, null);
	}

	static DiffEditor forEditing(Composite parent, FormToolkit toolkit) {
		DiffEditor editor = new DiffEditor(parent, toolkit);
		editor.editMode = true;
		return editor;
	}

	static DiffEditor forViewing(Composite parent) {
		return forViewing(parent, null);
	}

	static DiffEditor forViewing(Composite parent, FormToolkit toolkit) {
		DiffEditor editor = new DiffEditor(parent, toolkit);
		editor.editMode = false;
		return editor;
	}

	private DiffEditor(Composite parent, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
	}

	void initialize(JsonNode root, IJsonNodeLabelProvider labelProvider,
			IDependencyResolver dependencyResolver, Direction direction) {
		this.root = root;
		UI.gridLayout(this, 1, 0, 0);
		MenuBar menu = null;
		if (editMode && root.leftElement != null && root.rightElement != null)
			menu = new MenuBar(this, dependencyResolver);
		createTreeParts(direction);
		if (menu != null) {
			rightTree.getViewer().addSelectionChangedListener(
					menu::updateButtons);
			menu.initActions(root, leftTree, rightTree, dependencyResolver);
			menu.updateButtons(null);
		}
		leftTree.setLabelProvider(labelProvider);
		rightTree.setLabelProvider(labelProvider);
		leftTree.setInput(new JsonNode[] { root });
		rightTree.setInput(new JsonNode[] { root });
		if (toolkit == null)
			return;
		toolkit.adapt(this);
		if (menu != null)
			menu.apply(toolkit);
	}

	private void createTreeParts(Direction direction) {
		Composite comp = new Composite(this, SWT.BORDER);
		GridLayout layout = UI.gridLayout(comp, 2, 0, 0);
		layout.makeColumnsEqualWidth = true;
		UI.gridData(comp, true, true).widthHint = 1;
		leftTree = createTree(comp, "#Local model", Side.LEFT, direction);
		rightTree = createTree(comp, "#Remote model", Side.RIGHT, direction);
		leftTree.setCounterpart(rightTree);
		rightTree.setCounterpart(leftTree);
		// one listener is enough since trees are synced
		if (toolkit != null)
			toolkit.adapt(comp);
	}

	private JsonTreeViewer createTree(Composite container, String label,
			Side side, Direction direction) {
		Composite composite = UI.formComposite(container, toolkit);
		UI.gridLayout(composite, 1, 0, 0);
		UI.gridData(composite, true, true);
		UI.formLabel(composite, toolkit, label);
		return new JsonTreeViewer(composite, side, direction);
	}

	JsonNode getRootNode() {
		return root;
	}
}
