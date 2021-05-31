package org.openlca.app.editors.projects.reports;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.projects.ProjectEditor;
import org.openlca.app.editors.projects.reports.model.Report;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.Project;

class ReportEditorPage extends FormPage {

	private final Report report;
	private final ReportEditor editor;

	private FormToolkit tk;
	private SectionList sectionList;

	public ReportEditorPage(ReportEditor editor) {
		super(editor, "ReportInfoPage", M.Report);
		this.editor = editor;
		this.report = new Report();
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(mform, "Report");
		tk = mform.getToolkit();
		var body = UI.formBody(form, tk);
		createInfoSection(body);
		createAddButton(body);
		sectionList = new SectionList(editor, body, form, tk);
		form.reflow(true);
	}

	private void createAddButton(Composite body) {
		Composite comp = UI.formComposite(body, tk);
		UI.filler(comp);
		Button addButton = tk.createButton(comp, M.AddSection, SWT.NONE);
		addButton.setImage(Icon.ADD.get());
		Controls.onSelect(addButton, e -> sectionList.addNew());
	}

	private void createInfoSection(Composite body) {
		Composite comp = UI.formSection(body, tk, M.GeneralInformation);
		var titleText = UI.formText(comp, tk, M.Title);
		if (report.title != null) {
			titleText.setText(report.title);
		}
		titleText.addModifyListener($ -> {
			report.title = titleText.getText();
		});
	}

}
