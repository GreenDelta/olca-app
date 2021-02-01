package org.openlca.app.editors.lcia.geo;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.components.FileChooser;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.lcia.ImpactCategoryEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.ImpactCategory;

public class GeoPage extends ModelPage<ImpactCategory> {

	final ImpactCategoryEditor editor;
	Setup setup;

	private GeoParamSection paramSection;
	private GeoFlowSection flowSection;

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
		paramSection = new GeoParamSection(this);
		paramSection.drawOn(body, tk);
		flowSection = new GeoFlowSection(this);
		flowSection.drawOn(body, tk);
		form.reflow(true);
	}

	private void setupSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Setup");
		UI.gridLayout(comp, 3);

		// GeoJSON file
		UI.gridLayout(comp, 3);
		UI.gridData(comp, true, false);
		UI.formLabel(comp, tk, "GeoJSON File");
		var fileText = tk.createText(comp, "");
		fileText.setEditable(false);
		UI.gridData(fileText, false, false).widthHint = 350;
		var fileBtn = tk.createButton(comp, "Open file", SWT.NONE);
		fileBtn.setImage(Icon.FOLDER_OPEN.get());
		Controls.onSelect(fileBtn, _e -> {
			File file = FileChooser.open("*.geojson");
			if (file == null)
				return;
			Setup s = App.exec("Parse GeoJSON ...",
					() -> Setup.read(file));
			if (s == null || s.file == null)
				return;

			// copy possible elementary flow bindings
			// into the new setup (note that the parameters
			// are already initialized in the new setup)
			if (setup != null) {
				s.bindings.addAll(setup.bindings);
			}
			setup = s;
			fileText.setText(s.file);
			paramSection.update();
			flowSection.update();
		});

		UI.filler(comp, tk);
		Composite btnComp = tk.createComposite(comp);
		UI.gridLayout(btnComp, 2, 10, 0);
		Button openBtn = tk.createButton(
				btnComp, "Open setup", SWT.NONE);
		openBtn.setImage(Icon.FOLDER_OPEN.get());
		Button saveBtn = tk.createButton(
				btnComp, "Save setup", SWT.NONE);
		saveBtn.setImage(Icon.SAVE.get());
	}
}
