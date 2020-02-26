package org.openlca.app.editors.lcia.geo;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.lcia.ImpactCategoryEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;
import org.openlca.core.model.ImpactCategory;

public class GeoPage extends ModelPage<ImpactCategory> {

	final ImpactCategoryEditor editor;

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
		Button browseBtn = tk.createButton(
				comp, "Open file", SWT.NONE);
		browseBtn.setImage(Icon.FOLDER_OPEN.get());
		// Controls.onClick(link, fn);

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
