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
	private String dialogTitle;
	private String label = M.File;

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

	/// Set the title of the file dialog which is opened by this selector.
	FileSelector withDialogTitle(String title) {
		this.dialogTitle = title;
		return this;
	}

	/// Set the label of the selector component, default is `File`.
	FileSelector withLabel(String label) {
		if (label != null) {
			this.label = label;
		}
		return this;
	}

	void render(Composite comp) {
		var text = UI.labeledText(comp, label, SWT.READ_ONLY);
		text.setBackground(Colors.systemColor(SWT.COLOR_LIST_BACKGROUND));
		if (selection != null) {
			text.setText(selection.getName());
		}
		UI.fillHorizontal(text);
		var browse = new Button(comp, SWT.NONE);
		browse.setText(M.Browse);
		browse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		if (dialogTitle != null) {
			browse.setToolTipText(dialogTitle);
		}

		Controls.onSelect(browse, e -> {
			var file = FileChooser.openFile()
					.withTitle(dialogTitle)
					.withExtensions(extensions)
					.select()
					.orElse(null);
			if (file != null) {
				selection = file;
				text.setText(file.getName());
				handler.accept(file);
			}
		});
	}
}
