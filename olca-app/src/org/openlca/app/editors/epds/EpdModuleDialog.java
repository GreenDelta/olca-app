package org.openlca.app.editors.epds;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Selections;
import org.openlca.core.model.Epd;
import org.openlca.core.model.EpdModule;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Result;

import java.util.Optional;

class EpdModuleDialog extends FormDialog {

	private final EpdModule module;
	private final String[] names = {
		"A1A2A3", "A1", "A2", "A3", "A4", "A5", "B1", "B2toB7", "B2",
		"B3", "B4", "B5", "B6", "B7", "C1", "C2", "C3", "C4", "D"};

	public static Optional<EpdModule> createNew(Epd epd) {
		if (epd == null)
			return Optional.empty();
		var module = new EpdModule();
		var dialog = new EpdModuleDialog(module);
		module.name = dialog.proposeName(epd);
		int ret = dialog.open();
		return ret == OK
			? Optional.of(module)
			: Optional.empty();
	}

	public static boolean edit(EpdModule module) {
		return module != null && new EpdModuleDialog(module).open() == OK;
	}

	private EpdModuleDialog(EpdModule module) {
		super(UI.shell());
		this.module = module;
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

		var nameComp = tk.createComposite(body);
		UI.fillHorizontal(nameComp);
		UI.gridLayout(nameComp, 2, 10, 0);
		UI.formLabel(nameComp, tk, "Name:");
		var nameCombo = new Combo(nameComp, SWT.BORDER);
		nameCombo.setItems(names);
		if (module.name != null) {
			nameCombo.setText(module.name);
		}

		nameCombo.addModifyListener($ -> {
			module.name = nameCombo.getText();
			System.out.println(module.name);
		});

		UI.formLabel(nameComp, tk, "Result:");
		var tree = NavigationTree.forSingleSelection(body, ModelType.RESULT);
		tree.addSelectionChangedListener(e -> {
			var selection = Selections.firstOf(e);
			if (selection instanceof ModelElement elem) {
				var descriptor = elem.getContent();
				module.result = Database.get().get(Result.class, descriptor.id);
			}
		});
	}

	private String proposeName(Epd epd) {
		if (epd == null || epd.modules.isEmpty())
			return names[0];
		for (var name : names) {
			boolean found = false;
			for (var mod : epd.modules) {
				if (name.equalsIgnoreCase(mod.name)) {
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
