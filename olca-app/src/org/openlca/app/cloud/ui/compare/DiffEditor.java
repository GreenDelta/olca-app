package org.openlca.app.cloud.ui.compare;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.GridData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.ImageManager;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Info;
import org.openlca.app.util.UI;

class DiffEditor extends Composite {

	private FormToolkit toolkit;
	private JsonNode root;
	private ModelTree localTree;
	private ModelTree remoteTree;
	private List<JsonNode> nodesAsList = new ArrayList<JsonNode>();
	private boolean editMode;

	static DiffEditor forEditing(Composite parent, JsonNode root) {
		return forEditing(parent, null, root);
	}

	static DiffEditor forEditing(Composite parent, FormToolkit toolkit,
			JsonNode root) {
		DiffEditor editor = new DiffEditor(parent, toolkit, root);
		editor.editMode = true;
		editor.initialize();
		return editor;
	}

	static DiffEditor forViewing(Composite parent, JsonNode root) {
		return forViewing(parent, null, root);
	}

	static DiffEditor forViewing(Composite parent, FormToolkit toolkit,
			JsonNode root) {
		DiffEditor editor = new DiffEditor(parent, toolkit, root);
		editor.editMode = false;
		editor.initialize();
		return editor;
	}

	private DiffEditor(Composite parent, FormToolkit toolkit, JsonNode root) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
		this.root = root;
	}

	private void initialize() {
		UI.gridLayout(this, 1, 0, 0);
		if (editMode && root.getLocalElement() != null
				&& root.getRemoteElement() != null)
			createMenubar();
		createTreeParts();
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

	private void createTreeParts() {
		Composite container = new Composite(this, SWT.BORDER);
		GridLayout layout = UI.gridLayout(container, 2, 0, 0);
		layout.makeColumnsEqualWidth = true;
		UI.gridData(container, true, true);
		addChildrenToList(root);
		localTree = createTreePart(container, "#Local model", root, true);
		remoteTree = createTreePart(container, "#Remote model", root, false);
		localTree.setCounterpart(remoteTree);
		remoteTree.setCounterpart(localTree);
		if (toolkit != null)
			toolkit.adapt(container);
	}

	private ModelTree createTreePart(Composite container, String label,
			JsonNode root, boolean local) {
		Composite localComposite = UI.formComposite(container, toolkit);
		UI.gridLayout(localComposite, 1, 0, 0);
		UI.gridData(localComposite, true, true);
		ModelTree tree = new ModelTree(localComposite, local);
		tree.setInput(new JsonNode[] { root });
		return tree;
	}

	private void addChildrenToList(JsonNode node) {
		for (JsonNode child : node.children) {
			nodesAsList.add(child);
			addChildrenToList(child);
		}
	}

	private void copySelection() {
		List<JsonNode> selection = localTree.getSelection();
		for (JsonNode node : selection)
			if (!node.hasEqualValues())
				node.copyRemoteValue();
		localTree.refresh();
		remoteTree.refresh();
	}

	private void copyAll() {
		for (JsonNode node : root.children)
			if (!node.hasEqualValues())
				node.copyRemoteValue();
		localTree.refresh();
		remoteTree.refresh();
	}

	private void resetSelection() {
		List<JsonNode> selection = localTree.getSelection();
		for (JsonNode node : selection)
			node.reset();
		localTree.refresh();
		remoteTree.refresh();
	}

	private void resetAll() {
		for (JsonNode node : root.children)
			node.reset();
		localTree.refresh();
		remoteTree.refresh();
	}

	private void selectNext() {
		List<JsonNode> selection = localTree.getSelection();
		int selectedIndex = -1;
		if (!selection.isEmpty())
			selectedIndex = nodesAsList
					.indexOf(selection.get(selection.size() - 1));
		JsonNode select = null;
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
		List<JsonNode> selection = localTree.getSelection();
		int selectedIndex = nodesAsList.size();
		if (!selection.isEmpty())
			selectedIndex = nodesAsList
					.indexOf(selection.get(selection.size() - 1));
		JsonNode select = null;
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

	JsonNode getRootNode() {
		return root;
	}

}
