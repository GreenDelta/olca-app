package org.openlca.app.editors.lcia.geo;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
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
	private Setup setup;

	public GeoPage(ImpactCategoryEditor editor) {
		super(editor, "GeoPage", "Regionalized calculation");
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(this);
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		setupSection(body, tk);
		form.reflow(true);
	}

	private void setupSection(Composite body, FormToolkit tk) {
		Composite comp = UI.formSection(body, tk, "Setup");
		UI.gridLayout(comp, 3);

		// GeoJSON file
		UI.gridLayout(comp, 3);
		UI.gridData(comp, true, false);
		UI.formLabel(comp, tk, "GeoJSON File");
		Text fileText = tk.createText(comp, "");
		fileText.setEditable(false);
		UI.gridData(fileText, false, false).widthHint = 350;
		Button fileBtn = tk.createButton(
				comp, "Open file", SWT.NONE);
		fileBtn.setImage(Icon.FOLDER_OPEN.get());
		Controls.onSelect(fileBtn, _e -> {
			File file = FileChooser.open("*.geojson");
			if (file == null)
				return;
			setup = App.exec("Parse GeoJSON ...",
					() -> Setup.create(file));
			if (setup.file != null) {
				fileText.setText(setup.file);
			}

			// TODO: update parameters etc.
			// we will not remove the flows from setup
			// but update all parameters when the user
			// changes the GeoJSON file
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
