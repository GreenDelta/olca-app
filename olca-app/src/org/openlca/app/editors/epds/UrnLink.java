package org.openlca.app.editors.epds;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.openepd.Ec3;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.UI;
import org.openlca.core.model.Epd;
import org.openlca.util.Strings;

record UrnLink(EpdEditor editor) {

	private Epd epd() {
		return editor.getModel();
	}

	void render(Composite parent, FormToolkit tk) {
		var comp = tk.createComposite(parent);
		UI.gridLayout(comp, 2, 10, 0);

		// link text
		var link = tk.createImageHyperlink(comp, SWT.NONE);
		update(link);
		Controls.onClick(link, $ -> {
			var urn = epd().urn;
			if (Strings.nullOrEmpty(urn))
				return;
			if (urn.startsWith("openEPD:")) {
				var extId = urn.substring(8);
				var url = Ec3.displayUrl(extId);
				Desktop.browse(url);
			}
		});

		// delete button
		var deleteBtn = tk.createImageHyperlink(comp, SWT.TOP);
		deleteBtn.setToolTipText(M.Remove);
		deleteBtn.setHoverImage(Icon.DELETE.get());
		deleteBtn.setImage(Icon.DELETE_DISABLED.get());
		Controls.onClick(deleteBtn, $ -> {
			epd().urn = "";
			editor.emitEvent("urn.change");
			editor.setDirty();
		});

		editor.onEvent("urn.change", () -> update(link));
	}

	private void update(ImageHyperlink link) {
		if (link == null || link.isDisposed())
			return;
		link.setText(Strings.nullOrEmpty(epd().urn)
			? "- none -"
			: epd().urn
		);
		link.getParent().pack();
	}
}
