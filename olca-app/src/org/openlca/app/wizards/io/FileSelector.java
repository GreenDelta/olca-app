package org.openlca.app.wizards.io;

import java.io.File;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;

class FileSelector {

	private final Consumer<File> handler;

	private String[] extensions;
	private File selection;
	private String title;

	private FileSelector(Consumer<File> handler) {
		this.handler = handler;
	}

	static FileSelector on(Consumer<File> handler) {
		return new FileSelector(handler);
	}

	FileSelector withExtensions(String... exts) {
		if (exts == null || exts.length == 0) {
			extensions = null;
			return this;
		}
		extensions = exts;
		return this;
	}

	FileSelector withSelection(File file) {
		this.selection = file;
		return this;
	}

	FileSelector withTitle(String title) {
		this.title = title;
		return this;
	}

	void render(Composite comp) {
		var fileText = UI.labeledText(comp, M.File, SWT.READ_ONLY);
		fileText.setBackground(Colors.systemColor(SWT.COLOR_LIST_BACKGROUND));
		if (selection != null) {
			fileText.setText(selection.getName());
		}
		UI.fillHorizontal(fileText);
		var browse = new Button(comp, SWT.NONE);
		browse.setText(M.Browse);
		browse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
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
