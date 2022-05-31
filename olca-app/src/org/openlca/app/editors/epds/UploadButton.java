package org.openlca.app.editors.epds;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.openepd.output.ExportDialog;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.core.model.Epd;
import org.openlca.io.openepd.EpdConverter;

record UploadButton(EpdEditor editor) {

	private Epd epd() {
		return editor.getModel();
	}

	void render(Composite parent, FormToolkit tk) {
		var btn = tk.createButton(parent,"", SWT.NONE);
		update(btn);
		btn.setImage(Icon.BUILDING.get());
		Controls.onSelect(btn, $ -> {
			var check = EpdConverter.validate(epd());
			if (check.hasError()) {
				MsgBox.error("Validation error",
					"EPD cannot be converted to an openEPD document: " + check.error());
				return;
			}
			var state = ExportDialog.of(epd());
			if (state.isCreated()) {
				epd().urn = "openEPD:" + state.id();
				editor.emitEvent("urn.change");
				editor.setDirty();
			}
			state.display();
		});

		editor.onEvent("urn.change", () -> update(btn));
	}

	private void update(Button btn) {
		var text = epd().urn != null && epd().urn.startsWith("openEPD:")
			? "Update EPD results on EC3"
			: "Upload as draft to EC3";
		btn.setText(text);
		btn.getParent().layout();
	}
}
