package org.openlca.app.results.analysis.groups;


import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.components.CategoryDialog;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;
import org.openlca.util.Strings;

class EpdDialog extends FormDialog {

	private final ProductSystem system;
	private final List<ImpactGroupResult> results;

	private String name;
	private Category category;

	static void open(
		ProductSystem system, List<ImpactGroupResult> results
	) {
		if (system == null || results == null || results.isEmpty()) {
			MsgBox.error("Result is empty");
			return;
		}
		new EpdDialog(system, results).open();
	}

	private EpdDialog(
		ProductSystem system, List<ImpactGroupResult> results
	) {
		super(UI.shell());
		this.system = system;
		this.results = results;

		if (Strings.nullOrEmpty(system.name)) {
			name = "New EPD";
		} else {
			name = system.name;
			var p = system.referenceProcess;
			if (p != null
					&& p.location != null
					&& Strings.notEmpty(p.location.code)) {
				name += " - " + p.location.code;
			}
		}
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Save as EPD");
	}

	@Override
	protected Point getInitialSize() {
		return UI.initialSizeOf(this, 600, 450);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);

		var top = UI.composite(body, tk);
		UI.gridLayout(top, 3, 10, 0);
		UI.gridData(top, true, true);

		// name
		var nameText = UI.labeledText(top, tk, M.Name);
		Controls.onPainted(nameText, () -> {
			if (name != null) {
				nameText.setText(name);
			}
		});
		UI.filler(top, tk);

		// category
		var categoryText = UI.labeledText(top, tk, M.Category);
		categoryText.setEditable(false);
		var categoryBtn = tk.createButton(top, M.Browse, SWT.NONE);
		Controls.onSelect(categoryBtn, $ -> {
			var selection = CategoryDialog.selectFor(ModelType.EPD);
			if (selection.isCancelled())
				return;
			category = selection.isEmpty()
					? null
					: selection.category();
			var path = category != null
					? category.toPath()
					: "";
			categoryText.setText(path);
		});

		// list with results
		var resultLabel = UI.label(top, tk, M.Results);
		UI.gridData(resultLabel, false, false).verticalAlignment = SWT.TOP;
		var resultList = new org.eclipse.swt.widgets.List(top, SWT.BORDER);
		UI.gridData(resultList, true, true);

		nameText.addModifyListener($ -> {
			name = nameText.getText();
			updateResultNames(resultList);
		});
		Controls.onPainted(
				resultList, () -> updateResultNames(resultList));

	}

	private void updateResultNames(org.eclipse.swt.widgets.List resultList) {
		var names = system.analysisGroups.stream()
			.map(g -> name + " - " + g.name)
			.toArray(String[]::new);
		resultList.setItems(names);
	}

	@Override
	protected void okPressed() {
		if (Strings.nullOrEmpty(name)) {
			MsgBox.error(
				"Name is empty",
				"The name of the EPD cannot be empty.");
			return;
		}
		super.okPressed();
		// TODO: Build the EPD
	}
}
