package org.openlca.app.editors.lcia.geo;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.lcia.ImpactCategoryEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;
import org.openlca.core.model.ImpactCategory;
import org.openlca.util.Strings;

public class GeoPage extends ModelPage<ImpactCategory> {

	final ImpactCategoryEditor editor;
	Setup setup;

	private GeoPropertySection paramSection;
	private GeoFlowSection flowSection;
	private Text fileText;
	private Button saveBtn;

	public GeoPage(ImpactCategoryEditor editor) {
		super(editor, "GeoPage", "Regionalized calculation");
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(this);
		var tk = mform.getToolkit();
		var body = UI.formBody(form, tk);
		setupSection(body, tk);
		paramSection = new GeoPropertySection(this);
		paramSection.drawOn(body, tk);
		flowSection = new GeoFlowSection(this);
		flowSection.drawOn(body, tk);
		form.reflow(true);
	}

	private void setupSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Setup");
		UI.gridLayout(comp, 2);

		// file text
		UI.gridData(comp, true, false);
		UI.formLabel(comp, tk, "GeoJSON or setup file:");
		fileText = tk.createText(comp, "");
		fileText.setEditable(false);
		UI.gridData(fileText, true, false);

		// buttons
		UI.filler(comp, tk);
		var btnComp = tk.createComposite(comp);
		UI.gridLayout(btnComp, 2, 10, 0);
		var openBtn = tk.createButton(btnComp, "Open", SWT.NONE);
		openBtn.setImage(Icon.FOLDER_OPEN.get());
		UI.gridData(openBtn, false, false).widthHint = 80;
		Controls.onSelect(openBtn, _e -> onOpenFile());
		saveBtn = tk.createButton(btnComp, "Save", SWT.NONE);
		saveBtn.setImage(Icon.SAVE.get());
		UI.gridData(saveBtn, false, false).widthHint = 80;
		saveBtn.setEnabled(false);
		Controls.onSelect(saveBtn, _e -> onSaveFile());
	}

	private void onOpenFile() {
		var file = FileChooser.openFile()
			.withExtensions("*.geojson;*.json")
			.select()
			.orElse(null);
		if (file == null)
			return;
		var nextSetup = App.exec(
			"Parse setup ...",
			() -> Setup.read(file, Database.get()));
		if (nextSetup == null) {
			ErrorReporter.on("Failed to read setup or" +
											 " GeoJSON file from " + file);
			return;
		}
		setup = nextSetup;
		fileText.setText(file.getAbsolutePath());
		saveBtn.setEnabled(true);
		paramSection.update();
		flowSection.update();
	}

	private void onSaveFile() {
		if (setup == null) {
			MsgBox.error("No setup loaded", "Nothing to save.");
			return;
		}
		File file;
		var path = fileText.getText();
		if (Strings.notEmpty(path) && path.endsWith(".json")) {
			var temp = new File(path);
			file = FileChooser.forSavingFile(M.Export, temp.getName());
		} else {
			file = FileChooser.forSavingFile(M.Export, "setup.json");
		}

		if (file == null)
			return;
		try {
			setup.writeTo(file);
			fileText.setText(file.getAbsolutePath());
			Popup.info("Saved setup to file " + file.getName());
		} catch (Exception e) {
			ErrorReporter.on("Failed to save setup for calculation " +
											 "of regionalized characterization factors " +
											 "to file " + file);
		}
	}
}
