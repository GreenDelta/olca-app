package org.openlca.app.editors.lcia_methods.shapefiles;

import java.awt.Desktop;
import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.editors.lcia_methods.ImpactMethodEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Parameter;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shows imported shape-files and parameters from these shape-files that can be
 * used in a localized LCIA method.
 */
public class ShapeFilePage extends FormPage {

	final ImpactMethodEditor editor;

	private Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit tk;
	private Composite body;
	private SFSection[] sections;
	private ScrolledForm form;

	public ShapeFilePage(ImpactMethodEditor editor) {
		super(editor, "ShapeFilePage", "Shape files (beta)");
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(managedForm, "Shape file parameters");
		tk = managedForm.getToolkit();
		body = UI.formBody(form, tk);
		new SFParamMeanSection(this).render(body, tk);
		createFileSection();
		List<String> shapeFiles = ShapeFileUtils.getShapeFiles(method());
		sections = new SFSection[shapeFiles.size()];
		for (int i = 0; i < shapeFiles.size(); i++) {
			sections[i] = new SFSection(this, i, shapeFiles.get(i));
			sections[i].render(body, tk);
		}
		form.reflow(true);
	}

	private void createFileSection() {
		Composite comp = UI.formSection(body, tk, "Files");
		createFolderLink(comp);
		UI.filler(comp, tk);
		Button importButton = tk.createButton(comp, M.Import, SWT.NONE);
		importButton.setImage(Icon.IMPORT.get());
		Controls.onSelect(importButton, (e) -> {
			File file = FileChooser.forImport("*.shp");
			if (file != null)
				checkRunImport(file);
		});
		UI.filler(comp, tk);
		Button evaluateButton = tk.createButton(comp, M.EvaluateLocations, SWT.NONE);
		evaluateButton.setImage(Icon.EXPRESSION.get());
		Controls.onSelect(evaluateButton, (e) -> {
			try {
				new ProgressMonitorDialog(UI.shell()).run(true, true,
						new EvaluateLocationsJob(method()));
			} catch (Exception ex) {
				log.error("Failed to evaluate locations", ex);
			}
		});
	}

	private void createFolderLink(Composite composite) {
		UI.formLabel(composite, tk, "Location");
		ImageHyperlink link = tk.createImageHyperlink(composite, SWT.TOP);
		File folder = ShapeFileUtils.getFolder(method());
		link.setText(Strings.cut(folder.getAbsolutePath(), 75));
		link.setImage(Icon.FOLDER.get());
		link.setForeground(Colors.linkBlue());
		link.setToolTipText(folder.getAbsolutePath());
		Controls.onClick(link, e -> {
			try {
				if (folder.exists() && folder.isDirectory())
					Desktop.getDesktop().open(folder);
			} catch (Exception ex) {
				log.error("failed to open shape-file folder", ex);
			}
		});
	}

	List<ShapeFileParameter> checkRunImport(File file) {
		if (!ShapeFileUtils.isValid(file)) {
			org.openlca.app.util.Error.showBox("Invalid file", "The file "
					+ file.getName() + " is not a valid shape file.");
			return Collections.emptyList();
		}
		if (ShapeFileUtils.alreadyExists(method(), file)) {
			org.openlca.app.util.Error
					.showBox("File already exists", "A shape file with the given "
							+ "name already exists for this method.");
			return Collections.emptyList();
		}
		try {
			return runImport(file);
		} catch (Exception e) {
			log.error("Failed to import shape file " + file, e);
			return Collections.emptyList();
		}
	}

	private List<ShapeFileParameter> runImport(File file) throws Exception {
		String shapeFile = ShapeFileUtils.importFile(method(), file);
		List<ShapeFileParameter> params = ShapeFileUtils.getParameters(method(),
				shapeFile);
		for (ShapeFileParameter parameter : params) {
			if (!Parameter.isValidName(parameter.name)) {
				org.openlca.app.util.Error.showBox("Invalid parameter",
						"The parameter name '" + parameter.name
								+ "' is not supported");
				ShapeFileUtils.deleteFile(method(), shapeFile);
				return Collections.emptyList();
			}
		}
		addSection(shapeFile, params);
		return params;
	}

	private void addSection(String shapeFile, List<ShapeFileParameter> params) {
		SFSection s = new SFSection(
				this, sections.length, shapeFile);
		s.render(body, tk);
		SFSection[] newSections = new SFSection[sections.length + 1];
		System.arraycopy(sections, 0, newSections, 0, sections.length);
		newSections[sections.length] = s;
		this.sections = newSections;
		s.parameterTable.viewer.setInput(params);
		form.reflow(true);
		editor.getParameterSupport().evaluate();
	}

	void removeSection(SFSection section) {
		if (section == null)
			return;
		SFSection[] newSections = new SFSection[sections.length - 1];
		int idx = section.index;
		System.arraycopy(sections, 0, newSections, 0, idx);
		if ((idx + 1) < sections.length) {
			System.arraycopy(sections, idx + 1, newSections, idx,
					newSections.length - idx);
		}
		sections = newSections;
		form.reflow(true);
	}

	private ImpactMethod method() {
		return editor.getModel();
	}

}
