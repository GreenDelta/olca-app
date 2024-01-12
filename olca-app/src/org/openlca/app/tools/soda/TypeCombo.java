package org.openlca.app.tools.soda;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
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
			var modelType = modelTypeOf(type);
			if (modelType == null)
				continue;
			var item = new MenuItem(menu, SWT.NONE);
			item.setText(Labels.plural(modelType));
			item.setImage(Images.get(modelType));
			Controls.onSelect(item, e -> {
				this.type = type;
				button.setImage(Images.get(modelType));
				button.setToolTipText(Labels.plural(modelType));
			});
		}
		button.setMenu(menu);
		Controls.onSelect(button, e -> menu.setVisible(true));
	}

	private ModelType modelTypeOf(DataSetType type) {
		return switch (type) {
			case CONTACT -> ModelType.ACTOR;
			case MODEL -> ModelType.PRODUCT_SYSTEM;
			case FLOW -> ModelType.FLOW;
			case SOURCE -> ModelType.SOURCE;
			case PROCESS -> ModelType.PROCESS;
			case FLOW_PROPERTY -> ModelType.FLOW_PROPERTY;
			case UNIT_GROUP -> ModelType.UNIT_GROUP;
			case IMPACT_METHOD -> ModelType.IMPACT_CATEGORY;
			case EXTERNAL_FILE -> null;
		};
	}
}
