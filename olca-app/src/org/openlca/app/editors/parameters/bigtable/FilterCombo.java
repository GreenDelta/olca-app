package org.openlca.app.editors.parameters.bigtable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;

class FilterCombo {

	static final int ALL = 0;
	static final int NAMES = 1;
	static final int SCOPES = 2;
	static final int FORMULAS = 3;
	static final int DESCRIPTIONS = 4;
	static final int ERRORS = 5;

	int type = ALL;
	Runnable onChange;

	static FilterCombo create(Composite comp, FormToolkit tk) {
		FilterCombo combo = new FilterCombo();
		Button button = tk.createButton(comp, "All columns", SWT.NONE);
		button.setImage(Icon.DOWN.get());
		Menu menu = new Menu(button);
		int[] types = {
				ALL,
				NAMES,
				SCOPES,
				FORMULAS,
				DESCRIPTIONS,
				ERRORS
		};
		for (int type : types) {
			MenuItem item = new MenuItem(menu, SWT.NONE);
			item.setText(label(type));
			Controls.onSelect(item, e -> {
				combo.type = type;
				button.setText(label(type));
				button.setToolTipText(label(type));
				button.pack();
				button.getParent().layout();
				if (combo.onChange != null) {
					combo.onChange.run();
				}
			});
		}
		button.setMenu(menu);
		Controls.onSelect(button, e -> menu.setVisible(true));
		return combo;
	}

	private static String label(int type) {
		return switch (type) {
			case ALL -> "All columns";
			case NAMES -> "Names";
			case SCOPES -> "Parameter scopes";
			case FORMULAS -> "Formulas";
			case DESCRIPTIONS -> "Descriptions";
			case ERRORS -> "Evaluation errors";
			default -> "?";
		};
	}
}
