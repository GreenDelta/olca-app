package org.openlca.app.editors.reports;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.editors.DataBinding;
import org.openlca.app.editors.projects.ProjectEditor;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;

public class ReportEditorPage extends FormPage {

	private Report report;
	private ProjectEditor editor;
	private DataBinding binding;

	private FormToolkit toolkit;
	private SectionList sectionList;

	public ReportEditorPage(ProjectEditor editor, Report report) {
		super(editor, "ReportInfoPage", "Report sections");
		this.editor = editor;
		this.report = report;
		this.binding = new DataBinding(editor);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "Report sections");
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
		Button addButton = toolkit.createButton(composite, "Add section",
				SWT.NONE);
		addButton.setImage(ImageType.ADD_ICON.get());
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sectionList.addNew();
			}
		});
	}

	private void createInfoSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.GeneralInformation);
		Text titleText = UI.formText(composite, toolkit, "Title");
		binding.onString(() -> report, "title", titleText);
		UI.formLabel(composite, toolkit, Messages.Project);
		ImageHyperlink link = toolkit.createImageHyperlink(composite, SWT.TOP);
		link.setText(Labels.getDisplayName(report.getProject()));
		link.setForeground(Colors.getLinkBlue());
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				App.openEditor(report.getProject());
			}
		});
		createPreviewButton(composite);
	}

	private void createPreviewButton(Composite composite) {
		toolkit.createLabel(composite, "");
		Button button = toolkit.createButton(composite, "Preview", SWT.NONE);
		button.setImage(ImageType.SEARCH_ICON.get());
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ReportViewer.open(report);
			}
		});
	}

}