package org.openlca.app.collaboration.viewers.json;

import org.eclipse.jgit.diff.DiffEntry.Side;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.collaboration.viewers.json.content.IDependencyResolver;
import org.openlca.app.collaboration.viewers.json.content.JsonNode;
import org.openlca.app.collaboration.viewers.json.label.IJsonNodeLabelProvider;
import org.openlca.app.util.UI;

public class JsonCompareViewer extends Composite {

	private final FormToolkit toolkit;
	private JsonNode root;
	private final boolean canMerge;
	private JsonViewer leftTree;
	private JsonViewer rightTree;

	public static JsonCompareViewer forMerging(Composite parent, FormToolkit toolkit, JsonNode root) {
		return new JsonCompareViewer(parent, toolkit, root, true);
	}

	public static JsonCompareViewer forComparison(Composite parent, FormToolkit toolkit, JsonNode root) {
		return new JsonCompareViewer(parent, toolkit, root, false);
	}

	private JsonCompareViewer(Composite parent, FormToolkit toolkit, JsonNode root, boolean canMerge) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
		this.root = root;
		this.canMerge = canMerge;
	}

	public void initialize(IJsonNodeLabelProvider labelProvider, IDependencyResolver dependencyResolver) {
		UI.gridLayout(this, 1, 0, 0);
		MenuBar menu = null;
		if (canMerge && root != null && root.left != null && root.right != null) {
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
		leftTree = createTree(comp, Side.OLD);
		rightTree = createTree(comp, Side.NEW);
		leftTree.setCounterpart(rightTree);
		rightTree.setCounterpart(leftTree);
		leftTree.setLabelProvider(labelProvider);
		rightTree.setLabelProvider(labelProvider);
		if (toolkit != null) {
			toolkit.adapt(comp);
		}
	}

	private JsonViewer createTree(Composite container, Side side) {
		var composite = UI.formComposite(container, toolkit);
		UI.gridLayout(composite, 1, 0, 0);
		UI.gridData(composite, true, true);
		UI.formLabel(composite, toolkit, side == Side.OLD ? "Existing or previous model" : "Updated model");
		return new JsonViewer(composite, side);
	}

}
