package org.openlca.app.editors.graphical.model.commands.grouping;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.AnalysisGroup;

record GroupPanel(GroupDialog dialog, AnalysisGroup group) {

	void render(Composite parent, FormToolkit tk) {

		var radio = tk.createButton(parent, "", SWT.RADIO);
		radio.setSelection(Objects.equals(group, dialog.selected));
		Controls.onSelect(radio, _ -> {
			if (radio.getSelection()) {
				dialog.selected = group;
			}
		});

		var comp = tk.createComposite(parent);
		UI.stretchX(comp);
		UI.gridLayout(comp, group == null ? 1 : 3, 10, 1);

		if (group == null) {
			UI.label(comp, tk, M.None);
			return;
		}

		var text = UI.text(comp, tk);
		UI.stretchX(text);
		if (group.name != null) {
			text.setText(group.name);
		}
		text.addModifyListener(_ -> {
			group.name = text.getText();
			dialog.groupsChanged = true;
		});

		// color selector
		var colorBtn = tk.createButton(comp, "□□", SWT.NONE);
		var color = dialog.colorOf(group);
		colorBtn.setForeground(color);
		colorBtn.setBackground(color);
		colorBtn.setToolTipText("Select a group color");
		Controls.onSelect(colorBtn, _ -> {
			var cd = new ColorDialog(dialog.getShell());
			cd.setRGB(color.getRGB());
			var next = cd.open();
			if (next != null) {
				var nextColor = Colors.get(next);
				colorBtn.setForeground(nextColor);
				colorBtn.setBackground(nextColor);
				group.color = String.format(
					"#%02x%02x%02x", next.red, next.green, next.blue);
				dialog.groupsChanged = true;
			}
		});

		var delete = UI.imageHyperlink(comp, tk);
		delete.setImage(Icon.DELETE_DISABLED.get());
		delete.setHoverImage(Icon.DELETE.get());
		delete.setToolTipText("Delete analysis group");

		Controls.onClick(delete, _ -> {
			dialog.groups.remove(group);
			dialog.groupsChanged = true;
			radio.dispose();
			comp.dispose();
			parent.layout();
			parent.getParent().layout();
		});
	}
}
