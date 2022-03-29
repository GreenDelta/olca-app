package org.openlca.app.editors.processes;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.UI;
import org.openlca.core.database.FileStore;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.Source;
import org.openlca.util.Strings;


class ImageSection {

	private final ModelEditor<Process> editor;
	private final FormToolkit tk;
	private final Composite body;

	private Composite comp;
	private List<Control> controls = new ArrayList<>();

	ImageSection(ModelEditor<Process> editor, FormToolkit tk, Composite body) {
		this.body = body;
		this.tk = tk;
		this.editor = editor;
		editor.onEvent(ProcessEditor.SOURCES_CHANGED, this::update);
		update();
	}

	void update() {
		for (Control c : controls) {
			c.dispose();
		}
		controls.clear();
		ProcessDocumentation doc = editor.getModel().documentation;
		if (doc == null)
			return;

		List<File> files = new ArrayList<>();
		for (Source s : doc.sources) {
			if (!isImage(s.externalFile))
				continue;
			File dir = new FileStore(
					Database.get()).getFolder(s);
			File file = new File(dir, s.externalFile);
			if (!file.exists())
				continue;
			files.add(file);
		}
		if (files.isEmpty())
			return;

		if (comp == null) {
			comp = UI.formSection(body, tk, "Attached images");
		}
		files.forEach(this::createControls);

		comp.pack();
		Composite container = comp;
		while (container != null) {
			container = container.getParent();
			if (container instanceof ScrolledForm) {
				((ScrolledForm) container).reflow(true);
				break;
			}
		}
	}

	private void createControls(File f) {
		Label header = tk.createLabel(comp, Strings.cut(f.getName(), 40));
		header.setToolTipText(f.getName());
		UI.gridData(header, false, false).verticalAlignment = SWT.BEGINNING;
		controls.add(header);
		Image img = new Image(comp.getDisplay(), f.getAbsolutePath());
		Label lab = tk.createLabel(comp, "");
		lab.setImage(img);
		lab.addDisposeListener(e -> {
			if (!img.isDisposed()) {
				img.dispose();
			}
		});
		controls.add(lab);
	}

	private boolean isImage(String file) {
		if (file == null)
			return false;
		String[] parts = file.split("\\.");
		String ext = parts[parts.length - 1].toLowerCase().trim();
		List<String> exts = Arrays.asList(
				"gif", "jpg", "jpeg", "png", "bmp");
		return exts.contains(ext);
	}
}
