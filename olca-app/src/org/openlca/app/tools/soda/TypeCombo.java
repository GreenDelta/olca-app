package org.openlca.app.tools.soda;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.core.model.ModelType;
import org.openlca.ilcd.commons.DataSetType;

class TypeCombo {

	private DataSetType type = DataSetType.PROCESS;

	static TypeCombo create(Composite parent, FormToolkit tk) {
		var combo = new TypeCombo();
		combo.render(parent, tk);
		return combo;
	}

	public DataSetType selected() {
		return type;
	}

	private void render(Composite parent, FormToolkit tk) {
		var button = tk.createButton(parent, "", SWT.NONE);
		button.setImage(Images.get(ModelType.PROCESS));
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
		for (var type : types) {
			var item = new MenuItem(menu, SWT.NONE);
			item.setText(Util.labelOf(type));
			item.setImage(Util.imageOf(type));
			Controls.onSelect(item, e -> {
				this.type = type;
				button.setImage(Util.imageOf(type));
				button.setToolTipText(Util.labelOf(type));
			});
		}
		button.setMenu(menu);
		Controls.onSelect(button, e -> menu.setVisible(true));
	}


}
