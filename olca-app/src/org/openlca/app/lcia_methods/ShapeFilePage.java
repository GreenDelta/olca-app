package org.openlca.app.lcia_methods;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.FileChooser;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.model.ImpactMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Shows imported shape-files and parameters from these shape-files that
 * can be used in a localised LCIA method.
 */
class ShapeFilePage extends FormPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ImpactMethod method;
	private FormToolkit toolkit;
	private Composite body;
	private ShapeFileSection[] sections;
	private ScrolledForm form;

	public ShapeFilePage(ImpactMethodEditor editor) {
		super(editor, "ShapeFilePage", "Shape files (beta)");
		this.method = editor.getModel();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm, "Shape files");
		toolkit = managedForm.getToolkit();
		body = UI.formBody(form, toolkit);
		createImportButton(body);
		List<String> shapeFiles = ShapeFileUtils.getShapeFiles(method);
		sections = new ShapeFileSection[shapeFiles.size()];
		for (int i = 0; i < shapeFiles.size(); i++)
			sections[i] = new ShapeFileSection(i, shapeFiles.get(i));
		form.reflow(true);
	}

	private void createImportButton(Composite body) {
		Composite composite = UI.formComposite(body);
		Button importButton = toolkit.createButton(composite, Messages.Import,
				SWT.NONE);
		importButton.setImage(ImageType.IMPORT_ICON.get());
		importButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				File file = FileChooser.forImport("*.shp");
				if (file != null)
					checkRunImport(file);
			}
		});
	}

	private void checkRunImport(File file) {
		if (!ShapeFileUtils.isValid(file)) {
			org.openlca.app.util.Error.showBox("Invalid file", "The file "
					+ file.getName() + " is not a valid shape file.");
			return;
		}
		if (ShapeFileUtils.alreadyExists(method, file)) {
			org.openlca.app.util.Error.showBox("File already exists", "A shape "
					+ "file with the given name already exists for this method.");
			return;
		}
		try {
			runImport(file);
		} catch (Exception e) {
			log.error("Failed to import shape file " + file, e);
		}
	}

	private void runImport(File file) throws Exception {
		String shapeFile = ShapeFileUtils.importFile(method, file);
		ShapeFileSection section = new ShapeFileSection(sections.length,
				shapeFile);
		ShapeFileSection[] newSections = new ShapeFileSection[sections.length + 1];
		System.arraycopy(sections, 0, newSections, 0, sections.length);
		newSections[sections.length] = section;
		this.sections = newSections;
		form.reflow(true);
	}

	private class ShapeFileSection {

		private int index;
		private String shapeFile;
		private Section section;

		ShapeFileSection(int index, String shapeFile) {
			this.index = index;
			this.shapeFile = shapeFile;
			render();
		}

		private void render() {
			section = UI.section(body, toolkit, shapeFile);
			Action delete = Actions.onRemove(new Runnable() {
				@Override
				public void run() {
					delete();
				}
			});
			Actions.bind(section, delete);
		}

		private void delete() {
			boolean del = Question.ask("Delete " + shapeFile + "?", "Do you " +
					"really want to delete " + shapeFile + "?");
			if (!del)
				return;
			ShapeFileUtils.deleteFile(method, shapeFile);
			section.dispose();
			ShapeFileSection[] newSections = new ShapeFileSection[sections.length - 1];
			System.arraycopy(sections, 0, newSections, 0, index);
			if ((index + 1) < sections.length)
				System.arraycopy(sections, index + 1, newSections, index,
						newSections.length - index);
			sections = newSections;
			form.reflow(true);
		}
	}


}
