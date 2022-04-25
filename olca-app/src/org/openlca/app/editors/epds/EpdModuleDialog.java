package org.openlca.app.editors.epds;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Selections;
import org.openlca.core.model.Epd;
import org.openlca.core.model.EpdModule;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Result;
import org.openlca.util.Strings;

/**
 * Edit an EPD module. Note that this dialog always works on a new module or
 * a copy of an existing module.
 */
class EpdModuleDialog extends FormDialog {

	private final Epd epd;
	private final EpdModule module;

	// if the dialog is in edit-mode, it works on a copy and this field
// contains the original module then.
	private final EpdModule origin;

	private final String[] names = {
		"A1A2A3", "A1", "A2", "A3", "A4", "A5", "B1", "B2toB7", "B2",
		"B3", "B4", "B5", "B6", "B7", "C1", "C2", "C3", "C4", "D"};

	public static Optional<EpdModule> createNew(Epd epd) {
		if (epd == null)
			return Optional.empty();
		var module = new EpdModule();
		var dialog = new EpdModuleDialog(epd, module, null);
		module.name = dialog.proposeName(epd);
		int ret = dialog.open();
		return ret == OK
			? Optional.of(module)
			: Optional.empty();
	}

	public static boolean edit(Epd epd, EpdModule origin) {
		if (origin == null)
			return false;
		var copy = origin.copy();
		var dialog = new EpdModuleDialog(epd, copy, origin);
		if (dialog.open() != OK)
			return false;
		origin.name = copy.name;
		origin.result = copy.result;
		origin.multiplier = copy.multiplier;
		return true;
	}

	private EpdModuleDialog(
		Epd epd, EpdModule module, EpdModule origin) {
		super(UI.shell());
		this.epd = epd;
		this.module = module;
		this.origin = origin;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit EPD module");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 500);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.formBody(mForm.getForm(), tk);
		var comp = tk.createComposite(body);
		UI.fillHorizontal(comp);
		UI.gridLayout(comp, 2, 10, 0);

		// module name
		UI.formLabel(comp, tk, "Name:");
		var nameCombo = new Combo(comp, SWT.BORDER);
		UI.fillHorizontal(nameCombo);
		nameCombo.setItems(proposeNames());
		if (module.name != null) {
			nameCombo.setText(module.name);
		}
		nameCombo.addModifyListener($ -> module.name = nameCombo.getText());

		// result multiplier
		var factorText = UI.formText(comp, tk, "Result multiplier:");
		Controls.set(factorText, module.multiplier, f -> module.multiplier = f);

		// result tree
		UI.formLabel(comp, tk, "Result:");
		var tree = NavigationTree.forSingleSelection(body, ModelType.RESULT);
		UI.gridData(tree.getTree(), true, true);
		tree.addSelectionChangedListener(e -> {
			var selection = Selections.firstOf(e);
			if (selection instanceof ModelElement elem) {
				var descriptor = elem.getContent();
				module.result = Database.get().get(Result.class, descriptor.id);
			}
		});
	}

	@Override
	protected void okPressed() {
		// check that the module name is not empty
		if (Strings.nullOrEmpty(module.name)) {
			MsgBox.error("Empty name",
				"An empty name is not allowed for an EPD module.");
			return;
		}

		// check that the module-result pair is unique within the EPD
		for (var other : epd.modules) {
			if (Objects.equals(other, module)
				|| Objects.equals(other, origin))
				continue;
			if (Strings.nullOrEqual(other.name, module.name)
				&& Objects.equals(other.result, module.result)) {
				MsgBox.error("Duplicate module",
					"Module " + module.name + " already exists.");
				return;
			}
		}

		super.okPressed();
	}

	private String[] proposeNames() {
		var used = epd.modules.stream()
			.map(mod -> mod.name)
			.filter(Strings::notEmpty)
			.collect(Collectors.toSet());
		var filtered = new ArrayList<String>();
		if (origin != null && Strings.notEmpty(origin.name)) {
			filtered.add(origin.name);
		}
		for (var name : names) {
			if (used.contains(name))
				continue;
			filtered.add(name);
		}
		return filtered.toArray(String[]::new);
	}

	private String proposeName(Epd epd) {
		if (epd == null || epd.modules.isEmpty())
			return names[0];
		for (var name : names) {
			boolean found = false;
			var s = name.toLowerCase();
			for (var mod : epd.modules) {
				if (mod.name == null)
					continue;
				if (mod.name.toLowerCase().contains(s)) {
					found = true;
					break;
				}
			}
			if (!found)
				return name;
		}
		return names[0];
	}

}
