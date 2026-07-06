package org.openlca.app.tools.libraries;

import java.io.File;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;

class FileSelector {

	private final Text text;
	private Supplier<Optional<File>> handler;

	private FileSelector(Text text) {
		this.text = text;
	}

	static FileSelector create(
		Composite parent, FormToolkit tk, String label
	) {

		UI.label(parent, tk, label);
		var comp = UI.composite(parent, tk);
		UI.stretchX(comp);
		UI.gridLayout(comp, 2, 10, 0);

		var text = UI.text(comp, tk);
		text.setEditable(false);
		var button = UI.button(comp, tk, M.Browse);

		var selector = new FileSelector(text);
		Controls.onSelect(button, _ -> selector.handleClick());
		return selector;
	}

	void onSelect(Supplier<Optional<File>> handler) {
		this.handler = handler;
	}

	private void handleClick() {
		if (handler == null) return;
		var file = handler.get().orElse(null);
		if (file == null) {
			text.setText("");
		} else {
			text.setText(file.getAbsolutePath());
		}
	}

}
