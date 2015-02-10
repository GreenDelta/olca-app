package org.openlca.app.editors.reports;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.editors.DataBinding;
import org.openlca.app.editors.projects.ProjectEditor;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;

public class ReportEditorPage extends FormPage {

	private Report report;
	private ProjectEditor editor;
	private DataBinding binding;

	private FormToolkit toolkit;
	private SectionList sectionList;

	public ReportEditorPage(ProjectEditor editor, Report report) {
		super(editor, "ReportInfoPage", Messages.ReportSections);
		this.editor = editor;
		this.report = report;
		this.binding = new DataBinding(editor);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.ReportSections);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createInfoSection(body);
		createAddButton(body);
		sectionList = new SectionList(editor, body, form, toolkit);
		form.reflow(true);
	}

	private void createAddButton(Composite body) {
		Composite composite = UI.formComposite(body, toolkit);
		UI.formLabel(composite, "");
		Button addButton = toolkit.createButton(composite, Messages.AddSection,
				SWT.NONE);
		addButton.setImage(ImageType.ADD_ICON.get());
		Controls.onSelect(addButton, (e) -> {
			sectionList.addNew();
		});
	}

	private void createInfoSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.GeneralInformation);
		Text titleText = UI.formText(composite, toolkit, Messages.Title);
		binding.onString(() -> report, "title", titleText);
	}

}