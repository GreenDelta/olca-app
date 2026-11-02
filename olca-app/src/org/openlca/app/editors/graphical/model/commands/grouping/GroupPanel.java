package org.openlca.app.editors.graphical.model.commands.grouping;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.AnalysisGroup;

/// A row in the [GroupDialog] with a radio button for selecting the group.
/// For a `null` group, it is the "None" entry; otherwise, it also contains
/// controls for editing the name and color of the group and for deleting it.
record GroupPanel(GroupDialog dialog, AnalysisGroup group) {

	/// Returns the radio button, so that the dialog can select the "None" entry
	/// when the selected group is deleted.
	Button render(Composite parent, FormToolkit tk) {

		var radio = tk.createButton(parent, "", SWT.RADIO);
		radio.setSelection(Objects.equals(group, dialog.selected()));
		Controls.onSelect(radio, _ -> {
			if (radio.getSelection()) {
				dialog.select(group);
			}
		});

		var comp = tk.createComposite(parent);
		UI.stretchX(comp);
		UI.gridLayout(comp, group == null ? 1 : 3, 10, 1);

		if (group == null) {
			UI.label(comp, tk, M.None);
			return radio;
		}

		var text = UI.text(comp, tk);
		UI.stretchX(text);
		if (group.name != null) {
			text.setText(group.name);
		}
		text.addModifyListener(_ -> {
			group.name = text.getText();
			dialog.markChanged();
		});

		// color selector
		var colorBtn = tk.createButton(comp, "□□", SWT.NONE);
		var color = dialog.colorOf(group);
		colorBtn.setForeground(color);
		colorBtn.setBackground(color);
		colorBtn.setToolTipText("Select a group color");
		Controls.onSelect(colorBtn, _ -> {
			var cd = new ColorDialog(dialog.getShell());
			cd.setRGB(dialog.colorOf(group).getRGB());
			var next = cd.open();
			if (next != null) {
				var nextColor = Colors.get(next);
				colorBtn.setForeground(nextColor);
				colorBtn.setBackground(nextColor);
				group.color = String.format(
					"#%02x%02x%02x", next.red, next.green, next.blue);
				dialog.markChanged();
			}
		});

		var delete = UI.imageHyperlink(comp, tk);
		delete.setImage(Icon.DELETE_DISABLED.get());
		delete.setHoverImage(Icon.DELETE.get());
		delete.setToolTipText("Delete analysis group");

		Controls.onClick(delete, _ -> {
			dialog.delete(group);
			radio.dispose();
			comp.dispose();
			parent.layout();
			parent.getParent().layout();
		});
		return radio;
	}
}
