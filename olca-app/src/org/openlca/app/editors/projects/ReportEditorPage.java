package org.openlca.app.editors.projects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.editors.projects.reports.model.Report;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;

class ReportEditorPage extends FormPage {

	private final ProjectEditor editor;

	private FormToolkit tk;
	private ReportSectionList sectionList;

	public ReportEditorPage(ProjectEditor editor) {
		super(editor, "ReportInfoPage", M.Report);
		this.editor = editor;
	}

	private Report report() {
		return editor.report();
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(mform, "Report");
		tk = mform.getToolkit();
		var body = UI.formBody(form, tk);
		createInfoSection(body);
		createAddButton(body);
		sectionList = new ReportSectionList(editor, body, form, tk);
		form.reflow(true);
	}

	private void createAddButton(Composite body) {
		var comp = UI.formComposite(body, tk);
		UI.filler(comp);
		var addButton = tk.createButton(comp, M.AddSection, SWT.NONE);
		addButton.setImage(Icon.ADD.get());
		Controls.onSelect(addButton, e -> sectionList.addNew());
	}

	private void createInfoSection(Composite body) {
		var comp = UI.formSection(body, tk, M.GeneralInformation);
		var titleText = UI.formText(comp, tk, M.Title);
		if (report().title != null) {
			titleText.setText(report().title);
		}
		titleText.addModifyListener($ -> report().title = titleText.getText());
	}

}
