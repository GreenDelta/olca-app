package org.openlca.app.cloud.ui.compare.json;

import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.GridData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.cloud.ui.compare.json.viewer.JsonTreeViewer;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;

class MenuBar extends Composite {

	private JsonNode root;
	private Button copySelectionButton;
	private Button copyAllButton;
	private Button resetSelectionButton;
	private Button resetAllButton;
	private Button nextChangeButton;
	private Button previousChangeButton;

	MenuBar(Composite parent, IDependencyResolver dependencyResolver) {
		super(parent, SWT.NONE);
		createLayout();
		createButtons();
	}

	private void createLayout() {
		UI.gridLayout(this, 7, 0, 5);
		UI.gridData(this, true, false);
		Composite dummy = new Composite(this, SWT.NONE);
		UI.gridData(dummy, true, false).heightHint = 1;
	}

	private void createButtons() {
		copySelectionButton = createButton(Icon.COPY_SELECTED_CHANGE,
				M.CopySelectionFromRightToLeft);
		copyAllButton = createButton(Icon.COPY_ALL_CHANGES,
				M.CopyAllFromRightToLeft);
		resetSelectionButton = createButton(Icon.RESET_SELECTED_CHANGE,
				M.ResetSelection);
		resetAllButton = createButton(Icon.RESET_ALL_CHANGES, M.ResetAll);
		nextChangeButton = createButton(Icon.NEXT_CHANGE, M.SelectNext);
		previousChangeButton = createButton(Icon.PREVIOUS_CHANGE,
				M.SelectPrevious);
	}

	private Button createButton(Icon icon, String tooltipText) {
		Button button = new Button(this, SWT.FLAT | SWT.NO_FOCUS
				| SWT.HIDE_SELECTION);
		button.setImage(icon.get());
		button.setToolTipText(tooltipText);
		button.setData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		return button;
	}

	void apply(FormToolkit toolkit) {
		toolkit.adapt(this);
		for (Control comp : getChildren())
			toolkit.adapt(comp, false, false);
	}

	void initActions(JsonNode root, JsonTreeViewer left, JsonTreeViewer right, IDependencyResolver dependencyResolver) {
		this.root = root;
		MenuBarActions actions = new MenuBarActions(root, left, right, dependencyResolver);
		Controls.onSelect(copySelectionButton, (e) -> actions.copySelection());
		Controls.onSelect(copyAllButton, (e) -> actions.copyAll());
		Controls.onSelect(resetSelectionButton, (e) -> actions.resetSelection());
		Controls.onSelect(resetAllButton, (e) -> actions.resetAll());
		Controls.onSelect(nextChangeButton, (e) -> actions.selectNext());
		Controls.onSelect(previousChangeButton, (e) -> actions.selectPrevious());
	}

	void updateButtons(SelectionChangedEvent e) {
		List<JsonNode> nodes = getSelection(e);
		updateSelectionButtons(nodes);
		boolean rootsEqual = areRootsEqual();
		copyAllButton.setEnabled(!rootsEqual);
		resetAllButton.setEnabled(root.hadDifferences());
		nextChangeButton.setEnabled(!rootsEqual);
		previousChangeButton.setEnabled(!rootsEqual);
	}

	private void updateSelectionButtons(List<JsonNode> nodes) {
		copySelectionButton.setEnabled(false);
		resetSelectionButton.setEnabled(false);
		for (JsonNode node : nodes)
			if (!node.readOnly)
				if (!node.hasEqualValues())
					copySelectionButton.setEnabled(true);
				else if (node.hadDifferences())
					resetSelectionButton.setEnabled(true);
	}

	private List<JsonNode> getSelection(SelectionChangedEvent e) {
		if (e == null)
			return Collections.emptyList();
		return Viewers.getAll((IStructuredSelection) e.getSelection());
	}

	private boolean areRootsEqual() {
		for (JsonNode node : root.children)
			if (!node.hasEqualValues())
				return false;
		return true;
	}

}
