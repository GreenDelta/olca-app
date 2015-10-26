package org.openlca.app.cloud.ui.compare;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.GridData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.ImageManager;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Info;
import org.openlca.app.util.UI;

import com.google.gson.JsonObject;

class DiffEditor extends Composite {

	private FormToolkit toolkit;
	private Node root;
	private ModelTree localTree;
	private ModelTree remoteTree;
	private List<Node> nodesAsList = new ArrayList<Node>();
	private boolean editMode;

	static DiffEditor forEditing(Composite parent, JsonObject localJson,
			JsonObject remoteJson, JsonObject mergedJson) {
		return forEditing(parent, null, localJson, remoteJson, mergedJson);
	}

	static DiffEditor forEditing(Composite parent, FormToolkit toolkit,
			JsonObject localJson, JsonObject remoteJson, JsonObject mergedJson) {
		DiffEditor editor = new DiffEditor(parent, toolkit);
		editor.editMode = true;
		editor.initialize(localJson, remoteJson, mergedJson);
		return editor;
	}

	static DiffEditor forViewing(Composite parent, JsonObject localJson,
			JsonObject remoteJson) {
		return forViewing(parent, null, localJson, remoteJson);
	}

	static DiffEditor forViewing(Composite parent, FormToolkit toolkit,
			JsonObject localJson, JsonObject remoteJson) {
		DiffEditor editor = new DiffEditor(parent, toolkit);
		editor.editMode = false;
		editor.initialize(localJson, remoteJson, null);
		return editor;
	}

	private DiffEditor(Composite parent, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
	}

	private void initialize(JsonObject localJson, JsonObject remoteJson,
			JsonObject mergedJson) {
		UI.gridLayout(this, 1, 0, 0);
		if (localJson != null && remoteJson != null && editMode)
			createMenubar();
		createTreeParts(localJson, remoteJson, mergedJson);
		if (toolkit != null)
			toolkit.adapt(this);
	}

	private void createMenubar() {
		Composite container = new Composite(this, SWT.NONE);
		UI.gridLayout(container, 7, 0, 5);
		UI.gridData(container, true, false);
		Composite dummy = new Composite(container, SWT.NONE);
		UI.gridData(dummy, true, false).heightHint = 1;
		createButton(container, ImageType.COPY_SELECTED_CHANGE,
				"#Copy selection from right to left", this::copySelection);
		createButton(container, ImageType.COPY_ALL_CHANGES,
				"#Copy all from right to left", this::copyAll);
		createButton(container, ImageType.RESET_SELECTED_CHANGE,
				"#Reset selection", this::resetSelection);
		createButton(container, ImageType.RESET_ALL_CHANGES, "#Reset all",
				this::resetAll);
		createButton(container, ImageType.NEXT_CHANGE, "#Select next",
				this::selectNext);
		createButton(container, ImageType.PREVIOUS_CHANGE, "#Select previous",
				this::selectPrevious);
		if (toolkit != null)
			toolkit.adapt(container);
		if (toolkit != null)
			toolkit.adapt(dummy);
	}

	private Button createButton(Composite container, ImageType imageType,
			String tooltipText, Runnable onSelect) {
		Button button = new Button(container, SWT.FLAT | SWT.NO_FOCUS
				| SWT.HIDE_SELECTION);
		button.setImage(ImageManager.getImage(imageType));
		button.setToolTipText(tooltipText);
		button.setData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		Controls.onSelect(button, (e) -> onSelect.run());
		if (toolkit != null)
			toolkit.adapt(button, false, false);
		return button;
	}

	private void createTreeParts(JsonObject localJson, JsonObject remoteJson,
			JsonObject mergedJson) {
		Composite container = new Composite(this, SWT.BORDER);
		UI.gridLayout(container, 3, 0, 0);
		UI.gridData(container, true, true);
		root = Node.create(null, null, localJson, remoteJson, mergedJson);
		new NodeBuilder().build(root, localJson, remoteJson);
		new NodeSorter().sort(root);
		addChildrenToList(root);
		localTree = createTreePart(container, "#Local model", root, true);
		UI.gridData(new Label(container, SWT.VERTICAL | SWT.SEPARATOR), false,
				true);
		remoteTree = createTreePart(container, "#Remote model", root, false);
		localTree.setCounterpart(remoteTree);
		remoteTree.setCounterpart(localTree);
		if (toolkit != null)
			toolkit.adapt(container);
	}

	private ModelTree createTreePart(Composite container, String label,
			Node root, boolean local) {
		Composite localComposite = UI.formComposite(container, toolkit);
		UI.gridLayout(localComposite, 1, 0, 0);
		UI.gridData(localComposite, true, true);
		ModelTree tree = new ModelTree(localComposite, local);
		tree.setInput(new Node[] { root });
		return tree;
	}

	private void addChildrenToList(Node node) {
		for (Node child : node.children) {
			nodesAsList.add(child);
			addChildrenToList(child);
		}
	}

	private void copySelection() {
		List<Node> selection = localTree.getSelection();
		for (Node node : selection)
			if (!node.hasEqualValues())
				node.copyRemoteValue();
		localTree.refresh();
		remoteTree.refresh();
	}

	private void copyAll() {
		for (Node node : root.children)
			if (!node.hasEqualValues())
				node.copyRemoteValue();
		localTree.refresh();
		remoteTree.refresh();
	}

	private void resetSelection() {
		List<Node> selection = localTree.getSelection();
		for (Node node : selection)
			if (node.hasEqualValues())
				node.reset();
		localTree.refresh();
		remoteTree.refresh();
	}

	private void resetAll() {
		root.reset();
		localTree.refresh();
		remoteTree.refresh();
	}

	private void selectNext() {
		List<Node> selection = localTree.getSelection();
		int selectedIndex = -1;
		if (!selection.isEmpty())
			selectedIndex = nodesAsList
					.indexOf(selection.get(selection.size() - 1));
		Node select = null;
		for (int i = selectedIndex + 1; i < nodesAsList.size(); i++)
			if (!nodesAsList.get(i).hasEqualValues()) {
				select = nodesAsList.get(i);
				break;
			}
		if (select == null && selectedIndex > 0)
			for (int i = 0; i < selectedIndex; i++)
				if (!nodesAsList.get(i).hasEqualValues()) {
					select = nodesAsList.get(i);
					break;
				}
		if (select != null)
			localTree.select(select);
		else
			Info.showPopup("No more changes found");
	}

	private void selectPrevious() {
		List<Node> selection = localTree.getSelection();
		int selectedIndex = nodesAsList.size();
		if (!selection.isEmpty())
			selectedIndex = nodesAsList
					.indexOf(selection.get(selection.size() - 1));
		Node select = null;
		for (int i = selectedIndex - 1; i > 0; i--)
			if (!nodesAsList.get(i).hasEqualValues()) {
				select = nodesAsList.get(i);
				break;
			}
		if (select == null && selectedIndex < nodesAsList.size() - 1)
			for (int i = nodesAsList.size() - 1; i > selectedIndex; i--)
				if (!nodesAsList.get(i).hasEqualValues()) {
					select = nodesAsList.get(i);
					break;
				}
		if (select != null)
			localTree.select(select);
		else
			Info.showPopup("No more changes found");
	}

	Node getRootNode() {
		return root;
	}

}
