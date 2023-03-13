package org.openlca.app.wizards.io;

import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.util.Strings;

class FilePicker {

	private final Consumer<File> handler;

	private String[] extensions;
	private File selection;
	private String title;

	private FilePicker(Consumer<File> handler) {
		this.handler = handler;
	}

	static FilePicker on(Consumer<File> handler) {
		return new FilePicker(handler);
	}

	FilePicker withExtensions(String... exts) {
		if (exts == null || exts.length == 0) {
			extensions = null;
			return this;
		}
		extensions = Arrays.stream(exts)
				.filter(Strings::notEmpty)
				.map(e -> e.startsWith("*.") ? e : "*." + e)
				.toArray(String[]::new);
		return this;
	}

	FilePicker withSelection(File file) {
		this.selection = file;
		return this;
	}

	FilePicker withTitle(String title) {
		this.title = title;
		return this;
	}

	void renderOn(Composite body) {
		var comp = UI.formComposite(body);
		UI.fillHorizontal(comp);
		UI.gridLayout(comp, 3);
		var fileText = UI.formText(comp, M.File, SWT.READ_ONLY);
		if (selection != null) {
			fileText.setText(selection.getName());
		}
		UI.fillHorizontal(fileText);
		var browse = new Button(comp, SWT.NONE);
		browse.setText(M.Browse);
		if (title != null) {
			browse.setToolTipText(title);
		}

		Controls.onSelect(browse, e -> {
			var file = FileChooser.openFile()
					.withTitle(title)
					.withExtensions(extensions)
					.select()
					.orElse(null);
			if (file != null) {
				selection = file;
				fileText.setText(file.getName());
				handler.accept(file);
			}
		});
	}

}
