package org.openlca.app.editors.reports;

import org.eclipse.jface.viewers.TableViewer;
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
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;

class ReportInfoPage extends FormPage {

	private final Report report;

	private FormToolkit toolkit;

	public ReportInfoPage(ReportEditor editor, Report report) {
		super(editor, "ReportInfoPage", "Report");
		this.report = report;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "Report for project: "
				+ Labels.getDisplayName(report.getProject()));
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createIntroductionSection(body);
		createAcknowledgmentSection(body);
		createParameternamesSection(body);
		createPreviewButton(body);
		form.reflow(true);
	}

	private void createIntroductionSection(Composite parent) {
		Composite composite = UI.formSection(parent, toolkit, "Introduction");

	}

	private void createAcknowledgmentSection(Composite parent) {
		Composite composite = UI
				.formSection(parent, toolkit, "Acknowledgement");
	}

	private void createParameternamesSection(Composite body) {
		Section section = UI.section(body, toolkit, "Parameter mapping");
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] properties = { Messages.Parameter, Messages.Name };
		TableViewer viewer = Tables.createViewer(composite, properties);
		Tables.bindColumnWidths(viewer, 0.5, 0.5);
		UI.gridData(viewer.getTable(), true, true).minimumHeight = 150;
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
