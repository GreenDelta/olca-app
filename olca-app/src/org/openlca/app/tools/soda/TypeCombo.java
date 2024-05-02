package org.openlca.app.tools.soda;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Controls;
import org.openlca.ilcd.commons.DataSetType;

class TypeCombo {

	private final boolean hasEpds;
	private DataSetType type = DataSetType.PROCESS;

	private TypeCombo(boolean hasEpds) {
		this.hasEpds = hasEpds;
	}

	static TypeCombo create(Composite parent, FormToolkit tk, boolean hasEpds) {
		var combo = new TypeCombo(hasEpds);
		combo.render(parent, tk);
		return combo;
	}

	public DataSetType selected() {
		return type;
	}

	private void render(Composite parent, FormToolkit tk) {
		var button = tk.createButton(parent, "", SWT.NONE);
		button.setImage(Util.imageOf(type, hasEpds));
		var menu = new Menu(button);
		DataSetType[] types = {
				DataSetType.PROCESS,
				DataSetType.MODEL,
				DataSetType.IMPACT_METHOD,
				DataSetType.FLOW,
				DataSetType.FLOW_PROPERTY,
				DataSetType.UNIT_GROUP,
				DataSetType.CONTACT,
				DataSetType.SOURCE,
		};
		for (var t : types) {
			var item = new MenuItem(menu, SWT.NONE);
			item.setText(Util.labelOf(t, hasEpds));
			item.setImage(Util.imageOf(t, hasEpds));
			Controls.onSelect(item, e -> {
				this.type = t;
				button.setImage(Util.imageOf(t, hasEpds));
				button.setToolTipText(Util.labelOf(t, hasEpds));
			});
		}
		button.setMenu(menu);
		Controls.onSelect(button, e -> menu.setVisible(true));
	}
}
