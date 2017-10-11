package org.openlca.app.cloud.ui.compare.json;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Direction;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer.Side;
import org.openlca.app.cloud.ui.compare.json.viewer.label.IJsonNodeLabelProvider;
import org.openlca.app.util.UI;

public class DiffEditor extends Composite {

	private FormToolkit toolkit;
	private JsonTreeViewer leftTree;
	private JsonTreeViewer rightTree;
	private JsonNode root;
	private boolean editMode;
	private String leftLabel = M.LocalModel;
	private String rightLabel = M.RemoteModel;

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

	public void setLabels(String left, String right) {
		this.leftLabel = left;
		this.rightLabel = right;
	}

	public void initialize(JsonNode root, IJsonNodeLabelProvider labelProvider, IDependencyResolver dependencyResolver,
			Direction direction) {
		UI.gridLayout(this, 1, 0, 0);
		MenuBar menu = null;
		if (editMode && root != null && root.leftElement != null && root.rightElement != null)
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
		setInput(root);
		if (toolkit == null)
			return;
		toolkit.adapt(this);
		if (menu != null)
			menu.apply(toolkit);
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

	private void createTreeParts(Direction direction) {
		Composite comp = new Composite(this, SWT.BORDER);
		GridLayout layout = UI.gridLayout(comp, 2, 0, 0);
		layout.makeColumnsEqualWidth = true;
		UI.gridData(comp, true, true).widthHint = 1;
		leftTree = createTree(comp, leftLabel, Side.LEFT, direction);
		rightTree = createTree(comp, rightLabel, Side.RIGHT, direction);
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
